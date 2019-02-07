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
package com.nordstrom.gatekeeper;

import com.xjeffrose.xio.tls.SslContextFactory;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatekeeperServer {

  private Server server = null;
  private final GatekeeperGrpcService gatekeeperGrpcService;
  private final GatekeeperGrpcManagementService gatekeeperGrpcManagementService;
  private final GatekeeperConfig gatekeeperConfig;

  public GatekeeperServer(
      GatekeeperGrpcService gatekeeperGrpcService,
      GatekeeperGrpcManagementService gatekeeperGrpcManagementService,
      GatekeeperConfig config) {
    this.gatekeeperGrpcService = gatekeeperGrpcService;
    this.gatekeeperGrpcManagementService = gatekeeperGrpcManagementService;
    this.gatekeeperConfig = config;
  }

  public void start() throws IOException {
    server =
        NettyServerBuilder.forPort(gatekeeperConfig.getPort())
            .sslContext(SslContextFactory.buildServerContext(gatekeeperConfig.getTlsConfig()))
            .addService(gatekeeperGrpcService)
            .addService(gatekeeperGrpcManagementService)
            .build()
            .start();
    log.debug("gatekeeper started");
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public void stop() {
    if (server != null) {
      log.debug("gatekeeper stopping");
      server.shutdown();
      server = null;
    }
  }

  void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
