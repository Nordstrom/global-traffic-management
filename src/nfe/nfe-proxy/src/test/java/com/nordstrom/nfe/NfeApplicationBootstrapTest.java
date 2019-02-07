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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.nfe.bootstrap.NfeApplicationBootstrap;
import com.nordstrom.nfe.bootstrap.NfeServiceLocator;
import com.nordstrom.nfe.config.NfeConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.grpc.GrpcService;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class NfeApplicationBootstrapTest extends Assert {
  @Test
  public void testRegisteredGrpcServices() {
    // Creating this using the convenience initializer to get the default gRPC services.
    Config config = ConfigFactory.load();
    NfeConfig nfeConfig = new NfeConfig(config);
    NfeState nfeState = new NfeState(nfeConfig);
    NfeApplicationBootstrap subject = new NfeApplicationBootstrap(nfeState);

    RouteStates routeStates = mock(RouteStates.class);
    NfeServiceLocator.setInstance(spy(NfeServiceLocator.getInstance()));
    when(NfeServiceLocator.getInstance().getRouteStates()).thenReturn(routeStates);

    Application application = subject.build();

    ArgumentCaptor<List<GrpcService>> captor = ArgumentCaptor.forClass(List.class);
    verify(routeStates).buildInitialRoutes(captor.capture());

    // Spot check to ensure we have the correct services registered.
    assertEquals(5, captor.getValue().size());
    assertEquals("ServiceRegistration", captor.getValue().get(0).getServiceName());
    assertEquals("NlpDeployment", captor.getValue().get(1).getServiceName());
    assertEquals("ApiKeyer", captor.getValue().get(2).getServiceName());
    assertEquals("ServiceDeployment", captor.getValue().get(3).getServiceName());
    assertEquals("KubernetesDeployment", captor.getValue().get(4).getServiceName());

    application.close();
  }
}
