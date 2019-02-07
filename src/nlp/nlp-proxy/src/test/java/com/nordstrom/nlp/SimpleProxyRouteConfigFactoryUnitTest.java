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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleProxyRouteConfigFactoryUnitTest extends Assert {

  SimpleProxyRouteConfigFactory subject;

  @Before
  public void setUp() {
    Config config = ConfigFactory.load().getConfig("nlpStateUnitTest");
    NlpConfig nlpConfig = new NlpConfig(config);
    subject = new SimpleProxyRouteConfigFactory(nlpConfig);
  }

  @Test
  public void testBuild() {

    String route1Path = "/path1/";
    String route1Client1Ipaddress = "1.2.3.4";
    String route1Client2Ipaddress = "1.2.3.5";
    int route1ClientPort = 1234;
    boolean route1ClientTls = false;

    String route2Path = "/path2/";
    String route2Client1Ipaddress = "2.2.3.4";
    String route2Client2Ipaddress = "2.2.3.5";
    int route2ClientPort = 5678;
    boolean route2ClientTls = true;

    String route3Path = "/path2/";

    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(
        new DynamicClientConfig(route1Client1Ipaddress, route1ClientPort, route1ClientTls, null));
    clientConfigs1.add(
        new DynamicClientConfig(route1Client2Ipaddress, route1ClientPort, route1ClientTls, null));
    DynamicRouteConfig expectedRouteConfig1 = new DynamicRouteConfig(route1Path, clientConfigs1);

    List<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(
        new DynamicClientConfig(route2Client1Ipaddress, route2ClientPort, route2ClientTls, null));
    clientConfigs2.add(
        new DynamicClientConfig(route2Client2Ipaddress, route2ClientPort, route2ClientTls, null));
    DynamicRouteConfig expectedRouteConfig2 = new DynamicRouteConfig(route2Path, clientConfigs2);

    List<DynamicClientConfig> clientConfigs3 = new ArrayList<>();
    DynamicRouteConfig expectedRouteConfig3 = new DynamicRouteConfig(route3Path, clientConfigs3);

    List<DynamicRouteConfig> inputConfigs = new ArrayList<>();
    inputConfigs.add(expectedRouteConfig1);
    inputConfigs.add(expectedRouteConfig2);
    inputConfigs.add(expectedRouteConfig3);

    List<ProxyRouteConfig> results = subject.build(inputConfigs);

    ProxyRouteConfig prc1 = results.get(0);
    ProxyRouteConfig prc2 = results.get(1);
    ProxyRouteConfig prc3 = results.get(2);

    assertEquals(route1Path, prc1.path());
    assertEquals(route1Client1Ipaddress, prc1.clientConfigs().get(0).remote().getHostString());
    assertEquals(route1Client2Ipaddress, prc1.clientConfigs().get(1).remote().getHostString());
    assertEquals(route1ClientPort, prc1.clientConfigs().get(0).remote().getPort());
    assertEquals(route1ClientPort, prc1.clientConfigs().get(1).remote().getPort());
    assertEquals(route1ClientTls, prc1.clientConfigs().get(0).isTlsEnabled());
    assertEquals(route1ClientTls, prc1.clientConfigs().get(1).isTlsEnabled());

    assertEquals(route2Path, prc2.path());
    assertEquals(route2Client1Ipaddress, prc2.clientConfigs().get(0).remote().getHostString());
    assertEquals(route2Client2Ipaddress, prc2.clientConfigs().get(1).remote().getHostString());
    assertEquals(route2ClientPort, prc2.clientConfigs().get(0).remote().getPort());
    assertEquals(route2ClientPort, prc2.clientConfigs().get(1).remote().getPort());
    assertEquals(route2ClientTls, prc2.clientConfigs().get(0).isTlsEnabled());
    assertEquals(route2ClientTls, prc2.clientConfigs().get(1).isTlsEnabled());

    // route 3 has no client configs
    assertEquals(route3Path, prc3.path());
    assertEquals(0, prc3.clientConfigs().size());
  }
}
