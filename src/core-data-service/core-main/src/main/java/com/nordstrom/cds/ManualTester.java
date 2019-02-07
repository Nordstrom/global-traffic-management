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

import com.google.protobuf.Empty;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.coredb.DeleteApiKeyRequest;
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoRequest;
import com.nordstrom.gtm.coredb.ListApiKeysRequest;
import com.nordstrom.gtm.coredb.ListApiKeysResponse;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterRequest;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterResponse;
import com.nordstrom.gtm.ipfilter.IpFilter;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersRequest;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersResponse;
import com.nordstrom.gtm.ipfilter.RemoveAppIpFilterRequest;
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.DeleteServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.DeploymentPlatformInfo;
import com.nordstrom.gtm.servicedeploytarget.Environment;
import com.nordstrom.gtm.servicedeploytarget.GcpInfo;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.xjeffrose.xio.config.ThrowingFunction;
import java.sql.SQLException;

public class ManualTester {
  private ServiceRegistrationDao serviceRegistrationDao;
  private ControlPlaneDao controlPlaneDao;
  private ApiKeyDao apiKeyDao;

  public ManualTester() {
    DatabaseConfig databaseConfig = new DatabaseConfig("core", "127.0.0.1", "root", "test123");

    this.serviceRegistrationDao = new ServiceRegistrationDao(databaseConfig);
    this.controlPlaneDao = new ControlPlaneDao(databaseConfig);
    this.apiKeyDao = new ApiKeyDao(databaseConfig);
  }

  void doTheThings() {}

  // - Service Registration

  void doCreateServiceRegistration() {
    CreateServiceRegistrationRequest request =
        CreateServiceRegistrationRequest.newBuilder()
            .setOrganizationUnit("my-ou-2")
            .setServiceName("service-name-3")
            .setServiceNowId(2222)
            .setDescription("service-description-3")
            .setIsDefaultAllow(true)
            .build();

    get(request, serviceRegistrationDao::createServiceRegistration);
  }

  void doCreateServiceDeployTarget() {
    CreateServiceDeployTargetRequest request =
        CreateServiceDeployTargetRequest.newBuilder()
            .setServiceName("service-name-1")
            //             .setServiceVersion("v1.1")
            .setEnvironment(Environment.PROD)
            //             .setHealthCheckPath("/healthyme/")
            //             .setIsTlsEnabled(true)
            .setDeploymentPlatformInfo(
                DeploymentPlatformInfo.newBuilder()
                    .setGcpInfo(GcpInfo.newBuilder().setAccountId("my-aws-account-id-1").build())
                    .build())
            .build();

    get(request, serviceRegistrationDao::createServiceDeployTarget);
  }

  void doDeleteServiceDeployTarget() {
    DeleteServiceDeployTargetRequest request =
        DeleteServiceDeployTargetRequest.newBuilder()
            .setDeployTargetKey("292d81a1-d043-4743-b689-8d23742d03e9")
            .build();

    get(request, serviceRegistrationDao::deleteServiceDeployTarget);
  }

  void doGetServiceDeployTargetInfo() {
    GetServiceDeployTargetInfoRequest request =
        GetServiceDeployTargetInfoRequest.newBuilder()
            .setDeployTargetKey("abcd-7ijunh6ybg5tf4-hi")
            .build();

    get(request, serviceRegistrationDao::getServiceDeployTargetInfo);
  }

  void doGetNlpRoutingInfo() {
    GetNlpRoutingInfoRequest request =
        GetNlpRoutingInfoRequest.newBuilder().setAwsAccountId("aws-11111").build();

    get(request, serviceRegistrationDao::getNlpRoutingInfo);
  }

  // - Ip Filter

  AddAppIpFilterResponse doAddIpFilter() {
    AddAppIpFilterRequest request =
        AddAppIpFilterRequest.newBuilder()
            .setServiceName("service-name-1")
            .setIpFilter(
                IpFilter.newBuilder()
                    //                    .setType(IpFilter.Type.DENY)
                    .setCidrAddress("1.1.1.0/24")
                    //                    .setIsDisabled(true)
                    .setNotes("this is my filter 3")
                    .build())
            .build();

    return get(request, controlPlaneDao::addAppIpFilter);
  }

  Empty doRemoveIpFilter() {
    RemoveAppIpFilterRequest request =
        RemoveAppIpFilterRequest.newBuilder()
            .setServiceName("service-name-1")
            .setIpFilterKey("c2a7e47e-4b60-494a-9cbb-08eb242af8b4")
            .build();

    return get(request, controlPlaneDao::removeAppIpFilter);
  }

  ListAppIpFiltersResponse doListIpFilter() {
    ListAppIpFiltersRequest request =
        ListAppIpFiltersRequest.newBuilder().setServiceName("service-name-1").build();

    return get(request, controlPlaneDao::listAppIpFilter);
  }

  // - ApiKey

  void doApiKeyThings() {
    listApiKeys(apiKeyDao);

    createApiKey(apiKeyDao, "key name 1", "key value 1");

    listApiKeys(apiKeyDao);

    createApiKey(apiKeyDao, "key name 2", "key value 2");
    createApiKey(apiKeyDao, "key name 3", "key value 3");

    listApiKeys(apiKeyDao);

    deleteApiKey(apiKeyDao, "key value 2");

    listApiKeys(apiKeyDao);

    deleteApiKey(apiKeyDao, "key value 1");
    deleteApiKey(apiKeyDao, "key value 3");

    listApiKeys(apiKeyDao);

    System.out.println(" - Done - ");
  }

  private void createApiKey(ApiKeyDao apiKeyDao, String name, String key) {
    ApiKey createApiKeyRequest =
        ApiKey.newBuilder().setServiceName("service name").setKeyName(name).setKey(key).build();
    Empty createApiKeyResponse;
    try {
      createApiKeyResponse = apiKeyDao.saveApiKey(createApiKeyRequest);
    } catch (SQLException | CoreDataServiceException e) {
      e.printStackTrace();
      return;
    }
    System.out.println(createApiKeyResponse);
  }

  private void listApiKeys(ApiKeyDao apiKeyDao) {
    ListApiKeysRequest listApiKeysRequest =
        ListApiKeysRequest.newBuilder().setServiceName("service name").build();
    ListApiKeysResponse listApiKeysResponse;
    try {
      listApiKeysResponse = apiKeyDao.listApiKeys(listApiKeysRequest);
    } catch (SQLException | CoreDataServiceException e) {
      e.printStackTrace();
      return;
    }
    System.out.println(listApiKeysResponse);
  }

  private void deleteApiKey(ApiKeyDao apiKeyDao, String key) {
    DeleteApiKeyRequest deleteApiKeyRequest = DeleteApiKeyRequest.newBuilder().setKey(key).build();
    Empty deleteApiKeyResponse;
    try {
      deleteApiKeyResponse = apiKeyDao.deleteApiKey(deleteApiKeyRequest);
    } catch (SQLException | CoreDataServiceException e) {
      e.printStackTrace();
      return;
    }
    System.out.println(deleteApiKeyResponse);
  }

  private <Request, Response> Response get(
      Request request, ThrowingFunction<Request, Response> function) {
    try {
      Response response = function.apply(request);
      System.out.println(response);
      return response;
    } catch (Exception e) {
      System.out.println(e);
      return null;
    }
  }
}
