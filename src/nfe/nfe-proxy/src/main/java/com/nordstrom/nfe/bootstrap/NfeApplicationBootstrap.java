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
package com.nordstrom.nfe.bootstrap;

import com.nordstrom.nfe.GatekeeperAuthorizer;
import com.nordstrom.nfe.GatekeeperClientProxy;
import com.nordstrom.nfe.NfeState;
import com.xjeffrose.xio.SSL.MutualAuthHandler;
import com.xjeffrose.xio.SSL.TlsAuthState;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NfeApplicationBootstrap extends ApplicationBootstrap {
  private final NfeState state;
  private final GatekeeperClientProxy gatekeeperClientProxy;

  public NfeApplicationBootstrap(NfeState state) {
    super(state);
    this.state = state;

    gatekeeperClientProxy = NfeServiceLocator.buildInstance().getGatekeeperClient();
  }

  private SmartHttpPipeline pipelineFragment() {
    return new SmartHttpPipeline() {

      @Override
      public ChannelHandler getApplicationRouter() {
        return new PipelineRouter(state.getRoutes());
      }

      @Override
      public ChannelHandler getTlsAuthenticationHandler() {
        return new MutualAuthHandler() {
          @Override
          public void peerIdentityEstablished(ChannelHandlerContext ctx, String identity) {
            log.debug("peer identity: {}", identity);

            if (!identity.equals(TlsAuthState.UNAUTHENTICATED)) {
              log.debug("firing gatekeeper request");
              gatekeeperClientProxy.mutualAuth(ctx, identity.substring(3), state.allPermissions());
            } else {
              log.debug("unauthenticated peer");
            }
          }
        };
      }

      @Override
      public ChannelHandler getAuthorizationHandler() {
        return new GatekeeperAuthorizer(state, gatekeeperClientProxy);
      }
    };
  }

  public Application build() {
    List<GrpcService> grpcServices =
        Arrays.asList(
            NfeServiceLocator.getInstance().getServiceRegistrationGrpcService(),
            NfeServiceLocator.getInstance().getCustomerAccountNlpDeploymentGrpcService(),
            NfeServiceLocator.getInstance().getApiKeyGrpcService(),
            NfeServiceLocator.getInstance().getServiceDeploymentGrpcService(),
            NfeServiceLocator.getInstance().getKubernetesNlpDeploymentGrpcService());

    // RouteStates is lazily created in NfeServiceLocator.
    // Accessing the getter is required or no RouteStates will be created.
    NfeServiceLocator.getInstance().getRouteStates().buildInitialRoutes(grpcServices);

    // TODO(CK): fix tracing
    addServer(
        "nfe-main", xioServerBootstrap -> xioServerBootstrap.addToPipeline(pipelineFragment()));
    return super.build();
  }
}
