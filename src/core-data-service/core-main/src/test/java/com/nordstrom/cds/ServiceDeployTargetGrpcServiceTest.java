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
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetResponse;
import com.nordstrom.gtm.servicedeploytarget.DeleteServiceDeployTargetRequest;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceDeployTargetGrpcServiceTest extends Assert {
  private ServiceDeployTargetGrpcService subject;
  private ServiceRegistrationDao serviceRegistrationDao;

  @Before
  public void beforeEach() {
    serviceRegistrationDao = mock(ServiceRegistrationDao.class);
    subject = new ServiceDeployTargetGrpcService(serviceRegistrationDao);
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.servicedeploytarget", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("ServiceDeployTarget", subject.getServiceName());
  }

  @Test
  public void testCreateServiceDeployTargetSuccess() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceDeployTarget");

    GrpcRequestHandler<CreateServiceDeployTargetRequest, CreateServiceDeployTargetResponse>
        handler = createRoute.handler;
    CreateServiceDeployTargetRequest request =
        CreateServiceDeployTargetRequest.newBuilder().setServiceName("cool new service").build();
    CreateServiceDeployTargetResponse expectedResponse =
        CreateServiceDeployTargetResponse.newBuilder().build();

    when(serviceRegistrationDao.createServiceDeployTarget(request)).thenReturn(expectedResponse);
    CreateServiceDeployTargetResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testCreateServiceDeployTargetFailure() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceDeployTarget");

    GrpcRequestHandler<CreateServiceDeployTargetRequest, CreateServiceDeployTargetResponse>
        handler = createRoute.handler;
    CreateServiceDeployTargetRequest request =
        CreateServiceDeployTargetRequest.newBuilder().setServiceName("my service").build();

    when(serviceRegistrationDao.createServiceDeployTarget(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testDeleteServiceDeployTargetSuccess() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "DeleteServiceDeployTarget");

    GrpcRequestHandler<DeleteServiceDeployTargetRequest, Empty> handler = createRoute.handler;
    DeleteServiceDeployTargetRequest request =
        DeleteServiceDeployTargetRequest.newBuilder()
            .setDeployTargetKey("deploy-target-key")
            .build();
    Empty expectedResponse = Empty.getDefaultInstance();

    when(serviceRegistrationDao.deleteServiceDeployTarget(request)).thenReturn(expectedResponse);
    Empty actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testDeleteServiceDeployTargetFailure() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "DeleteServiceDeployTarget");

    GrpcRequestHandler<DeleteServiceDeployTargetRequest, Empty> handler = createRoute.handler;
    DeleteServiceDeployTargetRequest request =
        DeleteServiceDeployTargetRequest.newBuilder().setDeployTargetKey("my key").build();

    when(serviceRegistrationDao.deleteServiceDeployTarget(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }
}
