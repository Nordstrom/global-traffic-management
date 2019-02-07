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
package com.nordstrom.nfe.rules;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;

import com.nordstrom.apikey.ApiKeyTestClient;
import com.nordstrom.nfe.NfeState;
import com.nordstrom.nfe.OkHttpUnsafe;
import com.nordstrom.nfe.bootstrap.NfeApplicationBootstrap;
import com.nordstrom.nfe.config.NfeConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigResolveOptions;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.Application;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class NfeProxyRule implements BeforeEachCallback, AfterEachCallback {

  private OkHttpClient okHttpClient;
  private OkHttpClient okHttpClientH2;
  private MockWebServer mockWebServer;
  private Application application = null;
  private ApiKeyTestClient apiKeyTestClient = null;
  private NfeState nfeState;
  private int proxyPort = 0;

  private static Config load(Config overrides) {
    Config defaultOverrides = ConfigFactory.defaultOverrides();
    Config application = ConfigFactory.defaultApplication();
    Config reference = ConfigFactory.defaultReference();
    ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults();
    return overrides
        .withFallback(defaultOverrides)
        .withFallback(application)
        .withFallback(reference)
        .resolve(resolveOptions);
  }

  public void startProxy() {
    Config config =
        load(ConfigFactory.parseString("testing.port = " + mockWebServer.getPort()))
            // bind to a random port
            .withValue("nfe.application.servers.nfe-main.settings.bindPort", fromAnyRef(0));

    nfeState = new NfeState(new NfeConfig(config));
    application = new NfeApplicationBootstrap(nfeState).build();
    InetSocketAddress proxy = application.instrumentation("nfe-main").boundAddress();
    proxyPort = proxy.getPort();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    TlsConfig tlsConfig = TlsConfig.fromConfig("nfe.h1ProxyClient.settings.tls");
    mockWebServer = OkHttpUnsafe.getSslMockWebServer(tlsConfig);
    mockWebServer.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (application != null) {
      application.close();
    }

    if (okHttpClient != null) {
      okHttpClient.connectionPool().evictAll();
    }

    if (okHttpClientH2 != null) {
      okHttpClientH2.connectionPool().evictAll();
    }

    try {
      mockWebServer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (apiKeyTestClient != null) {
      try {
        apiKeyTestClient.shutdown();
      } catch (InterruptedException ignored) {
      }
    }
  }

  public MockWebServer getMockWebServer() {
    return mockWebServer;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public OkHttpClient getUnsafeOkhttpClient() {
    if (okHttpClient == null) {
      okHttpClient =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Collections.singletonList(HTTP_1_1))
              .build();
    }
    return okHttpClient;
  }

  public OkHttpClient getUnsafeOkhttpClientH2() {
    if (okHttpClientH2 == null) {
      okHttpClientH2 =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Arrays.asList(HTTP_2, HTTP_1_1))
              .build();
    }
    return okHttpClientH2;
  }

  public NfeState getNfeState() {
    return nfeState;
  }

  public ApiKeyTestClient getApiKeyTestClient() {
    if (apiKeyTestClient == null) {
      try {
        apiKeyTestClient = ApiKeyTestClient.run(getProxyPort());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return apiKeyTestClient;
  }
}
