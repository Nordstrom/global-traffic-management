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

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.coredb.DeleteApiKeyRequest;
import com.nordstrom.gtm.coredb.ListApiKeysRequest;
import com.nordstrom.gtm.coredb.ListApiKeysResponse;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class CoreDbApiKeyGrpcService implements GrpcService {
  private ApiKeyDao apiKeyDao;
  private ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

  public CoreDbApiKeyGrpcService(ApiKeyDao apiKeyDao) {
    this.apiKeyDao = apiKeyDao;
  }

  public String getPackageName() {
    return "nordstrom.gtm.coredb";
  }

  public String getServiceName() {
    return "ApiKey";
  }

  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(saveApiKeyRoute(), listApiKeysRoute(), deleteApiKeyRoute()));
  }

  private GrpcRoute saveApiKeyRoute() {
    GrpcRequestHandler<ApiKey, Empty> handler;
    handler =
        new GrpcRequestHandler<>(
            ApiKey::parseFrom,
            (ApiKey request) -> {
              try {
                return apiKeyDao.saveApiKey(request);
              } catch (SQLException | CoreDataServiceException e) {
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "SaveApiKey", handler);
  }

  private GrpcRoute listApiKeysRoute() {
    GrpcRequestHandler<ListApiKeysRequest, ListApiKeysResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            ListApiKeysRequest::parseFrom,
            (ListApiKeysRequest request) -> {
              try {
                return apiKeyDao.listApiKeys(request);
              } catch (SQLException | CoreDataServiceException e) {
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "ListApiKeys", handler);
  }

  private GrpcRoute deleteApiKeyRoute() {
    GrpcRequestHandler<DeleteApiKeyRequest, Empty> handler;
    handler =
        new GrpcRequestHandler<>(
            DeleteApiKeyRequest::parseFrom,
            (DeleteApiKeyRequest request) -> {
              try {
                return apiKeyDao.deleteApiKey(request);
              } catch (SQLException | CoreDataServiceException e) {
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "DeleteApiKey", handler);
  }
}
