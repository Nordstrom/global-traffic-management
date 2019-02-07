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
package com.nordstrom.nfe;

import static com.nordstrom.apikey.Result.resultFromFuture;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.Empty;
import com.nordstrom.apikey.ApiKeyTestClient;
import com.nordstrom.apikey.Result;
import com.nordstrom.gatekeeper.grpc.AuthZ;
import com.nordstrom.gatekeeper.util.GatekeeperTestClient;
import com.nordstrom.gatekeeper.util.GatekeeperTestServer;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.nfe.rules.NfeProxyRule;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ApiKeyGrpcFunctionsTest {
  private ApiKeyTestClient apiKeyTestClient;

  @RegisterExtension
  static GatekeeperTestServer gatekeeperTestServer = new GatekeeperTestServer(7777);

  @RegisterExtension GatekeeperTestClient gatekeeperTestClient = new GatekeeperTestClient(7777);
  @RegisterExtension NfeProxyRule proxyRule = new NfeProxyRule();

  @BeforeEach
  public void beforeEach() throws Exception {
    proxyRule.startProxy();
    apiKeyTestClient = proxyRule.getApiKeyTestClient();
  }

  @Test
  public void testKeyGeneration() throws Exception {
    // when a client generates an api key
    Result<ApiKey> result =
        resultFromFuture(() -> apiKeyTestClient.generateApiKey("TestOU", "TestService", "TestKey"));

    // then the correct result is returned
    assertNotNull(result.getValue());
    assertEquals("TestService", result.getValue().getServiceName());
    assertEquals("TestKey", result.getValue().getKeyName());
    assertNotNull(result.getValue().getKey());

    // and then gatekeeper is informed of the apikey authorization
    AuthZ authZ = gatekeeperTestClient.getClient().listSubjectAuthZ(result.getValue().getKey());
    assertEquals(result.getValue().getKey(), authZ.getId());
    assertTrue(authZ.getPermissionsList().contains("apikey:testou:testservice:*"));
  }

  @Test
  public void testKeyGenerationDuplicateService() throws Exception {
    Result<ApiKey> result1 =
        resultFromFuture(() -> apiKeyTestClient.generateApiKey("TestOU", "AcmeService", "AcmeKey"));
    Result<ApiKey> result2 =
        resultFromFuture(
            () -> apiKeyTestClient.generateApiKey("OtherOU", "AcmeService", "AcmeKey"));
    assertNotNull(result1.getValue());
    assertNotNull(result2.getValue());
    assertEquals("AcmeService", result1.getValue().getServiceName());
    assertEquals("AcmeService", result2.getValue().getServiceName());
    assertEquals("AcmeKey", result1.getValue().getKeyName());
    assertEquals("AcmeKey", result2.getValue().getKeyName());
    assertNotNull(result1.getValue().getKey());
    assertNotNull(result2.getValue().getKey());
    assertNotEquals(result1.getValue().getKey(), result2.getValue().getKey());
  }

  @Test
  public void testKeyGenerationProducesUniqueKeysForSameService() throws Exception {
    Result<ApiKey> apiKey1 =
        resultFromFuture(
            () -> apiKeyTestClient.generateApiKey("TestOU", "TestService", "TestKey1"));
    Result<ApiKey> apiKey2 =
        resultFromFuture(
            () -> apiKeyTestClient.generateApiKey("TestOU", "TestService", "TestKey2"));
    assertNotEquals(apiKey1.getValue(), apiKey2.getValue());
  }

  // todo: (WK) (BR)
  @Disabled("disabled until coredb integration enforces uniqueness")
  @Test
  public void testKeyGenerationProducesErrorWhenNamedTheSame() throws Exception {

    final Result<ApiKey> result1 =
        resultFromFuture(() -> apiKeyTestClient.generateApiKey("TestOU", "TestService", "TestKey"));
    assertNotNull(result1.getValue());
    assertEquals("TestService", result1.getValue().getServiceName());
    assertEquals("TestKey", result1.getValue().getKeyName());
    assertNotNull(result1.getValue().getKey());

    final Result<ApiKey> result2 =
        resultFromFuture(() -> apiKeyTestClient.generateApiKey("TestOU", "TestService", "TestKey"));
    Throwable error = result2.getError();
    assertNotNull(error);
    assertEquals(StatusRuntimeException.class, error.getClass());
    assertEquals(INVALID_ARGUMENT, ((StatusRuntimeException) error).getStatus().getCode());
    assertEquals(
        "TestOU.TestService.TestKey already in use",
        ((StatusRuntimeException) error).getStatus().getDescription());
  }

  @Test
  public void testRevokeKeys() throws Exception {
    // given 3 services generate keys who can't think of unique service and key name alias'
    final Result<ApiKey> result =
        resultFromFuture(
            () -> apiKeyTestClient.generateApiKey("TestOU", "AcmeService", "AcmeKey1"));

    // then all the keys are generated without collision
    ApiKey key1 = result.getValue();

    // when "TestOU" revokes it's "AcmeService", "TestKey1" key
    final Result<Empty> revokeResult =
        resultFromFuture(
            () ->
                apiKeyTestClient.revokeApiKey("TestOU", "AcmeService", "AcmeKey1", key1.getKey()));
    assertNull(revokeResult.getError());

    // then the gatekeeper authorization is removed for key 1
    AuthZ authZ = gatekeeperTestClient.getClient().listSubjectAuthZ(key1.getKey());
    assertEquals(key1.getKey(), authZ.getId());
    assertTrue(authZ.getPermissionsList().isEmpty());
  }
}
