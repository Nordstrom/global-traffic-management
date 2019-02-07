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
package com.nordstrom.keymaster;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeymasterServer {

  private Server server = null;
  private final KeymasterGrpcService keymasterGrpcService;

  public KeymasterServer(KeymasterGrpcService keymasterGrpcService) {
    this.keymasterGrpcService = keymasterGrpcService;
  }

  public void start(int port) throws IOException {
    server =
        ServerBuilder.forPort(port) // this is listening on 0.0.0.0:port
            .addService(keymasterGrpcService)
            .build()
            .start();
    log.debug("keymaster started");
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public void stop() {
    if (server != null) {
      log.debug("keymaster stopping");
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
