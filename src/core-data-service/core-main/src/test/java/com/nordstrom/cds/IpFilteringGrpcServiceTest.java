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
package com.nordstrom.cds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Empty;
import com.nordstrom.cds.TestHelpers.GrpcTestHelpers;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterRequest;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterResponse;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersRequest;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersResponse;
import com.nordstrom.gtm.ipfilter.RemoveAppIpFilterRequest;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IpFilteringGrpcServiceTest extends Assert {
  private IpFilteringGrpcService subject;
  private ControlPlaneDao controlPlaneDao;

  @Before
  public void beforeEach() {
    controlPlaneDao = mock(ControlPlaneDao.class);
    subject = new IpFilteringGrpcService(controlPlaneDao);
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.ipfilter", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("IpFiltering", subject.getServiceName());
  }

  @Test
  public void testAddAppIpFilterSuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "AddAppIpFilter");

    GrpcRequestHandler<AddAppIpFilterRequest, AddAppIpFilterResponse> handler = createRoute.handler;
    AddAppIpFilterRequest request =
        AddAppIpFilterRequest.newBuilder().setServiceName("my service name").build();
    AddAppIpFilterResponse expectedResponse = AddAppIpFilterResponse.newBuilder().build();

    when(controlPlaneDao.addAppIpFilter(request)).thenReturn(expectedResponse);
    AddAppIpFilterResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testAddAppIpFilterFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "AddAppIpFilter");

    GrpcRequestHandler<AddAppIpFilterRequest, AddAppIpFilterResponse> handler = createRoute.handler;
    AddAppIpFilterRequest request =
        AddAppIpFilterRequest.newBuilder().setServiceName("my service name").build();

    when(controlPlaneDao.addAppIpFilter(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testRemoveAppIpFilterSuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "RemoveAppIpFilter");

    GrpcRequestHandler<RemoveAppIpFilterRequest, Empty> handler = createRoute.handler;
    RemoveAppIpFilterRequest request =
        RemoveAppIpFilterRequest.newBuilder().setIpFilterKey("ip filter key").build();
    Empty expectedResponse = Empty.getDefaultInstance();

    when(controlPlaneDao.removeAppIpFilter(request)).thenReturn(expectedResponse);
    Empty actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testRemoveAppIpFilterFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "RemoveAppIpFilter");

    GrpcRequestHandler<RemoveAppIpFilterRequest, Empty> handler = createRoute.handler;
    RemoveAppIpFilterRequest request =
        RemoveAppIpFilterRequest.newBuilder().setIpFilterKey("ip filter key").build();

    when(controlPlaneDao.removeAppIpFilter(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testListAppIpFilterSuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "ListAppIpFilters");

    GrpcRequestHandler<ListAppIpFiltersRequest, ListAppIpFiltersResponse> handler =
        createRoute.handler;
    ListAppIpFiltersRequest request =
        ListAppIpFiltersRequest.newBuilder().setServiceName("cool new service").build();
    ListAppIpFiltersResponse expectedResponse = ListAppIpFiltersResponse.newBuilder().build();

    when(controlPlaneDao.listAppIpFilter(request)).thenReturn(expectedResponse);
    ListAppIpFiltersResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testListAppIpFilterFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "ListAppIpFilters");

    GrpcRequestHandler<ListAppIpFiltersRequest, ListAppIpFiltersResponse> handler =
        createRoute.handler;
    ListAppIpFiltersRequest request =
        ListAppIpFiltersRequest.newBuilder().setServiceName("my service").build();

    when(controlPlaneDao.listAppIpFilter(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }
}
