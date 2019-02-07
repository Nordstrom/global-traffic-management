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

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.http.PersistentProxyHandler;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.ProxyRouteState;
import com.xjeffrose.xio.http.RouteState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RouteStates {
  private final List<ProxyRouteState> proxyRouteStates;
  private final AtomicReference<ImmutableMap<String, ? extends RouteState>> routeMap;

  public RouteStates(
      List<ProxyRouteConfig> proxyRouteConfigs,
      NlpConfig nlpConfig,
      NlpState nlpState,
      ProxyClientFactory clientFactory) {
    List<ProxyRouteState> proxyRouteStates =
        proxyRouteConfigs
            // iterate over a stream of ProxyRouteConfig
            .stream()
            // for each ProxyRouteConfig create a ProxyRouteState
            .map(
                (ProxyRouteConfig config) -> {
                  return ProxyRouteState.create(
                      nlpState,
                      config,
                      new PersistentProxyHandler(clientFactory, config, new SocketAddressHelper()));
                })
            // collect into a List<ProxyRouteState>
            .collect(Collectors.toList());

    LinkedHashMap<String, ProxyRouteState> routeMap = new LinkedHashMap<>();
    proxyRouteStates.forEach(
        (ProxyRouteState state) -> {
          // put this state into the routeMap with the path as the key
          routeMap.put(state.path(), state);
        });

    this.proxyRouteStates = proxyRouteStates;
    this.routeMap = new AtomicReference<>(ImmutableMap.copyOf(routeMap));
  }

  public ImmutableMap<String, RouteState> routeMap() {
    return (ImmutableMap<String, RouteState>) routeMap.get();
  }
}
