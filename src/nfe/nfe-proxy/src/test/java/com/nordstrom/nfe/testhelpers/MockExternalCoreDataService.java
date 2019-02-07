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
package com.nordstrom.nfe.testhelpers;

import com.nordstrom.gtm.coredb.GetCustomerAccountNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetCustomerAccountNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceRoutingInfoResponse;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.http.GrpcAppLogic;
import io.grpc.Server;
import io.grpc.StatusException;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;

public class MockExternalCoreDataService {
  private class ServiceRegistrationService
      extends com.nordstrom.gtm.serviceregistration.ServiceRegistrationGrpc
          .ServiceRegistrationImplBase {
    @Override
    public void createServiceRegistration(
        CreateServiceRegistrationRequest request,
        StreamObserver<CreateServiceRegistrationResponse> responseObserver) {
      executeAppLogic(request, responseObserver, createServiceRegistrationLogic);
    }
  }

  private class CoreDbServiceRegistrationService
      extends com.nordstrom.gtm.coredb.ServiceRegistrationGrpc.ServiceRegistrationImplBase {
    @Override
    public void getCustomerAccountNlpRoutingInfo(
        GetCustomerAccountNlpRoutingInfoRequest request,
        StreamObserver<GetCustomerAccountNlpRoutingInfoResponse> responseObserver) {
      executeAppLogic(request, responseObserver, getNlpRoutingInfoLogic);
    }

    @Override
    public void getServiceRoutingInfo(
        GetServiceRoutingInfoRequest request,
        StreamObserver<GetServiceRoutingInfoResponse> responseObserver) {
      executeAppLogic(request, responseObserver, getServiceRoutingInfoLogic);
    }
  }

  private GrpcAppLogic<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
      createServiceRegistrationLogic;
  private GrpcAppLogic<
          GetCustomerAccountNlpRoutingInfoRequest, GetCustomerAccountNlpRoutingInfoResponse>
      getNlpRoutingInfoLogic;
  private GrpcAppLogic<GetServiceRoutingInfoRequest, GetServiceRoutingInfoResponse>
      getServiceRoutingInfoLogic;

  private Server grpcServer;
  private int port;

  public MockExternalCoreDataService() {
    this(5678);
  }

  public MockExternalCoreDataService(int port) {
    this.port = port;
    Config config = ConfigFactory.load("reference.conf");
    TlsConfig tlsConfig = new TlsConfig(config.getConfig("xio.serverSettings.tls"));
    SslContext sslContext =
        SslContextFactory.buildServerContext(tlsConfig, InsecureTrustManagerFactory.INSTANCE);
    this.grpcServer =
        NettyServerBuilder.forPort(port)
            .sslContext(sslContext)
            .addService(new ServiceRegistrationService())
            .addService(new CoreDbServiceRegistrationService())
            .build();
  }

  public int getPort() {
    return port;
  }

  public void start() {
    try {
      grpcServer.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    grpcServer.shutdownNow();
  }

  public void setCreateServiceRegistrationLogic(
      GrpcAppLogic<CreateServiceRegistrationRequest, CreateServiceRegistrationResponse>
          createServiceRegistrationLogic) {
    this.createServiceRegistrationLogic = createServiceRegistrationLogic;
  }

  public void setGetNlpRoutingInfoLogic(
      GrpcAppLogic<
              GetCustomerAccountNlpRoutingInfoRequest, GetCustomerAccountNlpRoutingInfoResponse>
          getNlpRoutingInfoLogic) {
    this.getNlpRoutingInfoLogic = getNlpRoutingInfoLogic;
  }

  public void setGetServiceRoutingInfoLogic(
      GrpcAppLogic<GetServiceRoutingInfoRequest, GetServiceRoutingInfoResponse>
          getServiceRoutingInfoLogic) {
    this.getServiceRoutingInfoLogic = getServiceRoutingInfoLogic;
  }

  private <
          GrpcRequest extends com.google.protobuf.GeneratedMessageV3,
          GrpcResponse extends com.google.protobuf.GeneratedMessageV3>
      void executeAppLogic(
          GrpcRequest request,
          StreamObserver<GrpcResponse> responseObserver,
          GrpcAppLogic<GrpcRequest, GrpcResponse> appLogic) {
    try {
      GrpcResponse response = appLogic.apply(request);
      responseObserver.onNext(response);
    } catch (StatusException e) {
      responseObserver.onError(e);
    }

    responseObserver.onCompleted();
  }
}
