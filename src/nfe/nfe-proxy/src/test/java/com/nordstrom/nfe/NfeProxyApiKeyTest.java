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

import static org.junit.jupiter.api.Assertions.*;

import com.nordstrom.gatekeeper.util.GatekeeperTestServer;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.nfe.rules.NfeProxyRule;
import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NfeProxyApiKeyTest {

  @RegisterExtension public final NfeProxyRule proxyRule = new NfeProxyRule();

  @RegisterExtension
  public static final GatekeeperTestServer testServer = new GatekeeperTestServer(7777);

  @BeforeEach
  public void beforeEach() throws Exception {
    proxyRule.startProxy();
    proxyRule
        .getMockWebServer()
        .setDispatcher(
            new Dispatcher() {
              @Override
              public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setBody("hi").setResponseCode(200);
              }
            });
  }

  @Test
  public void testApiKeyProxyRequest() throws Exception {
    // given a valid api key
    ApiKey key =
        proxyRule.getApiKeyTestClient().generateApiKey("TeamYo", "SlapService", "FooKey").get();

    // when a request is made with the correct key
    Request request = buildRequest(proxyRule.getProxyPort(), key.getKey()); // correct key
    Response responseH1 = get(request, false);
    Response responseH2 = get(request, true);

    // then an success response is returned
    assertEquals(200, responseH1.code());
    assertEquals(Protocol.HTTP_1_1, responseH1.protocol());

    assertEquals(200, responseH2.code());
    assertEquals(Protocol.HTTP_2, responseH2.protocol());
  }

  @Test
  public void testApiKeyProxyRequestNoApiKey() throws Exception {
    // given there are zero api keys
    testServer.getDao().deleteTables();

    // when a request is made with any key
    Request request =
        buildRequest(
            proxyRule.getProxyPort(), "d72e7b1071ab44b8be420d2624a607f0"); // an unknown key
    Response responseH1 = get(request, false);
    Response responseH2 = get(request, true);

    // then an unauthorized response is returned
    assertEquals(401, responseH1.code());
    assertEquals(Protocol.HTTP_1_1, responseH1.protocol());

    assertEquals(401, responseH2.code());
    assertEquals(Protocol.HTTP_2, responseH2.protocol());
  }

  @Test
  public void testApiKeyProxyRequestWrongApiKey() throws Exception {
    // given there gatekeeper grants the "permissionNeeded of apikey:encoded_path" for an api key
    // "d72e7b1071ab44b8be420d2624a607f0"
    proxyRule.getApiKeyTestClient().generateApiKey("TeamYo", "SlapService", "FooKey").get();

    // when a request is made with the wrong key
    Request request = buildRequest(proxyRule.getProxyPort(), "wrong_key");
    Response responseH1 = get(request, false);
    Response responseH2 = get(request, true);

    // then an unauthorized response is returned
    assertEquals(401, responseH1.code());
    assertEquals(Protocol.HTTP_1_1, responseH1.protocol());

    assertEquals(401, responseH2.code());
    assertEquals(Protocol.HTTP_2, responseH2.protocol());
  }

  private Request buildRequest(int port, @Nullable String apiKey) throws IOException {
    String path = "https://127.0.0.1:" + port + "/v1/teamyo/slapservice/api/v1/fives/hand/slap";
    Request.Builder builder = new Request.Builder().url(path);
    if (apiKey != null) {
      builder.header("apiKey", apiKey);
    }
    return builder.build();
  }

  private Response get(Request request, boolean h2) throws IOException {
    final OkHttpClient client;
    if (h2) {
      client = proxyRule.getUnsafeOkhttpClientH2();
    } else {
      client = proxyRule.getUnsafeOkhttpClient();
    }
    try (Response response = client.newCall(request).execute()) {
      if (response.code() == 404) {
        fail(request.url().encodedPath() + " not supported");
      }
      return response;
    }
  }
}
