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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.Route;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RoutesConfig is the source of all http routing configuration data. This class is meant to store
 * read-only configuration values. Do not put mutable state on this class.
 */
public class RoutesConfig {

  private Map<Route, ProxyRouteConfig> proxyRoutes = new LinkedHashMap<>();

  ImmutableMap<Route, ProxyRouteConfig> copyProxyRoutes() {
    return ImmutableMap.copyOf(proxyRoutes);
  }

  @SuppressWarnings("unchecked")
  public RoutesConfig(Config config) {
    addProxyRoutes((List<Config>) config.getConfigList("proxy.routes"));
  }

  private void addProxyRoutes(List<Config> configs) {
    for (Config config : configs) {
      ProxyRouteConfig routeConfig = new ProxyRouteConfig(config);
      Route route = Route.build(routeConfig.path() + ":*path");
      proxyRoutes.put(route, routeConfig);
    }
  }

  public ImmutableSet<String> permissionsNeeded() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (ProxyRouteConfig config : proxyRoutes.values()) {
      builder.add(config.permissionNeeded());
    }

    return builder.build();
  }

  public static RoutesConfig fromConfig(String key, Config config) {
    return new RoutesConfig(config.getConfig(key));
  }
}
