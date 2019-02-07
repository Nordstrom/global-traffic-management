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

import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.DynamicClientConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AppEndpointHealthCheckServiceTest extends Assert {

  @Mock OkHttpClientWrapper clientWrapper;

  AppEndpointHealthCheckService subject;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void test_Client_Configs_with_null_healthCheckPaths() throws Exception {
    String path1 = "/path1/";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = null;

    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, false, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, false, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));

    subject = new AppEndpointHealthCheckService(clientWrapper);

    ImmutableSet<String> results1 =
        subject.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs));

    // when a DynamicClientConfig has a null healthcheckpath, we will auto-mark that
    // DynamicClientConfig as healthy
    assertEquals(ImmutableSet.copyOf(Arrays.asList(ip1a, ip1b)), results1);
  }

  @Test
  public void test_Client_Configs_with_healthChecks_NO_heatlthchecks_succeed() throws Exception {
    String path1 = "/path1/";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "health-check-path";
    boolean tlsEnabled = false;
    Request request = new Request.Builder().url("http://moocow.com").build();
    Response response =
        new Response.Builder()
            .request(request)
            .code(500)
            .protocol(Protocol.HTTP_1_0)
            .message("boo")
            .build();

    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, tlsEnabled, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, tlsEnabled, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));

    when(clientWrapper.buildRequest(ip1a, port, healthCheckPath, tlsEnabled)).thenReturn(request);
    when(clientWrapper.buildRequest(ip1b, port, healthCheckPath, tlsEnabled)).thenReturn(request);
    when(clientWrapper.executeRequest(request)).thenReturn(response);

    subject = new AppEndpointHealthCheckService(clientWrapper);

    ImmutableSet<String> results1 =
        subject.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs));

    // since none of the healthchecks succeeded the result should be an empty list
    assertEquals(ImmutableSet.copyOf(Arrays.asList()), results1);
  }

  @Test
  public void test_Client_Configs_with_healthChecks_SOME_healthchecks_succeed() throws Exception {
    String path1 = "/path1/";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "health-check-path";
    boolean tlsEnabled = false;
    Request successRequest = new Request.Builder().url("http://moocow.com").build();
    Request failureRequest = new Request.Builder().url("http://moocow.com").build();

    Response successResponse =
        new Response.Builder()
            .request(successRequest)
            .code(200)
            .protocol(Protocol.HTTP_1_0)
            .message("boo")
            .build();

    Response failureResponse =
        new Response.Builder()
            .request(failureRequest)
            .code(500)
            .protocol(Protocol.HTTP_1_0)
            .message("boo")
            .build();

    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, tlsEnabled, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, tlsEnabled, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));

    when(clientWrapper.buildRequest(ip1a, port, healthCheckPath, tlsEnabled))
        .thenReturn(successRequest);
    when(clientWrapper.executeRequest(successRequest)).thenReturn(successResponse);

    when(clientWrapper.buildRequest(ip1b, port, healthCheckPath, tlsEnabled))
        .thenReturn(failureRequest);
    when(clientWrapper.executeRequest(failureRequest)).thenReturn(failureResponse);

    subject = new AppEndpointHealthCheckService(clientWrapper);

    ImmutableSet<String> results1 =
        subject.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs));

    // only ip1a should be healthy since the ip1b request failed
    assertEquals(ImmutableSet.copyOf(Arrays.asList(ip1a)), results1);
  }

  @Test
  public void test_Client_Configs_with_healthChecks_ALL_healthchecks_succeed() throws Exception {
    String path1 = "/path1/";
    String ip1a = "1.2.3.4";
    String ip1b = "2.3.4.5";
    int port = 123;
    String healthCheckPath = "health-check-path";
    boolean tlsEnabled = false;
    Request request = new Request.Builder().url("http://moocow.com").build();
    Response response =
        new Response.Builder()
            .request(request)
            .code(200)
            .protocol(Protocol.HTTP_1_0)
            .message("boo")
            .build();

    DynamicClientConfig dynamicClientConfig1a =
        new DynamicClientConfig(ip1a, port, tlsEnabled, healthCheckPath);
    DynamicClientConfig dynamicClientConfig1b =
        new DynamicClientConfig(ip1b, port, tlsEnabled, healthCheckPath);
    List<DynamicClientConfig> path1ClientConfigs =
        new ArrayList<>(Arrays.asList(dynamicClientConfig1a, dynamicClientConfig1b));

    when(clientWrapper.buildRequest(ip1a, port, healthCheckPath, tlsEnabled)).thenReturn(request);
    when(clientWrapper.buildRequest(ip1b, port, healthCheckPath, tlsEnabled)).thenReturn(request);
    when(clientWrapper.executeRequest(request)).thenReturn(response);

    subject = new AppEndpointHealthCheckService(clientWrapper);

    ImmutableSet<String> results1 =
        subject.checkEndpoints(path1, ImmutableList.copyOf(path1ClientConfigs));

    // all of the ip addresses from the two DynamicClientConfigs should have passed
    assertEquals(ImmutableSet.copyOf(Arrays.asList(ip1a, ip1b)), results1);
  }
}
