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
package com.nordstrom.nfe.config;

import com.nordstrom.gtm.apikey.ApiKey;
import com.typesafe.config.Config;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(fluent = true)
@Getter
@Slf4j
public class NfeConfig extends ApplicationConfig {

  private final GatekeeperConfig gatekeeperConfig;
  private final RoutesConfig routeConfig;
  private final List<ProxyRouteConfig> proxyRoutes;
  private final List<ApiKey> apiKeysConfig;
  private final CoreDatabaseConfig coreDatabaseConfig;
  private final NlpSharedCountConfig nlpSharedCountConfig;
  private final ServiceDeploymentConfig serviceDeploymentConfig;
  private final KubernetesRoutingConfig kubernetesRoutingConfig;
  private final ProxyRouteConfig defaultProxyRouteConfig;
  private final ClientConfig defaultClientConfig;

  public NfeConfig(Config config) {
    super(config.getConfig("nfe.application"));
    log.debug("CONFIG: " + config.root().render());

    proxyRoutes =
        config
            .getConfigList("nfe.proxy.routes")
            .stream()
            .map(ProxyRouteConfig::new)
            .collect(Collectors.toList());

    gatekeeperConfig = GatekeeperConfig.fromConfig("nfe.gatekeeper", config);
    routeConfig = RoutesConfig.fromConfig("nfe", config);

    apiKeysConfig =
        config
            .getConfigList("nfe.apiKeys")
            .stream()
            .map(
                cfg ->
                    ApiKey.newBuilder()
                        .setTeamName(cfg.getString("ou"))
                        .setServiceName(cfg.getString("service"))
                        .setKey(cfg.getString("key"))
                        .build())
            .collect(Collectors.toList());

    this.coreDatabaseConfig = new CoreDatabaseConfig(config.getConfig("nfe.coreDatabase"));
    this.nlpSharedCountConfig = new NlpSharedCountConfig(config.getConfig("nfe.nlpSharedCount"));
    this.serviceDeploymentConfig =
        ServiceDeploymentConfig.fromConfig(config.getConfig("nfe.serviceDeployment"));
    this.kubernetesRoutingConfig =
        KubernetesRoutingConfig.fromConfig(config.getConfig("nfe.kubernetesRouting"));
    this.defaultProxyRouteConfig = new ProxyRouteConfig(config.getConfig("nfe.proxyRouteTemplate"));
    this.defaultClientConfig = ClientConfig.from(config.getConfig("nfe.nlpClient"));
  }
}
