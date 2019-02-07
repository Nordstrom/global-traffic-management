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
package com.nordstrom.nlp.HealthChecks;

import java.net.URL;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpClientWrapper {
  private final OkHttpClient client;
  private final RequestBuilderFactory builderFactory;

  public OkHttpClientWrapper(OkHttpClient client, RequestBuilderFactory builderFactory) {
    this.client = client;
    this.builderFactory = builderFactory;
  }

  public Request buildRequest(String ipAddress, int port, String path, boolean tlsEnabled)
      throws java.net.MalformedURLException {
    String scheme = "https://";
    if (!tlsEnabled) {
      scheme = "http://";
    }
    String requestPath = scheme + ipAddress + ":" + port + path;
    return builderFactory.createBuilder().url(new URL(requestPath)).build();
  }

  public Response executeRequest(Request request) throws java.io.IOException {
    return client.newCall(request).execute();
  }
}
