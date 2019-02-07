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
package com.nordstrom.nfe.nlpmanagement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.RouteStates;
import com.xjeffrose.xio.core.ZkClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class KubernetesNlpDeploymentWatcherTest extends Assert {
  private RouteStates routeStates;
  private ZkClient zkClient;
  private TestingServer zkServer;
  private CoreDataService coreDataService;
  private CuratorFramework zkCuratorFramework;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void beforeEach() throws Exception {
    // start the zookeeper zkServer
    zkServer = new TestingServer();
    zkServer.start();

    // start the curator client (which talks to the zookeeper zkServer)
    RetryPolicy retryPolicy = new RetryOneTime(1);
    zkCuratorFramework =
        CuratorFrameworkFactory.newClient(zkServer.getConnectString(), retryPolicy);
    zkCuratorFramework.start();

    coreDataService = mock(CoreDataService.class);
  }

  private void finishSetup() {
    routeStates = mock(RouteStates.class);
    zkClient = new ZkClient(zkServer.getConnectString());
    zkClient.start();

    KubernetesNlpDeploymentWatcher subject =
        new KubernetesNlpDeploymentWatcher(routeStates, zkClient, objectMapper, coreDataService);
    subject.start();
  }

  @After
  public void afterEach() throws Exception {
    zkClient.stop();
    zkCuratorFramework.close();
    zkServer.close();
  }

  @Test
  @Ignore("This only passes locally")
  public void testStartingWithInitialValues() throws Exception {
    String region = "region";
    String clusterId1 = "cluster_id-1";
    String clusterId2 = "cluster_id-2";
    String nodeId1 = "node_id-1";
    String nodeId2 = "node_id-2";
    String serviceName1 = "sn-1";
    String serviceName2 = "sn-2";
    String servicePath1 = "/ou/sn-1/";
    String servicePath2 = "/ou/sn-2/";
    String ipAddress1 = "ip-1";
    String ipAddress2 = "ip-2";

    KubernetesServiceZookeeperInfo serviceZkInfo_1_1 =
        new KubernetesServiceZookeeperInfo(serviceName1, 1);
    KubernetesServiceZookeeperInfo serviceZkInfo_1_2 =
        new KubernetesServiceZookeeperInfo(serviceName2, 1);
    KubernetesServiceZookeeperInfo serviceZkInfo_2_1 =
        new KubernetesServiceZookeeperInfo(serviceName1, 1);

    // GIVEN K8S nodes are in zookeeper at start up time
    List<KubernetesNodeZookeeperInfo> initialK8sNodeZkInfos =
        Lists.newArrayList(
            new KubernetesNodeZookeeperInfo(
                region,
                clusterId1,
                nodeId1,
                ipAddress1,
                Arrays.asList(serviceZkInfo_1_1, serviceZkInfo_1_2)),
            new KubernetesNodeZookeeperInfo(
                region,
                clusterId2,
                nodeId2,
                ipAddress2,
                Collections.singletonList(serviceZkInfo_2_1)));

    initialK8sNodeZkInfos.forEach(this::writeInfoToZooKeeper);

    List<String> serviceNames = Arrays.asList(serviceName1, serviceName2);
    Map<String, String> pathsMap = Maps.newHashMap();
    pathsMap.put(serviceName1, servicePath1);
    pathsMap.put(serviceName2, servicePath2);
    when(coreDataService.getPathsForServices(serviceNames)).thenReturn(pathsMap);

    // WHEN NFE starts up
    finishSetup();

    // THEN it should add all the initial NLP instances
    ArgumentCaptor<Supplier<List<KubernetesNodeInfo>>> captor =
        ArgumentCaptor.forClass(Supplier.class);
    verify(routeStates, timeout(1000)).addKubernetesNlpInstances(captor.capture());
    List<KubernetesNodeInfo> actualNodeInfos = captor.getValue().get();

    assertEquals(2, actualNodeInfos.size());

    String uniqueId1 = String.join("-", Arrays.asList(region, clusterId1, nodeId1));
    String uniqueId2 = String.join("-", Arrays.asList(region, clusterId2, nodeId2));
    List<KubernetesNodeInfo> expectedNodeInfos =
        Lists.newArrayList(
            new KubernetesNodeInfo(
                uniqueId1, ipAddress1, Arrays.asList(servicePath1, servicePath2)),
            new KubernetesNodeInfo(uniqueId2, ipAddress2, Collections.singletonList(servicePath1)));

    assertEquals(uniqueId2, actualNodeInfos.get(0).getUniqueId());
    assertEquals(ipAddress2, actualNodeInfos.get(0).getIpAddress());
    assertEquals(Collections.singletonList(servicePath1), actualNodeInfos.get(0).getPaths());

    assertEquals(uniqueId1, actualNodeInfos.get(1).getUniqueId());
    assertEquals(ipAddress1, actualNodeInfos.get(1).getIpAddress());
    assertEquals(Arrays.asList(servicePath1, servicePath2), actualNodeInfos.get(1).getPaths());
  }

  @Test
  public void testNodeAdded() throws Exception {
    String servicdeName = "service-name";
    String ipAddress = "ip_address";
    String expectedPath = "/ou/sn/";
    String region = "region";
    String clusterId = "cluster_id";
    String nodeId = "node_id";
    Map<String, String> pathsMap = Collections.singletonMap(servicdeName, expectedPath);

    when(coreDataService.getPathsForServices(Collections.singletonList(servicdeName)))
        .thenReturn(pathsMap);

    // GIVEN NFE has already started up
    finishSetup();

    // WHEN a new K8S node has been deployed
    List<KubernetesServiceZookeeperInfo> kubernetesServiceZookeeperInfos =
        Collections.singletonList(new KubernetesServiceZookeeperInfo(servicdeName, 1));
    KubernetesNodeZookeeperInfo info =
        new KubernetesNodeZookeeperInfo(
            region, clusterId, nodeId, ipAddress, kubernetesServiceZookeeperInfos);
    writeInfoToZooKeeper(info);

    // THEN it should have registered the NLP
    ArgumentCaptor<Supplier<List<KubernetesNodeInfo>>> captor =
        ArgumentCaptor.forClass(Supplier.class);
    // initialized + node added = 2 `addKubernetesNlpInstances()` calls
    verify(routeStates, timeout(1000).times(2)).addKubernetesNlpInstances(captor.capture());

    List<KubernetesNodeInfo> actualNodeInfos = captor.getValue().get();
    assertEquals(1, actualNodeInfos.size());

    String expectedUniqueId = String.join("-", Arrays.asList(region, clusterId, nodeId));

    assertEquals(expectedUniqueId, actualNodeInfos.get(0).getUniqueId());
    assertEquals(ipAddress, actualNodeInfos.get(0).getIpAddress());
    assertEquals(Collections.singletonList(expectedPath), actualNodeInfos.get(0).getPaths());
  }

  @Test
  public void testNodeUpdated() throws Exception {
    String region = "region";
    String clusterId = "cluster_id";
    String nodeId = "node_id";

    // GIVEN NFE has already started up
    finishSetup();

    // GIVEN info for the node as already been added
    List<KubernetesServiceZookeeperInfo> prevKubernetesServiceZookeeperInfos =
        Collections.singletonList(new KubernetesServiceZookeeperInfo("service-name-prev", 1));
    KubernetesNodeZookeeperInfo prevInfo =
        new KubernetesNodeZookeeperInfo(
            region, clusterId, nodeId, "ip-address-prev", prevKubernetesServiceZookeeperInfos);
    writeInfoToZooKeeper(prevInfo);

    // (spot check and waits for node added event to finish)
    // initialized + node added = 2 `addKubernetesNlpInstances()` calls
    verify(routeStates, timeout(1000).times(2)).addKubernetesNlpInstances(any());

    // WHEN the info for the node has been updated
    String expectedServiceName = "service-name-updated";
    String expectedIpAddress = "ip_address-updated";
    String expectedPath = "/ou/sn-updated/";
    Map<String, String> pathsMap = Collections.singletonMap(expectedServiceName, expectedPath);

    when(coreDataService.getPathsForServices(Collections.singletonList(expectedServiceName)))
        .thenReturn(pathsMap);

    List<KubernetesServiceZookeeperInfo> expectedKubernetesServiceZookeeperInfos =
        Collections.singletonList(new KubernetesServiceZookeeperInfo(expectedServiceName, 1));
    KubernetesNodeZookeeperInfo newInfo =
        new KubernetesNodeZookeeperInfo(
            region, clusterId, nodeId, expectedIpAddress, expectedKubernetesServiceZookeeperInfos);
    writeInfoToZooKeeper(newInfo);

    // THEN it should have registered the NLP
    ArgumentCaptor<Supplier<List<KubernetesNodeInfo>>> captor =
        ArgumentCaptor.forClass(Supplier.class);

    // initialized + node added + node updated = 3 `addKubernetesNlpInstances()` calls
    verify(routeStates, timeout(1000).times(3)).addKubernetesNlpInstances(captor.capture());

    List<KubernetesNodeInfo> actualNodeInfos = captor.getValue().get();
    assertEquals(1, actualNodeInfos.size());

    String expectedUniqueId = String.join("-", Arrays.asList(region, clusterId, nodeId));

    assertEquals(expectedUniqueId, actualNodeInfos.get(0).getUniqueId());
    assertEquals(expectedIpAddress, actualNodeInfos.get(0).getIpAddress());
    assertEquals(Collections.singletonList(expectedPath), actualNodeInfos.get(0).getPaths());
  }

  @Test
  public void testNodeRemoved() throws Exception {
    String region = "region";
    String clusterId = "cluster_id";
    String nodeId = "node_id";

    // GIVEN NFE has already started up
    finishSetup();

    // GIVEN previous info for the node already exists in zookeeper
    List<KubernetesServiceZookeeperInfo> prevKubernetesServiceZookeeperInfos =
        Collections.singletonList(new KubernetesServiceZookeeperInfo("service-name", 1));
    KubernetesNodeZookeeperInfo prevInfo =
        new KubernetesNodeZookeeperInfo(
            region, clusterId, nodeId, "ip-address", prevKubernetesServiceZookeeperInfos);
    writeInfoToZooKeeper(prevInfo);

    // GIVEN NFE has already started up
    // initialized + node added = 2 `addKubernetesNlpInstances()` calls
    verify(routeStates, timeout(1000).times(2)).addKubernetesNlpInstances(any());

    // WHEN an NLP has been removed
    String zkPath =
        KubernetesNlpDeploymentGrpcService.KUBERNETES_NLP_AWS_BASE_PATH
            + "/"
            + region
            + "/"
            + clusterId
            + "/"
            + nodeId;
    zkCuratorFramework.delete().deletingChildrenIfNeeded().forPath(zkPath);

    // THEN it should have removed the NLP instance
    String expectedUniqueId = String.join("-", Arrays.asList(region, clusterId, nodeId));
    verify(routeStates, timeout(2000)).removeKubernetesNlpInstance(expectedUniqueId);
  }

  private void writeInfoToZooKeeper(KubernetesNodeZookeeperInfo info) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(info);
      String path =
          String.join(
              "/",
              Arrays.asList(
                  KubernetesNlpDeploymentGrpcService.KUBERNETES_NLP_AWS_BASE_PATH,
                  info.getRegion(),
                  info.getClusterId(),
                  info.getNodeId()));

      zkCuratorFramework.create().orSetData().creatingParentsIfNeeded().forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
