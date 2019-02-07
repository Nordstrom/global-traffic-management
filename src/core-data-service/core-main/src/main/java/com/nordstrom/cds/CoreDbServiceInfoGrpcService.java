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
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoResponse;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoreDbServiceInfoGrpcService implements GrpcService {
  private ServiceRegistrationDao serviceRegistrationDao;
  private ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

  public CoreDbServiceInfoGrpcService(ServiceRegistrationDao serviceRegistrationDao) {
    this.serviceRegistrationDao = serviceRegistrationDao;
  }

  public String getPackageName() {
    return "nordstrom.gtm.coredb";
  }

  public String getServiceName() {
    return "ServiceInfo";
  }

  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(getNlpRoutingInfoRoute(), getServiceDeployTargetInfoRoute()));
  }

  private GrpcRoute getNlpRoutingInfoRoute() {
    GrpcRequestHandler<GetNlpRoutingInfoRequest, GetNlpRoutingInfoResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            GetNlpRoutingInfoRequest::parseFrom,
            (GetNlpRoutingInfoRequest request) -> {
              try {
                return serviceRegistrationDao.getNlpRoutingInfo(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("get NLP routing info call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "GetNlpRoutingInfo", handler);
  }

  private GrpcRoute getServiceDeployTargetInfoRoute() {
    GrpcRequestHandler<GetServiceDeployTargetInfoRequest, GetServiceDeployTargetInfoResponse>
        handler;
    handler =
        new GrpcRequestHandler<>(
            GetServiceDeployTargetInfoRequest::parseFrom,
            (GetServiceDeployTargetInfoRequest request) -> {
              try {
                return serviceRegistrationDao.getServiceDeployTargetInfo(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("get service deploy target info call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "GetServiceDeployTargetInfo", handler);
  }
}
