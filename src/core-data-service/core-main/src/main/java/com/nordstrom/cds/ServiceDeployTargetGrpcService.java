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
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetResponse;
import com.nordstrom.gtm.servicedeploytarget.DeleteServiceDeployTargetRequest;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceDeployTargetGrpcService implements GrpcService {
  private final ServiceRegistrationDao serviceRegistrationDao;
  private final ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

  public ServiceDeployTargetGrpcService(ServiceRegistrationDao serviceRegistrationDao) {
    this.serviceRegistrationDao = serviceRegistrationDao;
  }

  @Override
  public String getPackageName() {
    return "nordstrom.gtm.servicedeploytarget";
  }

  @Override
  public String getServiceName() {
    return "ServiceDeployTarget";
  }

  @Override
  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(createServiceDeployTargetRoute(), deleteServiceDeployTargetRoute()));
  }

  private GrpcRoute createServiceDeployTargetRoute() {
    GrpcRequestHandler<CreateServiceDeployTargetRequest, CreateServiceDeployTargetResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            CreateServiceDeployTargetRequest::parseFrom,
            (CreateServiceDeployTargetRequest request) -> {
              try {
                return serviceRegistrationDao.createServiceDeployTarget(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("create service deploy target call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "CreateServiceDeployTarget", handler);
  }

  private GrpcRoute deleteServiceDeployTargetRoute() {
    GrpcRequestHandler<DeleteServiceDeployTargetRequest, Empty> handler;
    handler =
        new GrpcRequestHandler<>(
            DeleteServiceDeployTargetRequest::parseFrom,
            (DeleteServiceDeployTargetRequest request) -> {
              try {
                return serviceRegistrationDao.deleteServiceDeployTarget(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("delete service deploy target call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "DeleteServiceDeployTarget", handler);
  }
}
