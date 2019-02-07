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
package com.nordstrom.nlp;

import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.http.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class SimpleProxyRouteConfigFactory implements ProxyRouteConfigFactory {

  private final NlpConfig nlpConfig;

  public SimpleProxyRouteConfigFactory(NlpConfig nlpConfig) {
    this.nlpConfig = nlpConfig;
  }

  public List<ProxyRouteConfig> build(List<DynamicRouteConfig> dynamicRouteConfigs) {
    List<ProxyRouteConfig> proxyRouteConfigs = new ArrayList<ProxyRouteConfig>();
    for (DynamicRouteConfig dynamicRouteConfig : dynamicRouteConfigs) {
      proxyRouteConfigs.add(buildRouteConfig(dynamicRouteConfig));
    }
    return proxyRouteConfigs;
  }

  private ProxyRouteConfig buildRouteConfig(DynamicRouteConfig dynamicRouteConfig) {
    List<ClientConfig> clientConfigs = new ArrayList<ClientConfig>();
    for (DynamicClientConfig dynamicClientConfig : dynamicRouteConfig.getClientConfigs()) {
      clientConfigs.add(buildClientConfig(dynamicClientConfig));
    }

    ProxyRouteConfig routeConfig =
        ProxyRouteConfig.newBuilder(nlpConfig.getDefaultProxyRouteConfig())
            .setPath(dynamicRouteConfig.getPath())
            .setClientConfigs(clientConfigs)
            .build();

    return routeConfig;
  }

  private ClientConfig buildClientConfig(DynamicClientConfig dynamicClientConfig) {
    ClientConfig builderClientConfig;
    if (dynamicClientConfig.isTlsEnabled()) {
      builderClientConfig = nlpConfig.getDefaultClientConfig();
    } else {
      // when tls is disabled we will pull the client config that has useSsl = false
      builderClientConfig = nlpConfig.getNoSSLDefaultClientConfig();
    }
    ClientConfig clientConfig =
        ClientConfig.newBuilder(builderClientConfig)
            .setRemote(
                new InetSocketAddress(
                    dynamicClientConfig.getIpAddress(), dynamicClientConfig.getPort()))
            .build();
    return clientConfig;
  }
}
