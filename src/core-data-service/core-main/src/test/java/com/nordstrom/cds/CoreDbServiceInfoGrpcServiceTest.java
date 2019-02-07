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
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoResponse;
import com.nordstrom.gtm.coredb.PathComponents;
import com.nordstrom.gtm.servicedeploytarget.AwsInfo;
import com.nordstrom.gtm.servicedeploytarget.DeploymentPlatformInfo;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CoreDbServiceInfoGrpcServiceTest extends Assert {
  private CoreDbServiceInfoGrpcService subject;
  private ServiceRegistrationDao serviceRegistrationDao;

  @Before
  public void beforeEach() {
    serviceRegistrationDao = mock(ServiceRegistrationDao.class);
    subject = new CoreDbServiceInfoGrpcService(serviceRegistrationDao);
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.coredb", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("ServiceInfo", subject.getServiceName());
  }

  @Test
  public void testRoutesBasicInfo() throws Exception {
    // should have 2 routes
    assertEquals(2, subject.getRoutes().size());

    // should set the subject as the service for every route
    for (GrpcRoute route : subject.getRoutes()) {
      assertEquals(subject, route.service);
    }
  }

  @Test
  public void testGetNlpRoutingInfoSuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "GetNlpRoutingInfo");

    GrpcRequestHandler<GetNlpRoutingInfoRequest, GetNlpRoutingInfoResponse> handler =
        createRoute.handler;
    GetNlpRoutingInfoRequest request =
        GetNlpRoutingInfoRequest.newBuilder().setAwsAccountId("123").build();
    GetNlpRoutingInfoResponse expectedResponse =
        GetNlpRoutingInfoResponse.newBuilder()
            .addPathComponentsArray(
                PathComponents.newBuilder().setServiceName("hello service").build())
            .build();

    when(serviceRegistrationDao.getNlpRoutingInfo(request)).thenReturn(expectedResponse);
    GetNlpRoutingInfoResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testGetNlpRoutingInfoFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "GetNlpRoutingInfo");

    GrpcRequestHandler<GetNlpRoutingInfoRequest, GetNlpRoutingInfoResponse> handler =
        createRoute.handler;
    GetNlpRoutingInfoRequest request =
        GetNlpRoutingInfoRequest.newBuilder().setAwsAccountId("123").build();

    when(serviceRegistrationDao.getNlpRoutingInfo(request))
        .thenThrow(CoreDataServiceException.invalidArgument("foo", "bar"));

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testGetServiceDeployTargetInfoSuccess() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "GetServiceDeployTargetInfo");

    GrpcRequestHandler<GetServiceDeployTargetInfoRequest, GetServiceDeployTargetInfoResponse>
        handler = createRoute.handler;
    GetServiceDeployTargetInfoRequest request =
        GetServiceDeployTargetInfoRequest.newBuilder()
            .setDeployTargetKey("deploy_target_key")
            .build();
    GetServiceDeployTargetInfoResponse expectedResponse =
        GetServiceDeployTargetInfoResponse.newBuilder()
            .setDeploymentPlatformInfo(
                DeploymentPlatformInfo.newBuilder()
                    .setAwsInfo(AwsInfo.newBuilder().setAccountId("aws_account_id").build())
                    .build())
            .build();

    when(serviceRegistrationDao.getServiceDeployTargetInfo(request)).thenReturn(expectedResponse);
    GetServiceDeployTargetInfoResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = StatusException.class)
  public void testGetServiceDeployTargetInfoFailure() throws Exception {
    GrpcRoute createRoute =
        GrpcTestHelpers.findRoute(subject.getRoutes(), "GetServiceDeployTargetInfo");

    GrpcRequestHandler<GetServiceDeployTargetInfoRequest, GetServiceDeployTargetInfoResponse>
        handler = createRoute.handler;
    GetServiceDeployTargetInfoRequest request =
        GetServiceDeployTargetInfoRequest.newBuilder()
            .setDeployTargetKey("deploy_target_key")
            .build();

    when(serviceRegistrationDao.getServiceDeployTargetInfo(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }
}
