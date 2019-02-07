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
package com.nordstrom.nfe.nlpmanagement;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.nordstrom.gtm.serviceregistration.ServiceRegistrationGrpc;
import com.nordstrom.nfe.GrpcServerHelpers;
import com.nordstrom.nfe.config.CoreDatabaseConfig;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceRegistrationGrpcService implements GrpcService {
  private CoreDatabaseConfig coreDatabaseConfig;
  private ServiceRegistrationUpdatedWatcher serviceRegistrationUpdatedWatcher;

  public ServiceRegistrationGrpcService(
      CoreDatabaseConfig coreDatabaseConfig,
      ServiceRegistrationUpdatedWatcher serviceRegistrationUpdatedWatcher) {
    this.coreDatabaseConfig = coreDatabaseConfig;
    this.serviceRegistrationUpdatedWatcher = serviceRegistrationUpdatedWatcher;
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
    GrpcRequestHandler<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
        handler =
            new GrpcRequestHandler<>(
                CreateServiceRegistrationRequest::parseFrom,
                request -> {
                  SslContext sslContext =
                      SslContextFactory.buildClientContext(
                          coreDatabaseConfig.getTlsConfig(), InsecureTrustManagerFactory.INSTANCE);
                  ManagedChannel channel =
                      NettyChannelBuilder.forAddress(
                              coreDatabaseConfig.getHost(), coreDatabaseConfig.getPort())
                          .sslContext(sslContext)
                          .build();

                  ServiceRegistrationGrpc.ServiceRegistrationFutureStub futureStub =
                      ServiceRegistrationGrpc.newFutureStub(channel);
                  ListenableFuture<CreateServiceRegistrationResponse> listenableFuture =
                      futureStub.createServiceRegistration(request);

                  try {
                    CreateServiceRegistrationResponse response = listenableFuture.get();
                    serviceRegistrationUpdatedWatcher.postServiceRegistrationDataUpdated();
                    return response;
                  } catch (ExecutionException | InterruptedException e) {
                    throw GrpcServerHelpers.statusExceptionFrom(e);
                  }
                });

    return new GrpcRoute(this, "CreateServiceRegistration", handler);
  }
}
