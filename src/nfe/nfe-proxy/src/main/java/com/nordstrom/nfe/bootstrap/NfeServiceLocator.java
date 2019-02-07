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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.nordstrom.nfe.*;
import com.nordstrom.nfe.config.GatekeeperConfig;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.nlpmanagement.CustomerAccountNlpDeploymentGrpcService;
import com.nordstrom.nfe.nlpmanagement.CustomerAccountNlpDeploymentWatcher;
import com.nordstrom.nfe.nlpmanagement.KubernetesNlpDeploymentGrpcService;
import com.nordstrom.nfe.nlpmanagement.KubernetesNlpDeploymentWatcher;
import com.nordstrom.nfe.nlpmanagement.ServiceRegistrationGrpcService;
import com.nordstrom.nfe.nlpmanagement.ServiceRegistrationUpdatedWatcher;
import com.nordstrom.nfe.servicedeployment.ServiceDeploymentDao;
import com.nordstrom.nfe.servicedeployment.ServiceDeploymentGrpcService;
import com.xjeffrose.xio.bootstrap.XioServiceLocator;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.http.ProxyClientFactory;
import lombok.Getter;

public class NfeServiceLocator {
  private static NfeServiceLocator instance = null;

  @VisibleForTesting
  public static void setInstance(NfeServiceLocator nfeServiceLocator) {
    instance = nfeServiceLocator;
  }

  public static NfeServiceLocator getInstance() {
    Preconditions.checkNotNull(
        instance,
        "NfeServiceLocator is created by NfeApplicationBootstrap during it's constructor. Make sure that an NfeApplicationBootstrap has been created before accessing NfeServiceLocator.");
    return instance;
  }

  /**
   * This is how NfeApplicationBootstrap initializes the shared instance of NfeServiceLocator. The
   * above statement is why this function is package private and NfeServiceLocator is in the
   * bootstrap package.
   */
  static NfeServiceLocator buildInstance() {
    instance = new NfeServiceLocator();
    return instance;
  }

  public NfeConfig getNfeConfig() {
    return (NfeConfig) XioServiceLocator.getInstance().getApplicationConfig();
  }

  public NfeState getNfeState() {
    return (NfeState) XioServiceLocator.getInstance().getApplicationState();
  }

  public ZkClient getZkClient() {
    return XioServiceLocator.getInstance().getZkClient();
  }

  @Getter(lazy = true)
  private final GatekeeperClientProxy gatekeeperClient = createGatekeeperClientProxy();

  @Getter(lazy = true)
  private final ApiKeyGrpcService apiKeyGrpcService = new ApiKeyGrpcService(getGatekeeperClient());

  @Getter(lazy = true)
  private final CoreDataService coreDataService =
      new CoreDataService(getNfeConfig().coreDatabaseConfig());

  @Getter(lazy = true)
  private final CustomerAccountNlpDeploymentGrpcService customerAccountNlpDeploymentGrpcService =
      new CustomerAccountNlpDeploymentGrpcService(getZkClient(), getObjectMapper());

  @Getter(lazy = true)
  private final CustomerAccountNlpDeploymentWatcher customerAccountNlpDeploymentWatcher =
      new CustomerAccountNlpDeploymentWatcher(
          getRouteStates(), getZkClient(), getObjectMapper(), getCoreDataService());

  @Getter(lazy = true)
  private final KubernetesNlpDeploymentGrpcService kubernetesNlpDeploymentGrpcService =
      new KubernetesNlpDeploymentGrpcService(getZkClient(), getObjectMapper());

  @Getter(lazy = true)
  private final KubernetesNlpDeploymentWatcher kubernetesNlpDeploymentWatcher =
      new KubernetesNlpDeploymentWatcher(
          getRouteStates(), getZkClient(), getObjectMapper(), getCoreDataService());

  @Getter(lazy = true)
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Getter(lazy = true)
  private final ProxyClientFactory proxyClientFactory = new ProxyClientFactory(getNfeState());

  @Getter(lazy = true)
  private final RouteStates routeStates =
      new RouteStates(getNfeState(), getNfeConfig(), getProxyClientFactory());

  @Getter(lazy = true)
  private final ServiceDeploymentDao serviceDeploymentDao =
      new ServiceDeploymentDao(getNfeConfig().serviceDeploymentConfig());

  @Getter(lazy = true)
  private final ServiceDeploymentGrpcService serviceDeploymentGrpcService =
      new ServiceDeploymentGrpcService(getServiceDeploymentDao(), getCoreDataService());

  @Getter(lazy = true)
  private final ServiceRegistrationGrpcService serviceRegistrationGrpcService =
      new ServiceRegistrationGrpcService(
          getNfeConfig().coreDatabaseConfig(), getServiceRegistrationUpdatedWatcher());

  @Getter(lazy = true)
  private final ServiceRegistrationUpdatedWatcher serviceRegistrationUpdatedWatcher =
      new ServiceRegistrationUpdatedWatcher(
          getRouteStates(),
          getZkClient(),
          getCoreDataService(),
          getNfeConfig().nlpSharedCountConfig());

  private GatekeeperClientProxy createGatekeeperClientProxy() {
    GatekeeperConfig config = getNfeConfig().gatekeeperConfig();
    return new GatekeeperClientProxy(config.host, config.port);
  }
}
