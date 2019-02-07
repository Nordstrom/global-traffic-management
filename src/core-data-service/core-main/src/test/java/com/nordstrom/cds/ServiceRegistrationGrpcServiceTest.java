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

import com.nordstrom.cds.TestHelpers.GrpcTestHelpers;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceRegistrationGrpcServiceTest extends Assert {
  private ServiceRegistrationGrpcService subject;
  private ServiceRegistrationDao serviceRegistrationDao;

  @Before
  public void beforeEach() {
    serviceRegistrationDao = mock(ServiceRegistrationDao.class);
    subject = new ServiceRegistrationGrpcService(serviceRegistrationDao);
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.serviceregistration", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("ServiceRegistration", subject.getServiceName());
  }

  @Test
  public void testCreateServiceRegistrationBasicInfo() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceRegistration");

    assertNotNull(createRoute);
    assertEquals(subject, createRoute.service);
  }

  @Test
  public void testCreateServiceRegistrationSuccess() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceRegistration");

    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
        handler = createRoute.handler;
    CreateServiceRegistrationRequest request =
        CreateServiceRegistrationRequest.newBuilder().setServiceName("cool new service").build();
    CreateServiceRegistrationResponse expectedResponse =
        CreateServiceRegistrationResponse.newBuilder().build();

    when(serviceRegistrationDao.createServiceRegistration(request)).thenReturn(expectedResponse);
    CreateServiceRegistrationResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testCreateServiceRegistrationFailure() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceRegistration");

    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
        handler = createRoute.handler;
    CreateServiceRegistrationRequest request =
        CreateServiceRegistrationRequest.newBuilder().setServiceName("my service").build();

    when(serviceRegistrationDao.createServiceRegistration(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }
}
