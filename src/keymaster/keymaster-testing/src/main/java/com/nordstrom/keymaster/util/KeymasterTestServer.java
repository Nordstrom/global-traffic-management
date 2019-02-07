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
package com.nordstrom.keymaster.util;

import com.nordstrom.keymaster.KeymasterServer;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Junit5 KeymasterServer extension which creates a real {@link KeymasterServer} for use in
 * integration tests. The {@link KeymasterServer} as well as it's configured in-memory local
 * Dynamodb resource and lifecycle is managed by this extension.
 *
 * <p>Note: Use this extension as a static test class variable as the local Dynamodb is created
 * {@link org.junit.jupiter.api.BeforeAll} and torn down {@link org.junit.jupiter.api.AfterAll}.
 * Also note the in-memory local Dynamodb is cleared {@link org.junit.jupiter.api.AfterEach}.
 */
public class KeymasterTestServer extends KeymasterTestDao {

  private KeymasterServer server;
  private final int port;

  public KeymasterTestServer(int port) {
    this.port = port;
  }

  public KeymasterTestServer() {
    this.port = 8080;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    super.beforeEach(context);
    server = serviceLocator.getKeymasterServer();
    server.start(port);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    super.afterEach(context);
    server.stop();
  }

  public KeymasterServer getServer() {
    return server;
  }
}
