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

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.grpc.GrpcRouteStateBuilder;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.RouteState;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CoreApplicationState extends ApplicationState {
  private final AtomicReference<ImmutableMap<String, RouteState>> routeMapRef;

  public CoreApplicationState(ApplicationConfig applicationConfig) {
    super(applicationConfig);

    DatabaseConfig dbConfig =
        DatabaseConfig.fromConfig(applicationConfig.settings().getConfig("database"));

    ServiceRegistrationDao serviceRegistrationDao = new ServiceRegistrationDao(dbConfig);
    ApiKeyDao apiKeyDao = new ApiKeyDao(dbConfig);
    ControlPlaneDao controlPlaneDao = new ControlPlaneDao(dbConfig);

    List<GrpcService> grpcServices =
        Arrays.asList(
            new ServiceRegistrationGrpcService(serviceRegistrationDao),
            new ServiceDeployTargetGrpcService(serviceRegistrationDao),
            new CoreDbApiKeyGrpcService(apiKeyDao),
            new CoreDbServiceInfoGrpcService(serviceRegistrationDao),
            new IpFilteringGrpcService(controlPlaneDao));

    GrpcRouteStateBuilder grpcRouteStateBuilder = new GrpcRouteStateBuilder();
    List<RouteState> routeStates = grpcRouteStateBuilder.buildGrpcRouteStates(grpcServices);

    LinkedHashMap<String, RouteState> routeMap = new LinkedHashMap<>();
    routeStates.forEach(
        (RouteState state) -> {
          // put this state into the routeMap with the path as the key
          routeMap.put(state.path(), state);
        });

    this.routeMapRef = new AtomicReference<>(ImmutableMap.copyOf(routeMap));
  }

  public ImmutableMap<String, RouteState> routes() {
    return routeMapRef.get();
  }
}
