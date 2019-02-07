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

import static org.junit.jupiter.api.Assertions.*;

import com.nordstrom.keymaster.util.KeymasterTestClient;
import com.nordstrom.keymaster.util.KeymasterTestServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class KeymasterServerIntegrationTest {

  @RegisterExtension static KeymasterTestServer testServer = new KeymasterTestServer();

  @RegisterExtension KeymasterTestClient testClient = new KeymasterTestClient();

  @Test
  public void testServer() throws Exception {
    CertificateResponse response =
        testClient.getClient().certificate("somebody", CertificateRequest.DataClassification.GTM);

    assertTrue(response.hasSuccess());
    assertFalse(response.hasError());
    assertNotEquals(0, response.getSuccess().getCertChainCount());
    assertNotNull(response.getSuccess().getExpiration());
    assertNotNull(response.getSuccess().getPrivateKey());
  }
}
