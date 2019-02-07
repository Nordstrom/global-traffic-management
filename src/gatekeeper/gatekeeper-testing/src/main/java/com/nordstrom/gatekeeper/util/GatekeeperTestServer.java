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
package com.nordstrom.gatekeeper.util;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.nordstrom.gatekeeper.GatekeeperConfig;
import com.nordstrom.gatekeeper.GatekeeperServer;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Junit5 GatekeeperServer extension which creates a real {@link GatekeeperServer} for use in
 * integration tests. The {@link GatekeeperServer} as well as it's configured in-memory local
 * Dynamodb resource and lifecycle is managed by this extension.
 *
 * <p>Note: Use this extension as a static test class variable as the local Dynamodb is created
 * {@link org.junit.jupiter.api.BeforeAll} and torn down {@link org.junit.jupiter.api.AfterAll}.
 * Also note the in-memory local Dynamodb is cleared {@link org.junit.jupiter.api.AfterEach}.
 */
public class GatekeeperTestServer extends GatekeeperTestDao {

  private GatekeeperServer server;
  private final int port;

  public GatekeeperTestServer(int port) {
    this.port = port;
  }

  public GatekeeperTestServer() {
    this.port = 8080;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    super.beforeEach(context);
    // override the port
    GatekeeperConfig gatekeeperConfig = spy(new GatekeeperConfig());
    when(gatekeeperConfig.getPort()).thenReturn(port);
    when(serviceLocator.getGatekeeperConfig()).thenReturn(gatekeeperConfig);

    // start the server
    server = serviceLocator.getGatekeeperServer();
    server.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    super.afterEach(context);
    server.stop();
  }

  public GatekeeperServer getServer() {
    return server;
  }
}
