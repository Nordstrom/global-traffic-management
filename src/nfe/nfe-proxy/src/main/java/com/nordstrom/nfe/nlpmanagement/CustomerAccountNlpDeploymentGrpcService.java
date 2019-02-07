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
package com.nordstrom.nfe.nlpmanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.nlpdeployment.CustomerAccountNlp;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.Status;
import io.grpc.StatusException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomerAccountNlpDeploymentGrpcService implements GrpcService {
  public static final String CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH = "/nlps/aws";

  private ZkClient zkClient;
  private ObjectMapper objectMapper;

  public CustomerAccountNlpDeploymentGrpcService(ZkClient zkClient, ObjectMapper objectMapper) {
    this.zkClient = zkClient;
    this.objectMapper = objectMapper;
  }

  public String getPackageName() {
    return "nordstrom.gtm.nlpdeployment";
  }

  public String getServiceName() {
    return "NlpDeployment";
  }

  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(Lists.newArrayList(deployedNlpRoute()));
  }

  private GrpcRoute deployedNlpRoute() {
    return new GrpcRoute(this, "DeployedNlp", deployedNlpHandler());
  }

  private GrpcRequestHandler<CustomerAccountNlp, Empty> deployedNlpHandler() {
    return new GrpcRequestHandler<>(
        CustomerAccountNlp::parseFrom,
        (CustomerAccountNlp request) -> {
          saveCustomerAccountNlpToZooKeeper(request);
          return Empty.getDefaultInstance();
        });
  }

  private void saveCustomerAccountNlpToZooKeeper(CustomerAccountNlp customerAccountNlp)
      throws StatusException {
    CustomerAccountNlpZookeeperInfo zkInfo =
        new CustomerAccountNlpZookeeperInfo(customerAccountNlp);

    try {
      String path =
          String.join(
              "/",
              Arrays.asList(
                  CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH,
                  zkInfo.getAccountId(),
                  zkInfo.getInstanceId()));
      String data = objectMapper.writeValueAsString(zkInfo);
      zkClient.set(path, data);
    } catch (JsonProcessingException e) {
      log.error("Caught Exception: ", e);
      throw new StatusException(
          Status.INTERNAL.withDescription("Unable to save nlp deployment info to zookeeper"));
    }
  }
}
