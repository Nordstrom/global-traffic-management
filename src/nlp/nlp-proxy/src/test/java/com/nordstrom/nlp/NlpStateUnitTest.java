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
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.http.ProxyRouteState;
import com.xjeffrose.xio.http.RouteState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NlpStateUnitTest extends Assert {

  private NlpConfig nlpConfig;
  private NlpState subject;

  private ImmutableList<DynamicRouteConfig> previousConfig;
  private ImmutableList<DynamicRouteConfig> updatedConfig;

  private ImmutableMap<String, RouteState> expectedRouteMap;

  String path1 = "/path1/";
  String path2 = "/path2/";
  String ip1a = "1.2.3.4";
  String ip1b = "1.2.3.5";
  String ip2a = "2.2.3.4";
  String ip2b = "2.2.3.5";

  @Before
  public void setUp() {
    Config config = ConfigFactory.load().getConfig("nlpStateUnitTest");
    nlpConfig = new NlpConfig(config);

    // start with empty config
    previousConfig = ImmutableList.copyOf(new ArrayList<DynamicRouteConfig>());
    // create fancy new config with some routes
    updatedConfig = ImmutableList.copyOf(buildUpdatedConfig());

    SimpleProxyRouteConfigFactory simpleProxyRouteConfigFactory =
        new SimpleProxyRouteConfigFactory(nlpConfig);
    subject = new NlpState(nlpConfig, previousConfig, simpleProxyRouteConfigFactory);
    // client factory is null because it is a don't care in this scenario
    expectedRouteMap =
        new RouteStates(
                simpleProxyRouteConfigFactory.build(previousConfig), nlpConfig, subject, null)
            .routeMap();
  }

  private List<DynamicRouteConfig> buildUpdatedConfig() {
    List<DynamicRouteConfig> updatedConfig = new ArrayList<DynamicRouteConfig>();

    List<DynamicClientConfig> clientConfigs1 = new ArrayList<>();
    clientConfigs1.add(new DynamicClientConfig(ip1a, 1234, false, null));
    clientConfigs1.add(new DynamicClientConfig(ip1b, 1234, false, null));

    List<DynamicClientConfig> clientConfigs2 = new ArrayList<>();
    clientConfigs2.add(new DynamicClientConfig(ip2a, 5678, true, null));
    clientConfigs2.add(new DynamicClientConfig(ip2b, 5678, true, null));

    updatedConfig.add(new DynamicRouteConfig(path1, clientConfigs1));
    updatedConfig.add(new DynamicRouteConfig(path2, clientConfigs2));
    return updatedConfig;
  }

  @Test
  public void testReloadConfig_all_healthy() throws Exception {
    HashMap<String, ImmutableSet<String>> heatlhyMap = new HashMap<>();
    heatlhyMap.put(path1, ImmutableSet.copyOf(Arrays.asList(ip1a, ip1b)));
    heatlhyMap.put(path2, ImmutableSet.copyOf(Arrays.asList(ip2a, ip2b)));

    CountDownLatch countDownLatch = new CountDownLatch(2);

    // set the test hook so we know when stuff is done
    subject.routesUpdatedConsumer =
        () -> {
          countDownLatch.countDown();
        };

    subject.updateHealthyHostMap(ImmutableMap.copyOf(heatlhyMap));
    subject.reloadRouteStates(previousConfig, updatedConfig);

    // wait for the routes to fully update
    countDownLatch.await(5, TimeUnit.SECONDS);

    val updatedRoutes = subject.routes();
    assertNotEquals(expectedRouteMap, updatedRoutes);
  }

  @Test
  public void testReloadConfig_some_healthy() throws Exception {
    HashMap<String, ImmutableSet<String>> heatlhyMap = new HashMap<>();
    heatlhyMap.put(path1, ImmutableSet.copyOf(Arrays.asList(ip1a)));
    heatlhyMap.put(path2, ImmutableSet.copyOf(Arrays.asList(ip2a)));

    CountDownLatch countDownLatch = new CountDownLatch(2);

    // set the test hook so we know when stuff is done
    subject.routesUpdatedConsumer =
        () -> {
          countDownLatch.countDown();
        };

    subject.updateHealthyHostMap(ImmutableMap.copyOf(heatlhyMap));
    subject.reloadRouteStates(previousConfig, updatedConfig);

    // wait for the routes to fully update
    countDownLatch.await(5, TimeUnit.SECONDS);

    // get the ROUTES!
    val updatedRoutes = subject.routes();

    // make sure we only have two routes
    val numberOfRoutes = updatedRoutes.entrySet().size();
    assertEquals(2, numberOfRoutes);

    // get the routes for path 1 and make sure there is only one client
    ProxyRouteState routeState1 = (ProxyRouteState) updatedRoutes.get(path1);
    val numberOfClients1 = routeState1.clientStates().size();
    assertEquals(1, numberOfClients1);

    // make sure that the 1 client for path1 has the right ip address that matches
    val clientState1 = routeState1.clientStates().get(0);
    assertEquals(ip1a, clientState1.remote.getHostString());

    // get the routes for path 1 and make sure there is only one client
    ProxyRouteState routeState2 = (ProxyRouteState) updatedRoutes.get(path2);
    val numberOfClients2 = routeState2.clientStates().size();
    assertEquals(1, numberOfClients2);

    // make sure that the 1 client for path1 has the right ip address that matches
    val clientState2 = routeState2.clientStates().get(0);
    assertEquals(ip2a, clientState2.remote.getHostString());
  }

  @Test
  public void testReloadConfig_no_healthy() throws Exception {
    HashMap<String, ImmutableSet<String>> heatlhyMap = new HashMap<>();
    heatlhyMap.put(path1, ImmutableSet.copyOf(Arrays.asList()));
    heatlhyMap.put(path2, ImmutableSet.copyOf(Arrays.asList()));

    CountDownLatch countDownLatch = new CountDownLatch(2);

    // set the test hook so we know when stuff is done
    subject.routesUpdatedConsumer =
        () -> {
          countDownLatch.countDown();
        };

    subject.updateHealthyHostMap(ImmutableMap.copyOf(heatlhyMap));
    subject.reloadRouteStates(previousConfig, updatedConfig);

    // wait for the routes to fully update
    countDownLatch.await(5, TimeUnit.SECONDS);

    // get the ROUTES!
    val updatedRoutes = subject.routes();

    // make sure we only have two routes
    val numberOfRoutes = updatedRoutes.entrySet().size();
    assertEquals(2, numberOfRoutes);

    // get the routes for path 1 and make sure there is only one client
    ProxyRouteState routeState1 = (ProxyRouteState) updatedRoutes.get(path1);
    val numberOfClients1 = routeState1.clientStates().size();
    assertEquals(0, numberOfClients1);

    // get the routes for path 1 and make sure there is only one client
    ProxyRouteState routeState2 = (ProxyRouteState) updatedRoutes.get(path2);
    val numberOfClients2 = routeState2.clientStates().size();
    assertEquals(0, numberOfClients2);
  }
}
