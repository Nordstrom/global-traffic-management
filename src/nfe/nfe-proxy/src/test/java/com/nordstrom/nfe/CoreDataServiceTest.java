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
package com.nordstrom.nfe;

import com.nordstrom.gtm.coredb.GetCustomerAccountNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceRoutingInfoResponse;
import com.nordstrom.gtm.coredb.PathComponents;
import com.nordstrom.gtm.serviceregistration.AccountType;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.servicedeployment.CoreServiceDeploymentInfo;
import com.nordstrom.nfe.testhelpers.MockExternalCoreDataService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.grpc.Status;
import io.grpc.StatusException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CoreDataServiceTest extends Assert {
  private CoreDataService subject;
  private MockExternalCoreDataService mockExternalCoreDataService;

  @Before
  public void beforeEach() {
    // start the gRPC core data service
    mockExternalCoreDataService = new MockExternalCoreDataService();
    mockExternalCoreDataService.start();

    Config config =
        ConfigFactory.load("reference.conf")
            .withValue("nfe.coreDatabase.host", ConfigValueFactory.fromAnyRef("127.0.0.1"))
            .withValue(
                "nfe.coreDatabase.port",
                ConfigValueFactory.fromAnyRef(mockExternalCoreDataService.getPort()));
    NfeConfig nfeConfig = new NfeConfig(config);

    subject = new CoreDataService(nfeConfig.coreDatabaseConfig());
  }

  @After
  public void afterEach() {
    mockExternalCoreDataService.stop();
  }

  @Test
  public void testGetRoutePaths_HappyPath() {
    String accountId1 = "account_id_1";
    String accountId2 = "account_id_2";

    mockExternalCoreDataService.setGetNlpRoutingInfoLogic(
        request -> {
          if (request.getCloudAccountId().equals(accountId1)
              && request.getAccountType().equals(AccountType.AWS)) {
            return GetCustomerAccountNlpRoutingInfoResponse.newBuilder()
                .addPathComponentsArray(
                    PathComponents.newBuilder()
                        .setOrganizationUnit("ou")
                        .setServiceName("sn-1")
                        .build())
                .build();
          }

          if (request.getCloudAccountId().equals(accountId2)
              && request.getAccountType().equals(AccountType.AWS)) {
            return GetCustomerAccountNlpRoutingInfoResponse.newBuilder()
                .addPathComponentsArray(
                    PathComponents.newBuilder()
                        .setOrganizationUnit("ou")
                        .setServiceName("sn-2")
                        .build())
                .build();
          }

          throw new StatusException(Status.INTERNAL);
        });

    Map<String, List<String>> accountRoutePaths =
        subject.getCustomerAccountNlpRoutePaths(Arrays.asList(accountId1, accountId2));

    List<String> expectedRoutePaths1 = Collections.singletonList("/ou/sn-1/");
    assertEquals(expectedRoutePaths1, accountRoutePaths.get(accountId1));

    List<String> expectedRoutePaths2 = Collections.singletonList("/ou/sn-2/");
    assertEquals(expectedRoutePaths2, accountRoutePaths.get(accountId2));
  }

  @Test
  public void testGetRoutePaths_ErrorPath() {
    String accountId1 = "account_id_1";
    String accountId2 = "account_id_2";

    mockExternalCoreDataService.setGetNlpRoutingInfoLogic(
        request -> {
          if (request.getCloudAccountId().equals(accountId1)
              && request.getAccountType().equals(AccountType.AWS)) {
            return GetCustomerAccountNlpRoutingInfoResponse.newBuilder()
                .addPathComponentsArray(
                    PathComponents.newBuilder()
                        .setOrganizationUnit("ou")
                        .setServiceName("sn-1")
                        .build())
                .build();
          }

          if (request.getCloudAccountId().equals(accountId2)) {
            throw new StatusException(Status.NOT_FOUND);
          }

          throw new StatusException(Status.INTERNAL);
        });

    // should return empty result
    Map<String, List<String>> accountRoutePaths =
        subject.getCustomerAccountNlpRoutePaths(Arrays.asList(accountId1, accountId2));
    List<String> expectedRoutePaths1 = Collections.singletonList("/ou/sn-1/");
    assertEquals(expectedRoutePaths1, accountRoutePaths.get(accountId1));

    assertNull(accountRoutePaths.get(accountId2));
  }

  @Test
  public void testGetServiceRoutePaths_HappyPath() throws Exception {
    String serviceName = "service_name";
    String cloudAccountId = "cloud_account_id";
    String serviceDescripiton = "service_description";

    mockExternalCoreDataService.setGetServiceRoutingInfoLogic(
        request -> {
          if (request.getServiceName().equals(serviceName)) {
            return GetServiceRoutingInfoResponse.newBuilder()
                .setPathComponents(
                    PathComponents.newBuilder()
                        .setServiceVersion("v1")
                        .setOrganizationUnit("ou")
                        .setServiceName("service_name_returned")
                        .build())
                .setCloudAccountId(cloudAccountId)
                .setServiceDescription(serviceDescripiton)
                .build();
          }

          throw new StatusException(Status.INTERNAL);
        });

    CoreServiceDeploymentInfo coreServiceDeploymentInfo = subject.getServiceRoutePath(serviceName);
    assertEquals("/service_name_returned/", coreServiceDeploymentInfo.getPath());
    assertEquals(cloudAccountId, coreServiceDeploymentInfo.getCloudAccountId());
    assertEquals(serviceDescripiton, coreServiceDeploymentInfo.getServiceDescription());
  }

  @Test
  public void testGetServiceRoutePaths_ErrorPath() throws Exception {
    mockExternalCoreDataService.setGetServiceRoutingInfoLogic(
        request -> {
          throw new StatusException(Status.INTERNAL);
        });

    Exception exception = null;
    try {
      subject.getServiceRoutePath("service_name");
    } catch (ExecutionException | InterruptedException e) {
      exception = e;
    }

    // should throw an exception
    assertNotNull(exception);
  }
}
