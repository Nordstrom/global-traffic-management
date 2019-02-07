/**
 * Copyright (C) 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nordstrom.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.nlpdeployment.CustomerAccountNlp;
import com.nordstrom.gtm.nlpdeployment.NlpDeploymentGrpc;
import com.xjeffrose.xio.tls.SslContextFactory;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
class NlpDeployment {

  // TODO(br): need to add exponential backoff for error cases
  ListenableFuture<Empty> phoneHome(NlpConfig nlpConfig) {
    OkHttpClient client = new OkHttpClient();
    CustomerAccountNlp.Builder customerAccountNlpBuilder = CustomerAccountNlp.newBuilder();

    boolean isIpAddressSuccess =
        getIpAddress(client, nlpConfig.getNlpDeploymentConfig(), customerAccountNlpBuilder);
    boolean isAwsInfoSuccess =
        getAWSInfo(client, nlpConfig.getNlpDeploymentConfig(), customerAccountNlpBuilder);

    if (!isIpAddressSuccess || !isAwsInfoSuccess) {
      String msg = "Could not get deployment info";
      log.error(msg);
      return Futures.immediateFailedFuture(new Exception(msg));
    }

    NlpDeploymentGrpc.NlpDeploymentFutureStub futureStub = makeFutureStub(nlpConfig);

    log.debug("Phoning home to NFE");
    ListenableFuture<Empty> listenableFuture =
        futureStub.deployedNlp(customerAccountNlpBuilder.build());

    Futures.addCallback(
        listenableFuture,
        new FutureCallback<Empty>() {
          @Override
          public void onSuccess(@Nullable Empty result) {
            log.info("Successfully notified NFE of deployment");
          }

          @Override
          public void onFailure(Throwable t) {
            log.error("Could not notify NFE of deployment: ", t);
          }
        },
        MoreExecutors.directExecutor());

    return listenableFuture;
  }

  private NlpDeploymentGrpc.NlpDeploymentFutureStub makeFutureStub(NlpConfig nlpConfig) {
    NlpDeploymentConfig nlpDeploymentConfig = nlpConfig.getNlpDeploymentConfig();
    SslContext sslContext =
        SslContextFactory.buildClientContext(
            nlpConfig.getTlsConfig(), InsecureTrustManagerFactory.INSTANCE);
    ManagedChannel channel =
        NettyChannelBuilder.forAddress(nlpDeploymentConfig.getHost(), nlpDeploymentConfig.getPort())
            .sslContext(sslContext)
            .build();

    return NlpDeploymentGrpc.newFutureStub(channel);
  }

  /// If call is successful, will update the builder with new info and return true.
  private boolean getIpAddress(
      OkHttpClient client,
      NlpDeploymentConfig nlpDeploymentConfig,
      CustomerAccountNlp.Builder builder) {
    Request request = new Request.Builder().url(nlpDeploymentConfig.getAwsIpAddressPath()).build();

    try {
      log.debug("Attempt to get ip address");
      Response response = client.newCall(request).execute();
      if (response.body() == null) {
        log.error("Ip address response has no body");
        return false;
      }

      String ipAddress = response.body().string();
      log.debug("Got ip address: " + ipAddress);
      builder.setIpAddress(ipAddress);
      return true;

    } catch (IOException e) {
      log.error("Failed to get ip address: ", e);
      return false;
    }
  }

  /// If the call is successful, will update the builder with new info and return true.
  private boolean getAWSInfo(
      OkHttpClient client,
      NlpDeploymentConfig nlpDeploymentConfig,
      CustomerAccountNlp.Builder builder) {
    Request request =
        new Request.Builder().url(nlpDeploymentConfig.getAwsInstanceIdentityPath()).build();

    try {
      log.debug("Attempting to get aws instance identity document");
      Response response = client.newCall(request).execute();
      if (response.body() == null) {
        log.error("Instance identity document response has no body");
        return false;
      }

      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(response.body().string());
      JsonNode accountIdNode = rootNode.get("accountId");
      if (accountIdNode == null) {
        log.error("Instance identity document response does not have an account id");
        return false;
      }

      JsonNode instanceIdNode = rootNode.get("instanceId");
      if (instanceIdNode == null) {
        log.error("Instance identity document response does not have an instance id");
        return false;
      }

      String accountId = accountIdNode.asText();
      String instanceId = instanceIdNode.asText();

      log.debug("Got Account ID: " + accountId);
      log.debug("Got Instance ID: " + instanceId);

      builder.setAwsAccountId(accountId).setAwsInstanceId(instanceId);
      return true;

    } catch (IOException e) {
      log.error("Failed to get instance identity document: ", e);
      return false;
    }
  }
}
