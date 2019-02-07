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

import com.nordstrom.gatekeeper.GatekeeperClient;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Junit5 GatekeeperClient extension which creates a real {@link GatekeeperClient} for use in
 * integration tests. The {@link GatekeeperClient} resource and lifecycle is managed by this
 * extension.
 *
 * <p>Note the in-memory local Dynamodb is cleared {@link org.junit.jupiter.api.AfterEach}.
 *
 * <p>Important: This requires shared native libraries libsqlite4java<arch>.so which are in
 * "src/test/native-libs" for DynamoDbLocal.
 */
public class GatekeeperTestClient implements BeforeEachCallback, AfterEachCallback {

  private GatekeeperClient client;
  private final int port;
  private final String tlsConfigPath;

  public GatekeeperTestClient() {
    this(8080);
  }

  public GatekeeperTestClient(int port) {
    this(port, "tls.conf");
  }

  public GatekeeperTestClient(int port, String tlsConfigPath) {
    this.port = port;
    this.tlsConfigPath = tlsConfigPath;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    client = new GatekeeperClient("localhost", port, tlsConfigPath);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    client.stop();
  }

  public GatekeeperClient getClient() {
    return client;
  }
}
