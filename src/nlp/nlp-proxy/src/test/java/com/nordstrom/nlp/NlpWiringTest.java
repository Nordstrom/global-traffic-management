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
package com.nordstrom.nlp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import com.xjeffrose.xio.test.TestConfigFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.net.ssl.KeyManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NlpWiringTest extends Assert {

  public static Config load(int port) {
    return TestConfigFactory.load(ConfigFactory.parseString("testing.port = " + port));
  }

  OkHttpClient client;
  MockWebServer server;

  @Before
  public void setup() throws Exception {
    TlsConfig tls = TlsConfig.builderFrom(ConfigFactory.load().getConfig("testing.tls")).build();
    KeyManager[] keyManagers =
        OkHttpUnsafe.getKeyManagers(tls.getPrivateKey(), tls.getCertificateAndChain());
    server = OkHttpUnsafe.getSslMockWebServer(keyManagers);
    client = OkHttpUnsafe.getUnsafeClient();
  }

  @After
  public void teardown() throws Exception {
    server.close();
  }

  @Test
  public void testProxy() throws Exception {
    Config config = load(server.getPort());

    NlpConfig nlpConfig = new NlpConfig(config);

    String ipAddress = "127.0.0.1";
    String path = "/fives/";

    // start with empty config
    List<DynamicRouteConfig> initialConfig = new ArrayList<DynamicRouteConfig>();
    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig(ipAddress, server.getPort(), true, null));
    initialConfig.add(new DynamicRouteConfig(path, clientConfigs1));

    SimpleProxyRouteConfigFactory simpleProxyRouteConfigFactory =
        new SimpleProxyRouteConfigFactory(nlpConfig);
    NlpState nlpState =
        new NlpState(nlpConfig, ImmutableList.copyOf(initialConfig), simpleProxyRouteConfigFactory);

    HashMap<String, ImmutableSet<String>> healthyMap = new HashMap<>();
    healthyMap.put(path, ImmutableSet.copyOf(Arrays.asList(ipAddress)));
    ImmutableMap<String, ImmutableSet<String>> allHealthyMap = ImmutableMap.copyOf(healthyMap);
    nlpState.updateHealthyHostMap(allHealthyMap);
    Thread.sleep(5000);

    Application application = new NlpApplicationBootstrap(nlpState).build();
    server.enqueue(new MockResponse());

    InetSocketAddress proxy = application.instrumentation("nlp-main").boundAddress();

    Request request = buildRequest(proxy);
    Response response = get(request);
    RecordedRequest servedRequest = server.takeRequest();

    application.close();
    assertEquals(200, response.code());
    assertEquals("/slap", servedRequest.getRequestUrl().encodedPath());
  }

  protected Request buildRequest(InetSocketAddress address) throws IOException {
    StringBuilder path =
        new StringBuilder("https://")
            .append("127.0.0.1")
            .append(":")
            .append(address.getPort())
            .append("/fives/slap");
    return new Request.Builder().url(path.toString()).build();
  }

  protected Response get(Request request) throws IOException {
    return client.newCall(request).execute();
  }
}
