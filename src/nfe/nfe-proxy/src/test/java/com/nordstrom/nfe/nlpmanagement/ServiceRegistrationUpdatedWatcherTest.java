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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.RouteStates;
import com.nordstrom.nfe.config.NlpSharedCountConfig;
import com.nordstrom.nfe.testhelpers.TimingTestHelper;
import com.xjeffrose.xio.core.ZkClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.framework.recipes.shared.VersionedValue;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ServiceRegistrationUpdatedWatcherTest extends Assert {
  ServiceRegistrationUpdatedWatcher subject;
  private RouteStates routeStates;
  private ZkClient zkClient;
  private TestingServer zkServer;
  private CuratorFramework zkCuratorFramework;
  private CoreDataService coreDataService;

  @Before
  public void beforeEach() throws Exception {
    // start the zookeeper server
    zkServer = new TestingServer();
    zkServer.start();

    // start the curator client (which talks to the zookeeper server)
    RetryPolicy retryPolicy = new RetryOneTime(1);
    zkCuratorFramework =
        CuratorFrameworkFactory.newClient(zkServer.getConnectString(), retryPolicy);
    zkCuratorFramework.start();

    // start the zkClient (which talks to the zookeeper server / curator client)
    zkClient = new ZkClient(zkServer.getConnectString());
    zkClient.start();

    routeStates = mock(RouteStates.class);
    coreDataService = mock(CoreDataService.class);
    NlpSharedCountConfig nlpSharedCountConfig = new NlpSharedCountConfig(1, 1);

    subject =
        new ServiceRegistrationUpdatedWatcher(
            routeStates, zkClient, coreDataService, nlpSharedCountConfig);
    subject.start();
  }

  @After
  public void afterEach() throws Exception {
    zkClient.stop();
    zkCuratorFramework.close();
    zkServer.close();
  }

  @Test
  public void testPostServiceRegistrationDataUpdated() throws Exception {
    SharedCount sharedCount =
        zkClient.createSharedCounter("/service_registration/updated_counter", 0);
    sharedCount.start();

    // should have the initial value (sanity check)
    assertEquals(0, sharedCount.getCount());

    subject.postServiceRegistrationDataUpdated();

    // should have updated the count to plus one of the original
    boolean success =
        TimingTestHelper.contiuallyCheckForSuccess(1000, () -> sharedCount.getCount() == 1);
    assertTrue(success);
  }

  @Test
  public void testListeningForServiceRegistrationDataUpdated() throws Exception {
    // post new update to zookeeper
    SharedCount sharedCount =
        zkClient.createSharedCounter("/service_registration/updated_counter", 0);
    sharedCount.start();

    VersionedValue<Integer> previousValue = sharedCount.getVersionedValue();
    sharedCount.trySetCount(previousValue, previousValue.getValue() + 1);

    // should tell RouteStates to update all NLP info
    ArgumentCaptor<Function<ImmutableMap<String, AccountInfo>, Map<String, AccountInfo>>> captor =
        ArgumentCaptor.forClass(Function.class);
    verify(routeStates, timeout(1000)).updateCustomerAccountNlpInstanceMap(captor.capture());
    Function<ImmutableMap<String, AccountInfo>, Map<String, AccountInfo>>
        actualNlpInstanceMapUpdater = captor.getValue();

    // checking the update function
    String accountId = "account_id";
    List<String> ipAddresses = Arrays.asList("ip_address-1", "ip_address-2");
    List<String> fetchedRoutePaths = Arrays.asList("/ou/sn-new-1/", "/ou/sn-new-2/");
    List<String> accountIds = Collections.singletonList(accountId);

    Map<String, List<String>> accountRoutePaths = new HashMap<>();
    accountRoutePaths.put(accountId, fetchedRoutePaths);
    when(coreDataService.getCustomerAccountNlpRoutePaths(accountIds)).thenReturn(accountRoutePaths);

    Map<String, AccountInfo> originalNlpInstanceMap = new HashMap<>();
    originalNlpInstanceMap.put(
        accountId, new AccountInfo(ipAddresses, Collections.singletonList("/ou/sn-old/")));

    Map<String, AccountInfo> newNlpInstanceMap =
        actualNlpInstanceMapUpdater.apply(ImmutableMap.copyOf(originalNlpInstanceMap));
    AccountInfo updatedAccountInfo = newNlpInstanceMap.get(accountId);

    // should provide a new NLP instance map with the same IP address, and the fetched route paths
    assertNotNull(updatedAccountInfo);
    assertEquals(ipAddresses, updatedAccountInfo.getIpAddresses());
    assertEquals(fetchedRoutePaths, updatedAccountInfo.getPaths());
  }
}
