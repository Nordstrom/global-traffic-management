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
package com.nordstrom.nfe.servicedeployment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Empty;
import com.nordstrom.gtm.servicedeployment.DeployedService;
import com.nordstrom.gtm.servicedeployment.StartRoutingRequest;
import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.testhelpers.GrpcTestHelpers;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceDeploymentGrpcServiceTest extends Assert {
  private ServiceDeploymentGrpcService subject;
  private ServiceDeploymentDao serviceDeploymentDao;
  private CoreDataService coreDataService;

  @Before
  public void beforeEach() {
    serviceDeploymentDao = mock(ServiceDeploymentDao.class);
    coreDataService = mock(CoreDataService.class);

    subject = new ServiceDeploymentGrpcService(serviceDeploymentDao, coreDataService);
  }

  @Test
  public void testRouteCount() {
    assertEquals(2, subject.getRoutes().size());
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.servicedeployment", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("ServiceDeployment", subject.getServiceName());
  }

  @Test
  public void testStartRoutingRoute() throws Exception {
    String expectedMethodName = "StartRouting";
    GrpcRoute route = GrpcTestHelpers.findRoute(subject.getRoutes(), expectedMethodName);

    // test basic info
    assertEquals(expectedMethodName, route.methodName);
    assertEquals(subject, route.service);

    // test handler
    GrpcRequestHandler<StartRoutingRequest, DeployedService> handler = route.handler;
    StartRoutingRequest request = StartRoutingRequest.newBuilder().build();
    DeployedService expectedResponse =
        DeployedService.newBuilder().setDeploymentId("1234-abcd").build();
    CoreServiceDeploymentInfo coreServiceDeploymentInfo =
        new CoreServiceDeploymentInfo("path", "cloud_accouht_id", "service_description");

    when(coreDataService.getServiceRoutePath(request.getServiceName()))
        .thenReturn(coreServiceDeploymentInfo);
    when(serviceDeploymentDao.addDeployedService(request, coreServiceDeploymentInfo))
        .thenReturn(expectedResponse);

    DeployedService actualResponse = handler.getAppLogic().apply(request);

    // should return the response from ServiceDeploymentDao
    assertSame(expectedResponse, actualResponse);
  }

  @Test
  public void testStopRoutingRoute() throws Exception {
    String expectedMethodName = "StopRouting";
    GrpcRoute route = GrpcTestHelpers.findRoute(subject.getRoutes(), expectedMethodName);

    // test basic info
    assertEquals(expectedMethodName, route.methodName);
    assertEquals(subject, route.service);

    // test handler
    GrpcRequestHandler<DeployedService, Empty> handler = route.handler;
    DeployedService request = DeployedService.newBuilder().setDeploymentId("1234-abcd").build();
    Empty expectedResponse = Empty.newBuilder().build();
    when(serviceDeploymentDao.removeDeployedService(request)).thenReturn(expectedResponse);

    Empty actualResponse = handler.getAppLogic().apply(request);

    // should return the response from ServiceDeploymentDao
    assertSame(expectedResponse, actualResponse);
  }
}
