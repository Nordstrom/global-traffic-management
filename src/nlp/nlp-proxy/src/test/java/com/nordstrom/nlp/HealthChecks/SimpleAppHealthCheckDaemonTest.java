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

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SimpleAppHealthCheckDaemonTest extends Assert {

  @Mock AppEndpointHealthCheckService endpointHealthCheckService;

  @Mock Consumer<ImmutableMap<String, ImmutableSet<String>>> healthyHostListUpdater;

  @Mock Supplier<ImmutableList<DynamicRouteConfig>> healthCheckCandidateSupplier;

  SimpleAppHealthCheckDaemon subject;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testNoCandidateAccountInfo() throws Exception {
    List<DynamicRouteConfig> inputList = new ArrayList<>();

    when(healthCheckCandidateSupplier.get()).thenReturn(ImmutableList.copyOf(inputList));

    // make the health check service return no healthy hosts when it is asked to check the endpoints
    // for both accounts
    ImmutableSet<String> noHealthy = ImmutableSet.copyOf(new ArrayList<String>());

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    subject =
        new SimpleAppHealthCheckDaemon(
            executor,
            endpointHealthCheckService,
            healthyHostListUpdater,
            healthCheckCandidateSupplier);
    subject.setTestUpdateInterval(1);
    subject.start();

    ArgumentCaptor<ImmutableMap<String, ImmutableSet<String>>> captor =
        ArgumentCaptor.forClass(ImmutableMap.class);
    verify(healthyHostListUpdater, timeout(5000)).accept(captor.capture());

    ImmutableMap<String, ImmutableSet<String>> results = captor.getValue();

    Map<String, ImmutableList> expectedResults = new HashMap<>();

    assertEquals(ImmutableMap.copyOf(expectedResults), results);
  }

  @Test
  public void testNoHealthyHosts() throws Exception {
    List<DynamicRouteConfig> inputList = new ArrayList<>();

    String path1 = "path1";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "/healthCheckPath/";
    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, false, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));
    DynamicRouteConfig dynamicRouteConfig1 = new DynamicRouteConfig(path1, path1ClientConfigs);

    String path2 = "path2";
    String ip2a = "3.4.5.6";
    String ip2b = "4.5.6.7";
    DynamicClientConfig dynamicClientConfig2a =
        new DynamicClientConfig(ip2a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig2b =
        new DynamicClientConfig(ip2b, port, false, healthCheckPath);
    List<DynamicClientConfig> path2ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig2a, dynamicClientConfig2b));
    DynamicRouteConfig dynamicRouteConfig2 = new DynamicRouteConfig(path2, path2ClientConfigs);

    inputList.add(dynamicRouteConfig1);
    inputList.add(dynamicRouteConfig2);

    when(healthCheckCandidateSupplier.get()).thenReturn(ImmutableList.copyOf(inputList));

    // make the health check service return some healthy hosts when it is asked to check the
    // endpoints
    // for both accounts
    ImmutableSet<String> noHealthy = ImmutableSet.copyOf(new ArrayList<String>());
    when(endpointHealthCheckService.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs)))
        .thenReturn(noHealthy);
    when(endpointHealthCheckService.checkEndpoints(path2, ImmutableList.copyOf(path2ClientConfigs)))
        .thenReturn(noHealthy);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    subject =
        new SimpleAppHealthCheckDaemon(
            executor,
            endpointHealthCheckService,
            healthyHostListUpdater,
            healthCheckCandidateSupplier);
    subject.setTestUpdateInterval(1);
    subject.start();

    ArgumentCaptor<ImmutableMap<String, ImmutableSet<String>>> captor =
        ArgumentCaptor.forClass(ImmutableMap.class);
    verify(healthyHostListUpdater, timeout(5000)).accept(captor.capture());

    ImmutableMap<String, ImmutableSet<String>> results = captor.getValue();

    Map<String, ImmutableSet> expectedResults = new HashMap<>();
    expectedResults.put(path1, noHealthy);
    expectedResults.put(path2, noHealthy);

    assertEquals(ImmutableMap.copyOf(expectedResults), results);
  }

  @Test
  public void testSomeHealthyHosts() throws Exception {
    List<DynamicRouteConfig> inputList = new ArrayList<>();

    String path1 = "path1";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "/healthCheckPath/";
    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, false, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));
    DynamicRouteConfig dynamicRouteConfig1 = new DynamicRouteConfig(path1, path1ClientConfigs);

    String path2 = "path2";
    String ip2a = "3.4.5.6";
    String ip2b = "4.5.6.7";
    DynamicClientConfig dynamicClientConfig2a =
        new DynamicClientConfig(ip2a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig2b =
        new DynamicClientConfig(ip2b, port, false, healthCheckPath);
    List<DynamicClientConfig> path2ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig2a, dynamicClientConfig2b));
    DynamicRouteConfig dynamicRouteConfig2 = new DynamicRouteConfig(path2, path2ClientConfigs);

    inputList.add(dynamicRouteConfig1);
    inputList.add(dynamicRouteConfig2);

    when(healthCheckCandidateSupplier.get()).thenReturn(ImmutableList.copyOf(inputList));

    // make the health check service return some healthy hosts when it is asked to check the
    // endpoints
    // for both accounts
    ImmutableSet<String> someHealthy1 = ImmutableSet.copyOf(Arrays.asList(ip1a));
    ImmutableSet<String> someHealthy2 = ImmutableSet.copyOf(Arrays.asList(ip2a));
    when(endpointHealthCheckService.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs)))
        .thenReturn(someHealthy1);
    when(endpointHealthCheckService.checkEndpoints(path2, ImmutableList.copyOf(path2ClientConfigs)))
        .thenReturn(someHealthy2);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    subject =
        new SimpleAppHealthCheckDaemon(
            executor,
            endpointHealthCheckService,
            healthyHostListUpdater,
            healthCheckCandidateSupplier);
    subject.setTestUpdateInterval(1);
    subject.start();

    ArgumentCaptor<ImmutableMap<String, ImmutableSet<String>>> captor =
        ArgumentCaptor.forClass(ImmutableMap.class);
    verify(healthyHostListUpdater, timeout(5000)).accept(captor.capture());

    ImmutableMap<String, ImmutableSet<String>> results = captor.getValue();

    Map<String, ImmutableSet> expectedResults = new HashMap<>();
    expectedResults.put(path1, someHealthy1);
    expectedResults.put(path2, someHealthy2);

    assertEquals(ImmutableMap.copyOf(expectedResults), results);
  }

  @Test
  public void testAllHealthyHosts() throws Exception {
    List<DynamicRouteConfig> inputList = new ArrayList<>();

    String path1 = "path1";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "/healthCheckPath/";
    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, false, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));
    DynamicRouteConfig dynamicRouteConfig1 = new DynamicRouteConfig(path1, path1ClientConfigs);

    String path2 = "path2";
    String ip2a = "3.4.5.6";
    String ip2b = "4.5.6.7";
    DynamicClientConfig dynamicClientConfig2a =
        new DynamicClientConfig(ip2a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig2b =
        new DynamicClientConfig(ip2b, port, false, healthCheckPath);
    List<DynamicClientConfig> path2ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig2a, dynamicClientConfig2b));
    DynamicRouteConfig dynamicRouteConfig2 = new DynamicRouteConfig(path2, path2ClientConfigs);

    inputList.add(dynamicRouteConfig1);
    inputList.add(dynamicRouteConfig2);

    when(healthCheckCandidateSupplier.get()).thenReturn(ImmutableList.copyOf(inputList));

    // make the health check service return some healthy hosts when it is asked to check the
    // endpoints
    // for both accounts
    ImmutableSet<String> healthyAll1 = ImmutableSet.copyOf(Arrays.asList(ip1a, ip1b));
    ImmutableSet<String> healthyAll2 = ImmutableSet.copyOf(Arrays.asList(ip2a, ip2b));
    when(endpointHealthCheckService.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs)))
        .thenReturn(healthyAll1);
    when(endpointHealthCheckService.checkEndpoints(path2, ImmutableList.copyOf(path2ClientConfigs)))
        .thenReturn(healthyAll2);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    subject =
        new SimpleAppHealthCheckDaemon(
            executor,
            endpointHealthCheckService,
            healthyHostListUpdater,
            healthCheckCandidateSupplier);
    subject.setTestUpdateInterval(1);
    subject.start();

    ArgumentCaptor<ImmutableMap<String, ImmutableSet<String>>> captor =
        ArgumentCaptor.forClass(ImmutableMap.class);
    verify(healthyHostListUpdater, timeout(5000)).accept(captor.capture());

    ImmutableMap<String, ImmutableSet<String>> results = captor.getValue();

    Map<String, ImmutableSet> expectedResults = new HashMap<>();
    expectedResults.put(path1, healthyAll1);
    expectedResults.put(path2, healthyAll2);

    assertEquals(ImmutableMap.copyOf(expectedResults), results);
  }
}
