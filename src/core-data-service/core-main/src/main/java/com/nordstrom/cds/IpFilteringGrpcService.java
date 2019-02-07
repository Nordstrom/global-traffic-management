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
import com.nordstrom.gtm.ipfilter.AddAppIpFilterRequest;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterResponse;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersRequest;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersResponse;
import com.nordstrom.gtm.ipfilter.RemoveAppIpFilterRequest;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpFilteringGrpcService implements GrpcService {
  private final ControlPlaneDao controlPlaneDao;
  private final ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

  public IpFilteringGrpcService(ControlPlaneDao controlPlaneDao) {
    this.controlPlaneDao = controlPlaneDao;
  }

  public String getPackageName() {
    return "nordstrom.gtm.ipfilter";
  }

  public String getServiceName() {
    return "IpFiltering";
  }

  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(
            addAppIpFilterRoute(), removeAppIpFilterRoute(), listAppIpFilterRoute()));
  }

  private GrpcRoute addAppIpFilterRoute() {
    GrpcRequestHandler<AddAppIpFilterRequest, AddAppIpFilterResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            AddAppIpFilterRequest::parseFrom,
            (AddAppIpFilterRequest request) -> {
              try {
                return controlPlaneDao.addAppIpFilter(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("add app ip filter call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "AddAppIpFilter", handler);
  }

  private GrpcRoute removeAppIpFilterRoute() {
    GrpcRequestHandler<RemoveAppIpFilterRequest, Empty> handler;
    handler =
        new GrpcRequestHandler<>(
            RemoveAppIpFilterRequest::parseFrom,
            (RemoveAppIpFilterRequest request) -> {
              try {
                return controlPlaneDao.removeAppIpFilter(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("remove app ip filter call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "RemoveAppIpFilter", handler);
  }

  private GrpcRoute listAppIpFilterRoute() {
    GrpcRequestHandler<ListAppIpFiltersRequest, ListAppIpFiltersResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            ListAppIpFiltersRequest::parseFrom,
            (ListAppIpFiltersRequest request) -> {
              try {
                return controlPlaneDao.listAppIpFilter(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("list app ip filter call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "ListAppIpFilters", handler);
  }
}
