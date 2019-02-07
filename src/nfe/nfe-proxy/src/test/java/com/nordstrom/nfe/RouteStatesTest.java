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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.nordstrom.nfe.config.KubernetesRoutingConfig;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.nlpmanagement.AccountInfo;
import com.nordstrom.nfe.nlpmanagement.KubernetesNodeInfo;
import com.nordstrom.nfe.nlpmanagement.NlpInstanceEntry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import com.xjeffrose.xio.http.PersistentProxyHandler;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.RouteConfig;
import com.xjeffrose.xio.http.RouteState;
import io.netty.handler.codec.http.HttpMethod;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

public class RouteStatesTest {
  private RouteStates subject;
  private Config config;
  private NfeState nfeState;

  @Captor private ArgumentCaptor<ImmutableMap<String, RouteState>> routeMapCaptor;

  @Before
  public void beforeEach() {
    routeMapCaptor = ArgumentCaptor.forClass(ImmutableMap.class);

    config = ConfigFactory.load("application.conf");
    NfeConfig nfeConfig = new NfeConfig(config);
    nfeState = spy(new NfeState(nfeConfig));
    ProxyClientFactory proxyClientFactory = new ProxyClientFactory(nfeState);

    subject = new RouteStates(nfeState, nfeConfig, proxyClientFactory);
  }

  @Test
  public void testProxyHandler() {
    subject.buildInitialRoutes(Collections.emptyList());
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    RouteState firstRouteState = routeMapCaptor.getValue().entrySet().asList().get(0).getValue();
    assertTrue(firstRouteState.handler() instanceof PersistentProxyHandler);
  }

  @Test
  public void testGrpcRoutes() {
    TestGrpcService grpcService = new TestGrpcService();
    subject.buildInitialRoutes(Collections.singletonList(grpcService));
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    RouteState routeState = routeMapCaptor.getValue().get(grpcService.route.buildPath());
    RouteConfig routeConfig = routeState.config();

    assertEquals(Collections.singletonList(HttpMethod.POST), routeConfig.methods());
    assertEquals("", routeConfig.host());
    assertEquals("none", routeConfig.permissionNeeded());
    assertEquals(grpcService.route.buildPath(), routeConfig.path());
    assertEquals(grpcService.route.handler, routeState.handler());
  }

  @Test
  public void testAddCustomerAccountNlpInstances_Simple() {
    String path = "/ou/sn/";
    String accountId = "account_id";
    String ipAddress = "ip_address";

    // WHEN a customer account NLP is added
    NlpInstanceEntry entry =
        new NlpInstanceEntry(accountId, ipAddress, Collections.singletonList(path));
    subject.addCustomerAccountNlpInstances(() -> Collections.singletonList(entry));

    // THEN it should have added the route to NfeState
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());
    RouteState routeState = routeMapCaptor.getValue().get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();

    // THEN it should have configured the route info correctly
    assertEquals(path, routeConfig.path());
    assertEquals("/sn/", routeConfig.proxyPath());

