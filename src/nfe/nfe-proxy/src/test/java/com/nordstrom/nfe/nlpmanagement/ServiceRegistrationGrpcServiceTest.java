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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.testhelpers.GrpcTestHelpers;
import com.nordstrom.nfe.testhelpers.MockExternalCoreDataService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServiceRegistrationGrpcServiceTest {
  private ServiceRegistrationGrpcService subject;
  private ServiceRegistrationUpdatedWatcher serviceRegistrationUpdatedWatcher;
  private MockExternalCoreDataService coreDataService;
  private Config config;

  @Before
  public void beforeEach() {
    coreDataService = new MockExternalCoreDataService();
    coreDataService.start();

    config =
        ConfigFactory.load("reference.conf")
            .withValue("nfe.coreDatabase.host", ConfigValueFactory.fromAnyRef("127.0.0.1"))
            .withValue(
                "nfe.coreDatabase.port", ConfigValueFactory.fromAnyRef(coreDataService.getPort()));
    NfeConfig nfeConfig = new NfeConfig(config);
    serviceRegistrationUpdatedWatcher = mock(ServiceRegistrationUpdatedWatcher.class);

    subject =
        new ServiceRegistrationGrpcService(
            nfeConfig.coreDatabaseConfig(), serviceRegistrationUpdatedWatcher);
  }

  @After
  public void afterEach() {
    coreDataService.stop();
  }

  @Test
  public void testRouteCount() {
    assertEquals(1, subject.getRoutes().size());
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
  public void testCreateServiceRegistrationRoute_HappyPath() throws Exception {
    GrpcRoute route = GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceRegistration");

    // should have correct method name and service
    assertEquals("CreateServiceRegistration", route.methodName);
    assertEquals(subject, route.service);

    // test handler
    String serviceName = "Awesome Service";
    CreateServiceRegistrationRequest originalRequest =
        CreateServiceRegistrationRequest.newBuilder().setServiceName(serviceName).build();
    CreateServiceRegistrationResponse expectedResponse =
        CreateServiceRegistrationResponse.newBuilder().build();

    coreDataService.setCreateServiceRegistrationLogic(
        request -> {
          if (request.equals(originalRequest)) {
            return expectedResponse;
          }

          throw new StatusException(Status.INTERNAL);
        });

    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
        handler = route.handler;
    CreateServiceRegistrationResponse response = handler.getAppLogic().apply(originalRequest);

    // should tell the ServiceRegistrationUpdatedWatcher that data was updated
    verify(serviceRegistrationUpdatedWatcher).postServiceRegistrationDataUpdated();

    // should pass the gRPC response to the client
    assertEquals(expectedResponse, response);
  }

  @Test
  public void testCreateServiceRegistrationRoute_ErrorPath() throws Exception {
    GrpcRoute route = GrpcTestHelpers.findRoute(subject.getRoutes(), "CreateServiceRegistration");

    // should have correct method name and service
    assertEquals("CreateServiceRegistration", route.methodName);
    assertEquals(subject, route.service);

    // test handler
    Status expectedStatus = Status.ABORTED.withDescription("I'm too tired..");
    coreDataService.setCreateServiceRegistrationLogic(
        request -> {
          throw new StatusException(expectedStatus);
        });

    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
        handler = route.handler;

    CreateServiceRegistrationRequest request =
        CreateServiceRegistrationRequest.newBuilder().build();

    StatusException statusException = null;
    try {
      handler.getAppLogic().apply(request);
    } catch (StatusException e) {
      statusException = e;
    }

    // should NOT tell the ServiceRegistrationUpdatedWatcher that data was updated
    verify(serviceRegistrationUpdatedWatcher, never()).postServiceRegistrationDataUpdated();

    // should pass the gRPC error through to the client
    assertNotNull(statusException);
    assertEquals(expectedStatus.getCode(), statusException.getStatus().getCode());
    assertEquals(expectedStatus.getDescription(), statusException.getStatus().getDescription());
  }
}
