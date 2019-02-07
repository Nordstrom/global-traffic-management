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

import com.google.common.util.concurrent.ListenableFuture;
import com.nordstrom.gtm.coredb.GetCustomerAccountNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetCustomerAccountNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetKubernetesNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetKubernetesNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceRoutingInfoResponse;
import com.nordstrom.gtm.coredb.PathComponents;
import com.nordstrom.gtm.coredb.ServiceRegistrationGrpc;
import com.nordstrom.gtm.serviceregistration.AccountType;
import com.nordstrom.nfe.config.CoreDatabaseConfig;
import com.nordstrom.nfe.servicedeployment.CoreServiceDeploymentInfo;
import com.xjeffrose.xio.SSL.SslContextFactory;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoreDataService {
  private final CoreDatabaseConfig coreDatabaseConfig;

  public CoreDataService(CoreDatabaseConfig coreDatabaseConfig) {
    this.coreDatabaseConfig = coreDatabaseConfig;
  }

  public Map<String, List<String>> getCustomerAccountNlpRoutePaths(List<String> accounIds) {
    Map<String, List<String>> routePathsMap = new HashMap<>();

    for (String accountId : accounIds) {
      Optional<List<String>> optRoutePaths = getCustomerAccountNlpRoutePaths(accountId);
      optRoutePaths.ifPresent(routePaths -> routePathsMap.put(accountId, routePaths));
    }

    return routePathsMap;
  }

  /** This is to get the info that customer account NLPs need when a service is deployed. */
  public CoreServiceDeploymentInfo getServiceRoutePath(String serviceName)
      throws ExecutionException, InterruptedException {
    GetServiceRoutingInfoRequest request =
        GetServiceRoutingInfoRequest.newBuilder().setServiceName(serviceName).build();
    ServiceRegistrationGrpc.ServiceRegistrationFutureStub futureStub = makeFutureStub();

    ListenableFuture<GetServiceRoutingInfoResponse> listenableFuture =
        futureStub.getServiceRoutingInfo(request);
    GetServiceRoutingInfoResponse response = listenableFuture.get();

    return new CoreServiceDeploymentInfo(
        "/" + response.getPathComponents().getServiceName() + "/",
        response.getCloudAccountId(),
        response.getServiceDescription());
  }

  public Map<String, String> getPathsForServices(List<String> serviceName)
      throws ExecutionException, InterruptedException {
    GetKubernetesNlpRoutingInfoRequest request =
        GetKubernetesNlpRoutingInfoRequest.newBuilder().addAllServiceName(serviceName).build();
    ServiceRegistrationGrpc.ServiceRegistrationFutureStub futureStub = makeFutureStub();

    ListenableFuture<GetKubernetesNlpRoutingInfoResponse> listenableFuture =
        futureStub.getKubernetesNlpRoutingInfo(request);
    GetKubernetesNlpRoutingInfoResponse response = listenableFuture.get();

    return response
        .getPathComponentsMap()
        .entrySet()
        .stream()
        .map(
            (entry) -> new AbstractMap.SimpleEntry<>(entry.getKey(), routingPath(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Optional<List<String>> getCustomerAccountNlpRoutePaths(String accountId) {
    GetCustomerAccountNlpRoutingInfoRequest request =
        GetCustomerAccountNlpRoutingInfoRequest.newBuilder()
            .setAccountType(AccountType.AWS)
            .setCloudAccountId(accountId)
            .build();
    ServiceRegistrationGrpc.ServiceRegistrationFutureStub futureStub = makeFutureStub();

    ListenableFuture<GetCustomerAccountNlpRoutingInfoResponse> listenableFuture =
        futureStub.getCustomerAccountNlpRoutingInfo(request);

    try {
      GetCustomerAccountNlpRoutingInfoResponse response = listenableFuture.get();
      return Optional.of(
          response
              .getPathComponentsArrayList()
              .stream()
              .map(this::routingPath)
              .collect(Collectors.toList()));
    } catch (InterruptedException | ExecutionException e) {
      log.error("Unable to get routing info for NLP: ", e);
      return Optional.empty();
    }
  }

  private ServiceRegistrationGrpc.ServiceRegistrationFutureStub makeFutureStub() {
    SslContext sslContext =
        SslContextFactory.buildClientContext(
            coreDatabaseConfig.getTlsConfig(), InsecureTrustManagerFactory.INSTANCE);
    ManagedChannel channel =
        NettyChannelBuilder.forAddress(coreDatabaseConfig.getHost(), coreDatabaseConfig.getPort())
            .sslContext(sslContext)
            .build();

    return ServiceRegistrationGrpc.newFutureStub(channel);
  }

  private String routingPath(PathComponents routingInformation) {
    String path = "/";

    if (routingInformation.getServiceVersion() != null
        && !routingInformation.getServiceVersion().isEmpty()) {
      path += routingInformation.getServiceVersion() + "/";
    }

    path +=
        routingInformation.getOrganizationUnit() + "/" + routingInformation.getServiceName() + "/";

    return path;
  }
}
