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

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.concurrent.TimeUnit;

public class GrpcClient {
  private final ManagedChannel channel;

  public static ManagedChannel buildChannel(String host, int port) {
    return NettyChannelBuilder.forAddress(host, port)
        .overrideAuthority(host + ":" + port)
        .sslContext(
            SslContextFactory.buildClientContext(
                TlsConfig.fromConfig("xio.serverTemplate.settings.tls", ConfigFactory.load()),
                InsecureTrustManagerFactory.INSTANCE))
        .build();
  }

  private GrpcClient(ManagedChannel channel) {
    this.channel = channel;
  }

  public static GrpcClient run(ManagedChannel channel) {
    return new GrpcClient(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }
}
