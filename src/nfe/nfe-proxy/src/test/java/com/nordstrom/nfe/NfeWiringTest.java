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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.nordstrom.nfe.rules.NfeProxyRule;
import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NfeWiringTest {

  @RegisterExtension public final NfeProxyRule proxyRule = new NfeProxyRule();

  @Test
  public void sanityCheck() throws Exception {
    proxyRule.getMockWebServer().enqueue(new MockResponse().setBody("hello, world!"));

    Request request = buildRequest(proxyRule.getMockWebServer().getPort());
    Response response = get(request);
    assertEquals(200, response.code());
    RecordedRequest servedRequest = proxyRule.getMockWebServer().takeRequest();
    assertEquals(
        "/v1/teamyo/slapservice/api/v1/hello/hi", servedRequest.getRequestUrl().encodedPath());
  }

  @Test
  public void testProxy() throws Exception {
    proxyRule.startProxy();
    proxyRule
        .getMockWebServer()
        .setDispatcher(
            new Dispatcher() {
              @Override
              public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setResponseCode(200);
              }
            });

    Request request = buildRequest(proxyRule.getProxyPort());
    Response response = get(request);
    assertEquals(200, response.code());
    RecordedRequest servedRequest = proxyRule.getMockWebServer().takeRequest();
    assertEquals("/hi", servedRequest.getRequestUrl().encodedPath());
  }

  private Request buildRequest(int port) throws IOException {
    String path = "https://127.0.0.1:" + port + "/v1/teamyo/slapservice/api/v1/hello/hi";
    return new Request.Builder().url(path).build();
  }

  private Response get(Request request) throws IOException {
    try (Response response = proxyRule.getUnsafeOkhttpClient().newCall(request).execute()) {
      if (response.code() == 404) {
        fail(request.url().encodedPath() + " not supported");
      }
      return response;
    }
  }
}
