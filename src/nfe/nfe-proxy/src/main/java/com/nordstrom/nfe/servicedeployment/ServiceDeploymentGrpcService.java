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
package com.nordstrom.nfe.servicedeployment;

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.servicedeployment.DeployedService;
import com.nordstrom.gtm.servicedeployment.StartRoutingRequest;
import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.GrpcServerHelpers;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.StatusException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ServiceDeploymentGrpcService implements GrpcService {
  private ServiceDeploymentDao serviceDeploymentDao;
  private CoreDataService coreDataService;

  public ServiceDeploymentGrpcService(
      ServiceDeploymentDao serviceDeploymentDao, CoreDataService coreDataService) {
    this.serviceDeploymentDao = serviceDeploymentDao;
    this.coreDataService = coreDataService;
  }

  @Override
  public String getPackageName() {
    return "nordstrom.gtm.servicedeployment";
  }

  @Override
  public String getServiceName() {
    return "ServiceDeployment";
  }

  @Override
  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(startRoutingRoute(), stopRoutingRoute()));
  }

  private GrpcRoute startRoutingRoute() {
    GrpcRequestHandler<StartRoutingRequest, DeployedService> handler =
        new GrpcRequestHandler<>(
            StartRoutingRequest::parseFrom,
            (request) -> {
              try {
                CoreServiceDeploymentInfo coreServiceDeploymentInfo =
                    coreDataService.getServiceRoutePath(request.getServiceName());
                DeployedService deployedService =
                    serviceDeploymentDao.addDeployedService(request, coreServiceDeploymentInfo);
                return deployedService;

              } catch (ExecutionException
                  | InterruptedException
                  | SQLException
                  | StatusException e) {
                throw GrpcServerHelpers.statusExceptionFrom(e);
              }
            });

    return new GrpcRoute(this, "StartRouting", handler);
  }

  private GrpcRoute stopRoutingRoute() {
    GrpcRequestHandler<DeployedService, Empty> handler =
        new GrpcRequestHandler<>(
            DeployedService::parseFrom,
            (request) -> {
              try {
                return serviceDeploymentDao.removeDeployedService(request);

              } catch (SQLException e) {
                throw GrpcServerHelpers.statusExceptionFrom(e);
              }
            });

    return new GrpcRoute(this, "StopRouting", handler);
  }
}
