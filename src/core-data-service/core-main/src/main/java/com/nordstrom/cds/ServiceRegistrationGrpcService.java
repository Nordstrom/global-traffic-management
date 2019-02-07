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
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceRegistrationGrpcService implements GrpcService {
  private final ServiceRegistrationDao serviceRegistrationDao;
  private final ExceptionTransformer exceptionTransformer = new ExceptionTransformer();

  public ServiceRegistrationGrpcService(ServiceRegistrationDao serviceRegistrationDao) {
    this.serviceRegistrationDao = serviceRegistrationDao;
  }

  public String getPackageName() {
    return "nordstrom.gtm.serviceregistration";
  }

  public String getServiceName() {
    return "ServiceRegistration";
  }

  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(Lists.newArrayList(createServiceRegistrationRoute()));
  }

  private GrpcRoute createServiceRegistrationRoute() {
    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse> handler;
    handler =
        new GrpcRequestHandler<>(
            CreateServiceRegistrationRequest::parseFrom,
            (CreateServiceRegistrationRequest request) -> {
              try {
                return serviceRegistrationDao.createServiceRegistration(request);
              } catch (SQLException | CoreDataServiceException e) {
                log.info("create service registration call failed", e);
                throw exceptionTransformer.convertToStatusException(e);
              }
            });

    return new GrpcRoute(this, "CreateServiceRegistration", handler);
  }
}
