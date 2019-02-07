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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Empty;
import com.nordstrom.cds.TestHelpers.GrpcTestHelpers;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.coredb.DeleteApiKeyRequest;
import com.nordstrom.gtm.coredb.ListApiKeysRequest;
import com.nordstrom.gtm.coredb.ListApiKeysResponse;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CoreDbApiKeyGrpcServiceTest extends Assert {
  private CoreDbApiKeyGrpcService subject;
  private ApiKeyDao apiKeyDao;

  @Before
  public void beforeEach() {
    apiKeyDao = mock(ApiKeyDao.class);
    subject = new CoreDbApiKeyGrpcService(apiKeyDao);
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.coredb", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("ApiKey", subject.getServiceName());
  }

  @Test
  public void testCreateApiKeyRouteBasicInfo() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "SaveApiKey");

    assertNotNull(createRoute);
    assertEquals(subject, createRoute.service);
  }

  @Test
  public void testSaveApiKeySuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "SaveApiKey");

    GrpcRequestHandler<ApiKey, Empty> handler = createRoute.handler;
    ApiKey request =
        ApiKey.newBuilder()
            .setServiceName("my service")
            .setKeyName("my key name")
            .setKey("my key value")
            .build();

    when(apiKeyDao.saveApiKey(request)).thenReturn(Empty.getDefaultInstance());

    Empty actualResponse = handler.getAppLogic().apply(request);
    assertNotNull(actualResponse);
    verify(apiKeyDao).saveApiKey(request);
  }

  @Test(expected = StatusException.class)
  public void testSaveApiKeyFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "SaveApiKey");

    GrpcRequestHandler<ApiKey, Empty> handler = createRoute.handler;
    ApiKey request =
        ApiKey.newBuilder()
            .setServiceName("my service")
            .setKeyName("my key name")
            .setKey("my key value")
            .build();

    when(apiKeyDao.saveApiKey(request))
        .thenThrow(CoreDataServiceException.apiKeyAlreadyExists("my key"));

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testListApiKeysRouteBasicInfo() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "ListApiKeys");

    assertNotNull(createRoute);
    assertEquals(subject, createRoute.service);
  }

  @Test
  public void testListApiKeysSuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "ListApiKeys");

    GrpcRequestHandler<ListApiKeysRequest, ListApiKeysResponse> handler = createRoute.handler;
    ListApiKeysRequest request =
        ListApiKeysRequest.newBuilder().setServiceName("service name").build();
    ListApiKeysResponse expectedResponse =
        ListApiKeysResponse.newBuilder()
            .addApiKeys(ApiKey.newBuilder().setKeyName("key name").setKey("key value").build())
            .build();

    when(apiKeyDao.listApiKeys(request)).thenReturn(expectedResponse);

    ListApiKeysResponse actualResponse = handler.getAppLogic().apply(request);
    assertEquals(expectedResponse, actualResponse);
    verify(apiKeyDao).listApiKeys(request);
  }

  @Test(expected = StatusException.class)
  public void testListApiKeysFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "ListApiKeys");

    GrpcRequestHandler<ListApiKeysRequest, ListApiKeysResponse> handler = createRoute.handler;
    ListApiKeysRequest request =
        ListApiKeysRequest.newBuilder().setServiceName("service name").build();

    when(apiKeyDao.listApiKeys(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }

  @Test
  public void testDeleteApiKeyRouteBasicInfo() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "DeleteApiKey");

    assertNotNull(createRoute);
    assertEquals(subject, createRoute.service);
  }

  @Test
  public void testDeleteApiKeySuccess() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "DeleteApiKey");

    GrpcRequestHandler<DeleteApiKeyRequest, Empty> handler = createRoute.handler;
    DeleteApiKeyRequest request = DeleteApiKeyRequest.newBuilder().setKey("key value").build();

    when(apiKeyDao.deleteApiKey(request)).thenReturn(Empty.getDefaultInstance());

    Empty actualResponse = handler.getAppLogic().apply(request);
    assertNotNull(actualResponse);
    verify(apiKeyDao).deleteApiKey(request);
  }

  @Test(expected = StatusException.class)
  public void testDeleteApiKeyFailure() throws Exception {
    GrpcRoute createRoute = GrpcTestHelpers.findRoute(subject.getRoutes(), "DeleteApiKey");

    GrpcRequestHandler<DeleteApiKeyRequest, Empty> handler = createRoute.handler;
    DeleteApiKeyRequest request = DeleteApiKeyRequest.newBuilder().setKey("key value").build();

    when(apiKeyDao.deleteApiKey(request)).thenThrow(new SQLException());

    handler.getAppLogic().apply(request);
  }
}
