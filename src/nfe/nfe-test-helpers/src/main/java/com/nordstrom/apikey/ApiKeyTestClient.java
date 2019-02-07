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
package com.nordstrom.apikey;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.apikey.ApiKeyerGrpc;
import com.nordstrom.gtm.apikey.KeyRequest;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.concurrent.TimeUnit;

public class ApiKeyTestClient {

  private final ManagedChannel channel;
  private final ApiKeyerGrpc.ApiKeyerFutureStub futureStub;

  public static ApiKeyTestClient run(int port) throws Exception {
    return new ApiKeyTestClient("127.0.0.1", port);
  }

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  private ApiKeyTestClient(String host, int port) {
    this(build(host, port));
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  private ApiKeyTestClient(ManagedChannel channel) {
    this.channel = channel;
    futureStub = ApiKeyerGrpc.newFutureStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdownNow();
    channel.awaitTermination(10, TimeUnit.SECONDS);
  }

  /** Generate ApiKey */
  public ListenableFuture<ApiKey> generateApiKey(
      String teamName, String serviceName, String keyName) {
    KeyRequest request =
        KeyRequest.newBuilder()
            .setTeamName(teamName)
            .setServiceName(serviceName)
            .setKeyName(keyName)
            .build();

    return futureStub.generateNewApiKey(request);
  }

  private static ManagedChannel build(String host, int port) {
    return NettyChannelBuilder.forAddress(host, port)
        .overrideAuthority(host + ":" + port)
        .sslContext(
            SslContextFactory.buildClientContext(
                TlsConfig.fromConfig("xio.serverTemplate.settings.tls"),
                InsecureTrustManagerFactory.INSTANCE))
        .build();
  }

  public ListenableFuture<Empty> revokeApiKey(
      String teamName, String serviceName, String keyName, String keyValue) {
    ApiKey request =
        ApiKey.newBuilder()
            .setTeamName(teamName)
            .setServiceName(serviceName)
            .setKeyName(keyName)
            .setKey(keyValue)
            .build();
    return futureStub.revokeApiKey(request);
  }
}
