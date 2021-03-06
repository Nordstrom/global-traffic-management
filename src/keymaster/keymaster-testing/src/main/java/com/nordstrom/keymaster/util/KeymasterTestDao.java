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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.nordstrom.keymaster.KeymasterDao;
import com.nordstrom.keymaster.KeymasterServiceLocator;
import org.junit.jupiter.api.extension.*;

/**
 * Junit5 KeymasterDao extension which creates a real {@link KeymasterDao} for use in integration
 * tests. The {@link KeymasterDao} using an in-memory local Dynamodb resource is managed by this
 * extension.
 *
 * <p>Note: Use this extension as a static test class variable as the local Dynamodb is created
 * {@link org.junit.jupiter.api.BeforeAll} and torn down {@link org.junit.jupiter.api.AfterAll}.
 *
 * <p>Important: This requires shared native libraries libsqlite4java<arch>.so which are in
 * "src/test/native-libs" for DynamoDbLocal.
 */
public class KeymasterTestDao
    implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

  KeymasterServiceLocator serviceLocator;
  private DynamoDBProxyServer server;
  private KeymasterDao dao;

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    server.stop();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    dao.deleteTables();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    System.setProperty("sqlite4java.library.path", "src/test/native-libs");
    String port = "8000";
    server =
        ServerRunner.createServerFromCommandLineArgs(new String[] {"-inMemory", "-port", port});
    server.start();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    serviceLocator = spy(new KeymasterServiceLocator());
    dao = spy(new KeymasterDao("localhost"));
    dao.guaranteeTables();
    when(serviceLocator.getDao()).thenReturn(dao);
  }

  public KeymasterDao getDao() {
    return dao;
  }
}
