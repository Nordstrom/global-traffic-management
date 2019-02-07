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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

public class CustomerAccountNlpDeploymentWatcherTest extends Assert {
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

    CustomerAccountNlpDeploymentWatcher subject =
        new CustomerAccountNlpDeploymentWatcher(
            routeStates, zkClient, objectMapper, coreDataService);
    subject.start();
  }

  @After
  public void afterEach() throws Exception {
    zkClient.stop();
    zkCuratorFramework.close();
    zkServer.close();
  }

  @Test
  public void testStartingWithoutInitialValues() {
    // GIVEN no NLP instances are in zookeeper at start up time

    // WHEN NFE starts up
    finishSetup();

    // THEN it should not add any NLP instances
    verify(routeStates, never()).addCustomerAccountNlpInstances(any());
  }

  @Test
  @Ignore("This only passes locally")
  public void testStartingWithInitialValues() {
    String accountId1 = "account_id-1";
    String accountId2 = "account_id-2";
    String ipAddress1 = "ip-1";
    String ipAddress2 = "ip-2";
    String ipAddress3 = "ip-3";
    Map<String, List<String>> expectedAccountRoutePaths = Maps.newHashMap();
    expectedAccountRoutePaths.put(accountId1, Arrays.asList("/ou/sn-1/", "/ou/sn-2/"));
    expectedAccountRoutePaths.put(accountId2, Collections.singletonList("/ou/sn-3/"));

    // GIVEN NLP instances are in zookeeper at start up time
    List<CustomerAccountNlpZookeeperInfo> initialNlpInfos =
        Lists.newArrayList(
            new CustomerAccountNlpZookeeperInfo(accountId1, "instance_id-1", ipAddress1),
            new CustomerAccountNlpZookeeperInfo(accountId1, "instance_id-2", ipAddress2),
            new CustomerAccountNlpZookeeperInfo(accountId2, "instance_id-3", ipAddress3));

    initialNlpInfos.forEach(this::writeInfoToZooKeeper);

    List<String> accountIds = Arrays.asList(accountId1, accountId2);
    when(coreDataService.getCustomerAccountNlpRoutePaths(accountIds))
        .thenReturn(expectedAccountRoutePaths);

    // WHEN NFE starts up
    finishSetup();

    // THEN it should add all the initial NLP instances
    ArgumentCaptor<Supplier<List<NlpInstanceEntry>>> captor =
        ArgumentCaptor.forClass(Supplier.class);
    verify(routeStates, timeout(1000)).addCustomerAccountNlpInstances(captor.capture());
    List<NlpInstanceEntry> actualEntries = captor.getValue().get();

    List<NlpInstanceEntry> expectedEntries =
        Lists.newArrayList(
            new NlpInstanceEntry(accountId1, ipAddress1, expectedAccountRoutePaths.get(accountId1)),
            new NlpInstanceEntry(accountId1, ipAddress2, expectedAccountRoutePaths.get(accountId1)),
            new NlpInstanceEntry(
                accountId2, ipAddress3, expectedAccountRoutePaths.get(accountId2)));

    assertEquals(Sets.newHashSet(expectedEntries), Sets.newHashSet(actualEntries));
  }

  @Test
  public void testNodeAdded() {
    String accountId = "account_id";
    String ipAddress = "ip_address";
    List<String> expectedPaths = Collections.singletonList("/ou/sn/");
    Map<String, List<String>> pathsMap = Collections.singletonMap(accountId, expectedPaths);

    when(coreDataService.getCustomerAccountNlpRoutePaths(Collections.singletonList(accountId)))
        .thenReturn(pathsMap);

    // GIVEN NFE has already started up
    finishSetup();

    // WHEN a new NLP instance has been deployed
    CustomerAccountNlpZookeeperInfo info =
        new CustomerAccountNlpZookeeperInfo(accountId, "instance_id", ipAddress);
    writeInfoToZooKeeper(info);

    // THEN it should have registered the NLP
    ArgumentCaptor<Supplier<List<NlpInstanceEntry>>> captor =
        ArgumentCaptor.forClass(Supplier.class);
    // initialized + node added = 2 `addNlpInstance()` calls
    verify(routeStates, timeout(1000).times(2)).addCustomerAccountNlpInstances(captor.capture());

    List<NlpInstanceEntry> actualEntries = captor.getValue().get();
    NlpInstanceEntry expectedEntry = new NlpInstanceEntry(accountId, ipAddress, expectedPaths);
    assertEquals(Collections.singletonList(expectedEntry), actualEntries);
  }

  @Test
  @Ignore("This only passes locally")
  public void testNodeRemoved() throws Exception {
    String accountId = "account_id";
    String ipAddress = "ip_address";

    // GIVEN NFE has already started up and has at least one NLP instance
    finishSetup();

    CustomerAccountNlpZookeeperInfo info =
        new CustomerAccountNlpZookeeperInfo(accountId, "instance_id", ipAddress);
    writeInfoToZooKeeper(info);

    // WHEN an NLP has been removed
    String zkPath =
        CustomerAccountNlpDeploymentGrpcService.CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH
            + "/"
            + info.getAccountId()
            + "/"
            + info.getInstanceId();
    zkCuratorFramework.delete().deletingChildrenIfNeeded().forPath(zkPath);

    // THEN it should have removed the NLP instance
    verify(routeStates, timeout(1000)).removeCustomerAccountNlpInstance(accountId, ipAddress);
  }

  private void writeInfoToZooKeeper(CustomerAccountNlpZookeeperInfo info) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(info);
      String path =
          String.join(
              "/",
              Arrays.asList(
                  CustomerAccountNlpDeploymentGrpcService.CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH,
                  info.getAccountId(),
                  info.getInstanceId()));

      zkCuratorFramework.create().orSetData().creatingParentsIfNeeded().forPath(path, payload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