    ClientConfig clientConfig = routeConfig.clientConfigs().get(0);
    ClientConfig defaultClientConfig = ClientConfig.from(config.getConfig("nfe.nlpClient"));
    InetSocketAddress expectedAddress =
        new InetSocketAddress(ipAddress, defaultClientConfig.remote().getPort());
    assertEquals(expectedAddress, clientConfig.remote());
  }

  @Test
  public void testAddCustomerAccountNlpInstances_Combinatorial() {
    List<String> paths1 = Arrays.asList("/ou-1/sn-1-a/", "/ou-1/sn-1-b/");
    List<String> paths2 = Collections.singletonList("/ou-2/sn-2/");

    // WHEN multiple customer account NLPs are added (some of them in the same account)
    List<NlpInstanceEntry> entries =
        Arrays.asList(
            new NlpInstanceEntry("accountId-1", "ipAddress-1", paths1),
            new NlpInstanceEntry("accountId-1", "ipAddress-2", paths1),
            new NlpInstanceEntry("accountId-2", "ipAddress-3", paths2));
    subject.addCustomerAccountNlpInstances(() -> entries);

    // THEN it should have added the routes to NfeState
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    // THEN it should have the added the customer account NLPs instances to the list
    AccountInfo accountInfo1 = subject.getCustomerAccountNlpMap().get("accountId-1");
    assertNotNull(accountInfo1);
    assertEquals(Arrays.asList("ipAddress-1", "ipAddress-2"), accountInfo1.getIpAddresses());
    assertEquals(paths1, accountInfo1.getPaths());

    AccountInfo accountInfo2 = subject.getCustomerAccountNlpMap().get("accountId-2");
    assertNotNull(accountInfo2);
    assertEquals(Collections.singletonList("ipAddress-3"), accountInfo2.getIpAddresses());
    assertEquals(paths2, accountInfo2.getPaths());

    ClientConfig defaultClientConfig = ClientConfig.from(config.getConfig("nfe.nlpClient"));
    int defaultPort = defaultClientConfig.remote().getPort();

    // - '/ou-1/sn-1-a/'
    // THEN it should have route state for path '/ou-1/sn-1-a/'
    RouteState routeState1 = routeMapCaptor.getValue().get(paths1.get(0));
    ProxyRouteConfig routeConfig1 = (ProxyRouteConfig) routeState1.config();
    assertEquals(paths1.get(0), routeConfig1.path());
    assertEquals("/sn-1-a/", routeConfig1.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-1' for path '/ou-1/sn-1-a/'
    ClientConfig clientConfig1_1 = routeConfig1.clientConfigs().get(0);
    InetSocketAddress expectedAddress1_1 = new InetSocketAddress("ipAddress-1", defaultPort);
    assertEquals(expectedAddress1_1, clientConfig1_1.remote());

    // THEN it should have client for NLP at IP address 'ipAddress-2' for path '/ou-1/sn-1-a/'
    ClientConfig clientConfig1_2 = routeConfig1.clientConfigs().get(1);
    InetSocketAddress expectedAddress1_2 = new InetSocketAddress("ipAddress-2", defaultPort);
    assertEquals(expectedAddress1_2, clientConfig1_2.remote());

    // - '/ou-1/sn-1-b/'
    // THEN it should have route state for path '/ou-1/sn-1-b/'
    RouteState routeState2 = routeMapCaptor.getValue().get(paths1.get(1));
    ProxyRouteConfig routeConfig2 = (ProxyRouteConfig) routeState2.config();
    assertEquals(paths1.get(1), routeConfig2.path());
    assertEquals("/sn-1-b/", routeConfig2.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-1' for path '/ou-1/sn-1-b/'
    ClientConfig clientConfig2_1 = routeConfig2.clientConfigs().get(0);
    InetSocketAddress expectedAddress2_1 = new InetSocketAddress("ipAddress-1", defaultPort);
    assertEquals(expectedAddress2_1, clientConfig2_1.remote());

    // THEN it should have client for NLP at IP address 'ipAddress-2' for path '/ou-1/sn-1-b/'
    ClientConfig clientConfig2_2 = routeConfig2.clientConfigs().get(1);
    InetSocketAddress expectedAddress2_2 = new InetSocketAddress("ipAddress-2", defaultPort);
    assertEquals(expectedAddress2_2, clientConfig2_2.remote());

    // - '/ou-1/sn-2/'
    // THEN it should have route state for path '/ou-1/sn-2/'
    RouteState routeState3 = routeMapCaptor.getValue().get(paths2.get(0));
    ProxyRouteConfig routeConfig3 = (ProxyRouteConfig) routeState3.config();
    assertEquals(paths2.get(0), routeConfig3.path());
    assertEquals("/sn-2/", routeConfig3.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-3' for path '/ou-1/sn-2/'
    ClientConfig clientConfig3 = routeConfig3.clientConfigs().get(0);
    InetSocketAddress expectedAddress3 = new InetSocketAddress("ipAddress-3", defaultPort);
    assertEquals(expectedAddress3, clientConfig3.remote());
  }

  @Test
  public void testRemoveCustomerAccountNlpInstance_LastInstanceForAccount() {
    // GIVEN a customer account NLP has been added
    String path = "/ou/sn/";
    NlpInstanceEntry entry =
        new NlpInstanceEntry("accountId", "ipAddress", Collections.singletonList(path));
    subject.addCustomerAccountNlpInstances(() -> Collections.singletonList(entry));
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    // THEN it should have the newly added route (sanity check)
    RouteState routeState = routeMapCaptor.getValue().get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();
    assertNotNull(routeConfig);

    // WHEN the last service for the account is removed
    subject.removeCustomerAccountNlpInstance("accountId", "ipAddress");

    // add NLP + removce NLP = 2 `setRoutes()` calls
    verify(nfeState, timeout(500).times(2)).setRoutes(routeMapCaptor.capture());

    // THEN it should have removed the entire account
    assertFalse(subject.getCustomerAccountNlpMap().containsKey("accountId"));

    // THEN it should have removed the entire proxy route
    assertNull(routeMapCaptor.getValue().get(path));
  }

  @Test
  public void testRemoveCustomerAccountNlpInstance_StillHasInstancesForAccount() {
    // GIVEN 2 customer account NLPs have been added
    String path = "/ou/sn/";
    NlpInstanceEntry entry1 =
        new NlpInstanceEntry("accountId", "ipAddress-1", Collections.singletonList(path));
    NlpInstanceEntry entry2 =
        new NlpInstanceEntry("accountId", "ipAddress-2", Collections.singletonList(path));
    List<NlpInstanceEntry> entries = Arrays.asList(entry1, entry2);
    subject.addCustomerAccountNlpInstances(() -> entries);

    // WHEN one of the services for the account is removed
    subject.removeCustomerAccountNlpInstance("accountId", "ipAddress-1");

    // add NLP + remove NLP = 2 `setRoutes()` calls
    verify(nfeState, timeout(500).times(2)).setRoutes(routeMapCaptor.capture());

    // THEN it should have only removed the NLP instance (not the entire account)
    assertTrue(subject.getCustomerAccountNlpMap().containsKey("accountId"));

    AccountInfo accountInfo = subject.getCustomerAccountNlpMap().get("accountId");
    assertEquals(Collections.singletonList("ipAddress-2"), accountInfo.getIpAddresses());

    // THEN it should have removed the client config, but kept the route
    assertNotNull(routeMapCaptor.getValue().get(path));
    RouteState routeState = routeMapCaptor.getValue().get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();
    assertEquals(1, routeConfig.clientConfigs().size());
    assertEquals("ipAddress-2", routeConfig.clientConfigs().get(0).remote().getHostName());
  }

  @Test
  public void testRemoveCustomerAccountNlpInstance_NonExistingAccountOrIpAddress() {
    subject.buildInitialRoutes(Collections.emptyList());

    // WHEN removing an account that does not exist
    NlpInstanceEntry entry =
        new NlpInstanceEntry("accountId", "ipAddress", Collections.singletonList("/ou/sn/"));
    subject.addCustomerAccountNlpInstances(() -> Collections.singletonList(entry));

    // THEN it should not cause a crash
    subject.removeCustomerAccountNlpInstance("nonExistingAccount", "ipAddress");
    subject.removeCustomerAccountNlpInstance("accountId", "nonExistingIpAddress");
  }

  @Test
  public void testUpdateCustomerAccountNlpInstanceMap() {
    String accountId = "account_id";
    List<String> ipAddresses = Arrays.asList("ip_address-1", "ip_address-2");
    String path = "/ou/sn/";

    Map<String, AccountInfo> newNlpInstanceMap = new HashMap<>();
    newNlpInstanceMap.put(accountId, new AccountInfo(ipAddresses, Collections.singletonList(path)));

    // WHEN the customer account NLP map is updated
    subject.updateCustomerAccountNlpInstanceMap(originalNlpInstanceMap -> newNlpInstanceMap);
    verify(nfeState, timeout(500).times(1)).setRoutes(routeMapCaptor.capture());

    // THEN it should have given NfeState a RouteState map for the newly given NLP map
    // - note: full testing of the RouteState can be found in other tests in this file
    ImmutableMap<String, RouteState> routeStateMap = routeMapCaptor.getValue();
    assertEquals(1, routeStateMap.size());
    assertNotNull(routeStateMap.get(path));
  }

  @Test
  public void testAddKubernetesNlpInstanceMap_Simple() {
    String path = "/ou/sn/";
    String ipAddress = "ip_address";

    // WHEN a K8S node is added
    KubernetesNodeInfo nodeInfo =
        new KubernetesNodeInfo("unique-id", ipAddress, Collections.singletonList(path));
    subject.addKubernetesNlpInstances(() -> Collections.singletonList(nodeInfo));
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    // THEN it should now have the newly added routes
    RouteState routeState = routeMapCaptor.getValue().get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();

    // THEN it should have configured the route info correctly
    assertEquals(path, routeConfig.path());
    assertEquals("/sn/", routeConfig.proxyPath());

    ClientConfig clientConfig = routeConfig.clientConfigs().get(0);
    KubernetesRoutingConfig kubernetesRoutingConfig =
        KubernetesRoutingConfig.fromConfig(config.getConfig("nfe.kubernetesRouting"));
    InetSocketAddress expectedAddress =
        new InetSocketAddress(ipAddress, kubernetesRoutingConfig.getReservedNlpPort());
    assertEquals(expectedAddress, clientConfig.remote());
  }

  @Test
  public void testAddKubernetesNlpInstanceMap_Complex() {
    List<String> paths1 = Arrays.asList("/ou-1/sn-1-a/", "/ou-1/sn-1-b/");
    List<String> paths2 = Collections.singletonList("/ou-2/sn-2/");

    // WHEN multiple K8S nodes are added (some with the same paths)
    List<KubernetesNodeInfo> nodeInfos =
        Arrays.asList(
            new KubernetesNodeInfo("uniqueId-1", "ipAddress-1", paths1),
            new KubernetesNodeInfo("uniqueId-2", "ipAddress-2", paths1),
            new KubernetesNodeInfo("uniqueId-3", "ipAddress-3", paths2));
    subject.addKubernetesNlpInstances(() -> nodeInfos);
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    // THEN it should have the K8S-NLP instances
    KubernetesNodeInfo nodeInfo1 = subject.getK8sNlpMap().get("uniqueId-1");
    assertNotNull(nodeInfo1);
    assertEquals("ipAddress-1", nodeInfo1.getIpAddress());
    assertEquals(paths1, nodeInfo1.getPaths());

    KubernetesNodeInfo nodeInfo2 = subject.getK8sNlpMap().get("uniqueId-2");
    assertNotNull(nodeInfo2);
    assertEquals("ipAddress-2", nodeInfo2.getIpAddress());
    assertEquals(paths1, nodeInfo2.getPaths());

    KubernetesNodeInfo nodeInfo3 = subject.getK8sNlpMap().get("uniqueId-3");
    assertNotNull(nodeInfo3);
    assertEquals("ipAddress-3", nodeInfo3.getIpAddress());
    assertEquals(paths2, nodeInfo3.getPaths());

    KubernetesRoutingConfig kubernetesRoutingConfig =
        KubernetesRoutingConfig.fromConfig(config.getConfig("nfe.kubernetesRouting"));
    int reservedNlpPort = kubernetesRoutingConfig.getReservedNlpPort();

    // - '/ou-1/sn-1-a/'
    // THEN it should have route state for path '/ou-1/sn-1-a/'
    RouteState routeState1 = routeMapCaptor.getValue().get(paths1.get(0));
    ProxyRouteConfig routeConfig1 = (ProxyRouteConfig) routeState1.config();
    assertEquals(paths1.get(0), routeConfig1.path());
    assertEquals("/sn-1-a/", routeConfig1.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-1' for path '/ou-1/sn-1-a/'
    ClientConfig clientConfig1_1 = routeConfig1.clientConfigs().get(0);
    InetSocketAddress expectedAddress1_1 = new InetSocketAddress("ipAddress-1", reservedNlpPort);
    assertEquals(expectedAddress1_1, clientConfig1_1.remote());

    // THEN it should have client for NLP at IP address 'ipAddress-2' for path '/ou-1/sn-1-a/'
    ClientConfig clientConfig1_2 = routeConfig1.clientConfigs().get(1);
    InetSocketAddress expectedAddress1_2 = new InetSocketAddress("ipAddress-2", reservedNlpPort);
    assertEquals(expectedAddress1_2, clientConfig1_2.remote());

    // - '/ou-1/sn-1-b/'
    // THEN it should have route state for path '/ou-1/sn-1-b/'
    RouteState routeState2 = routeMapCaptor.getValue().get(paths1.get(1));
    ProxyRouteConfig routeConfig2 = (ProxyRouteConfig) routeState2.config();
    assertEquals(paths1.get(1), routeConfig2.path());
    assertEquals("/sn-1-b/", routeConfig2.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-1' for path '/ou-1/sn-1-b/'
    ClientConfig clientConfig2_1 = routeConfig2.clientConfigs().get(0);
    InetSocketAddress expectedAddress2_1 = new InetSocketAddress("ipAddress-1", reservedNlpPort);
    assertEquals(expectedAddress2_1, clientConfig2_1.remote());

    // THEN it should have client for NLP at IP address 'ipAddress-2' for path '/ou-1/sn-1-b/'
    ClientConfig clientConfig2_2 = routeConfig2.clientConfigs().get(1);
    InetSocketAddress expectedAddress2_2 = new InetSocketAddress("ipAddress-2", reservedNlpPort);
    assertEquals(expectedAddress2_2, clientConfig2_2.remote());

    // - '/ou-1/sn-2/'
    // THEN it should have route state for path '/ou-1/sn-2/'
    RouteState routeState3 = routeMapCaptor.getValue().get(paths2.get(0));
    ProxyRouteConfig routeConfig3 = (ProxyRouteConfig) routeState3.config();
    assertEquals(paths2.get(0), routeConfig3.path());
    assertEquals("/sn-2/", routeConfig3.proxyPath());

    // THEN it should have client for NLP at IP address 'ipAddress-3' for path '/ou-1/sn-2/'
    ClientConfig clientConfig3 = routeConfig3.clientConfigs().get(0);
    InetSocketAddress expectedAddress3 = new InetSocketAddress("ipAddress-3", reservedNlpPort);
    assertEquals(expectedAddress3, clientConfig3.remote());
  }

  @Test
  public void testAddKubernetesNlpInstanceMap_OverridingExistingNode() {
    // GIVEN a K8S node has already been added
    String uniqueId = "unique-id";
    KubernetesNodeInfo prevNodeInfo =
        new KubernetesNodeInfo(
            uniqueId, "ip-address-previous", Collections.singletonList("/ou/sn-prev/"));
    subject.addKubernetesNlpInstances(() -> Collections.singletonList(prevNodeInfo));

    String path = "/ou/sn-updated/";
    String ipAddress = "ip_address-updated";

    // GIVEN a node is added that has the same unique Id as an existing node
    KubernetesNodeInfo nodeInfo =
        new KubernetesNodeInfo(uniqueId, ipAddress, Collections.singletonList(path));
    subject.addKubernetesNlpInstances(() -> Collections.singletonList(nodeInfo));
    verify(nfeState, timeout(500).times(2)).setRoutes(routeMapCaptor.capture());

    // THEN it should now have the updated routes
    RouteState routeState = routeMapCaptor.getAllValues().get(1).get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();

    // THEN it should have configured the route info with the new info
    assertEquals(path, routeConfig.path());
    assertEquals("/sn-updated/", routeConfig.proxyPath());

    ClientConfig clientConfig = routeConfig.clientConfigs().get(0);
    KubernetesRoutingConfig kubernetesRoutingConfig =
        KubernetesRoutingConfig.fromConfig(config.getConfig("nfe.kubernetesRouting"));
    InetSocketAddress expectedAddress =
        new InetSocketAddress(ipAddress, kubernetesRoutingConfig.getReservedNlpPort());
    assertEquals(expectedAddress, clientConfig.remote());
  }

  @Test
  public void testRemoveKubernetesNlpInstance_ExistingNode() {
    // GIVEN a K8S node has been added
    String path = "/ou/sn/";
    KubernetesNodeInfo entry =
        new KubernetesNodeInfo("node-id", "ipAddress", Collections.singletonList(path));
    subject.addKubernetesNlpInstances(() -> Collections.singletonList(entry));
    verify(nfeState, timeout(500)).setRoutes(routeMapCaptor.capture());

    // THEN it should have the newly added route (sanity check)
    RouteState routeState = routeMapCaptor.getValue().get(path);
    ProxyRouteConfig routeConfig = (ProxyRouteConfig) routeState.config();
    assertNotNull(routeConfig);
    assertTrue(subject.getK8sNlpMap().containsKey("node-id"));

    // WHEN an existing K8S node is removed
    subject.removeKubernetesNlpInstance("node-id");

    // add NLP + remove NLP = 2 `setRoutes()` calls
    verify(nfeState, timeout(500).times(2)).setRoutes(routeMapCaptor.capture());

    // THEN it should have removed the entire account
    assertFalse(subject.getK8sNlpMap().containsKey("node-id"));

    // THEN it should have removed the entire proxy route
    assertNull(routeMapCaptor.getValue().get(path));
  }

  @Test
  public void testRemoveKubernetesNlpInstance_NonExistingNode() {
    // WHEN a non-existing K8S node is removed
    subject.removeKubernetesNlpInstance("node-id");
    waitForRouteStatesToFinishProcessingQueue();

    // THEN it should not have updated NfeState
    assertTrue(routeMapCaptor.getAllValues().isEmpty());
  }

  private static class TestGrpcService implements GrpcService {
    GrpcRoute route;

    public TestGrpcService() {
      this.route =
          new GrpcRoute(
              this,
              "method_name",
              new GrpcRequestHandler<>(
                  CreateServiceRegistrationRequest::parseFrom,
                  (request) -> CreateServiceRegistrationResponse.newBuilder().build()));
    }

    public String getPackageName() {
      return "package_name";
    }

    public String getServiceName() {
      return "service_name";
    }

    public List<GrpcRoute> getRoutes() {
      return Collections.singletonList(route);
    }
  }

  private void waitForRouteStatesToFinishProcessingQueue() {
    AtomicBoolean finished = new AtomicBoolean(false);
    subject.addToQueue(() -> finished.set(true));
    while (finished.get()) {
      /* keep waiting */
    }
  }
}
