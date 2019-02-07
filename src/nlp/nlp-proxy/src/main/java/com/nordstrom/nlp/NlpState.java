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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.RouteState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NlpState extends ApplicationState {

  private final ProxyClientFactory clientFactory;
  private AtomicReference<RouteStates> routeStatesRef;
  private AtomicReference<ImmutableList<DynamicRouteConfig>> currentDynamicRouteConfigRef;
  private AtomicReference<ImmutableMap<String, ImmutableSet<String>>> healthyHostMapRef;
  private final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
  private final NlpConfig nlpConfig;
  private final ProxyRouteConfigFactory proxyRouteConfigFactory;

  @VisibleForTesting Runnable routesUpdatedConsumer = null;

  public NlpState(
      NlpConfig appConfig,
      ImmutableList<DynamicRouteConfig> initialDynamicRouteConfigs,
      ProxyRouteConfigFactory proxyRouteConfigFactory) {
    super(appConfig);
    clientFactory = new ProxyClientFactory(this);
    this.proxyRouteConfigFactory = proxyRouteConfigFactory;
    nlpConfig = appConfig;

    // bootstrap empty routeStatesRef - this is the actual routes that we advertise as valid
    routeStatesRef = new AtomicReference<>();
    routeStatesRef.set(new RouteStates(Collections.emptyList(), nlpConfig, this, clientFactory));

    // bootstrap empty healthyHostMap - this is the Sieve/filter that we will use when building
    // ProxyRoutes
    ImmutableMap<String, ImmutableSet<String>> emptyHealthyHostMap =
        ImmutableMap.copyOf(new HashMap<>());
    healthyHostMapRef = new AtomicReference<>();
    healthyHostMapRef.set(emptyHealthyHostMap);

    // bootstrap empty currentDynamicConfigRef - this is the internal representation of our current
    // dynamicRoutes
    ImmutableList<DynamicRouteConfig> emptyDynamicRouteConfigList =
        ImmutableList.copyOf(Collections.emptyList());
    currentDynamicRouteConfigRef = new AtomicReference<>();
    currentDynamicRouteConfigRef.set(emptyDynamicRouteConfigList);

    // enqueue the initial routes
    reloadRouteStates(emptyDynamicRouteConfigList, initialDynamicRouteConfigs);

    // serialize updates to the routeStates with serialized queue
    new Thread(this::startQueue).start();
  }

  /*
   This method is used to expose the current set of candidate dynamic route configs that another daemon/thread will
   use to determine healthchecks
  */
  ImmutableList<DynamicRouteConfig> getCandidateDynamicRouteConfigs() {
    return currentDynamicRouteConfigRef.get();
  }

  /*
   This method is used by the health check daemon to update the current set of healthy hosts by path
  */
  public void updateHealthyHostMap(ImmutableMap<String, ImmutableSet<String>> healthyHostMap) {
    // serialize the operations
    addToQueue(
        () -> {
          ImmutableMap<String, ImmutableSet<String>> currentHealthyHostMap =
              healthyHostMapRef.get();
          // if the new map is different than the old map lets trigger an update
          if (!currentHealthyHostMap.equals(healthyHostMap)) {
            log.info("Current NLP healthyHostMap is: " + healthyHostMap);
            healthyHostMapRef.set(healthyHostMap);
            updateRouteStates();
          }
        });
  }

  /*
   This method is used by the routeReloader to update the if there are any changes
   to the candidate list of routes the NLP should be routing to
  */
  // TODO:DC remove the previous argument since we are caching it locally now
  public void reloadRouteStates(
      ImmutableList<DynamicRouteConfig> previous, ImmutableList<DynamicRouteConfig> updated) {
    // serialize the operations
    addToQueue(
        () -> {
          // check if things are different that what we currently have
          ImmutableList<DynamicRouteConfig> currentDynamicRouteConfig =
              currentDynamicRouteConfigRef.get();
          if (!currentDynamicRouteConfig.equals(updated)) {
            // update the currentDynamicRouteConfigRef to the new value
            currentDynamicRouteConfigRef.set(updated);
            log.info("NLPState - RoutesReloaded with candidate routes: " + updated);
            // lets build new route states
            updateRouteStates();
          }
        });
  }

  public ImmutableMap<String, RouteState> routes() {
    return routeStatesRef.get().routeMap();
  }

  public NlpConfig getNlpConfig() {
    return nlpConfig;
  }

  @VisibleForTesting
  public void addToQueue(Runnable runnable) {
    try {
      blockingQueue.put(runnable);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /*
   This method is the common method to updateRouteStates when
   1) Candidate Routes get updated (and triggers updateRouteStates)
   2) The healthyHostMap gets updated (and triggers updateRouteStates)
  */
  private void updateRouteStates() {
    // grab the current (candidate) dynamicRouteConfig and rebuild everything
    ImmutableList<DynamicRouteConfig> currentDynamicRouteConfig =
        currentDynamicRouteConfigRef.get();

    log.info("NLP updateRouteStates - candidate routes: " + currentDynamicRouteConfig);

    // grab the healthy host map
    ImmutableMap<String, ImmutableSet<String>> healthyHostMap = healthyHostMapRef.get();

    log.info("NLP updateRouteStates - healthy host map: " + healthyHostMap);

    // walk the candidate dynamic route config list and config client health
    ArrayList<DynamicRouteConfig> healthFilteredRoutes = new ArrayList<>();
    for (DynamicRouteConfig routeConfig : currentDynamicRouteConfig) {
      // grab the path from the route config, this is used as the unique identifier into the
      // healthyHostMap
      String path = routeConfig.getPath();

      // grab the healthyIps given the routeConfig's path
      ImmutableSet<String> healthyIpsForPath = healthyHostMap.get(path);

      // grab the candidate DynamicClientConfigs
      List<DynamicClientConfig> candidateClientConfigs = routeConfig.getClientConfigs();

      // filter out the un-healthy client configs
      List<DynamicClientConfig> healthyClientConfigs =
          candidateClientConfigs
              .stream()
              .filter(
                  clientConfig -> {
                    String ipAddress = clientConfig.getIpAddress();
                    return healthyIpsForPath != null && healthyIpsForPath.contains(ipAddress);
                  })
              .collect(Collectors.toList());

      // construct a new DynamicRouteConfig based on the filtered DynamicClientConfigs
      DynamicRouteConfig healthCheckedRouteConfig =
          new DynamicRouteConfig(path, healthyClientConfigs);

      // add to the new list of healthFilteredRoutes
      healthFilteredRoutes.add(healthCheckedRouteConfig);
    }

    // build a new proxy route config list based on filtered healthy filtered routes
    List<ProxyRouteConfig> updatedProxyRouteConfigs =
        proxyRouteConfigFactory.build(healthFilteredRoutes);

    log.info("NLP updateRouteStates - health checked proxy routes: " + updatedProxyRouteConfigs);

    // update the proxyroutestates ref with the updatedProxyRouteConfigs list
    routeStatesRef.set(new RouteStates(updatedProxyRouteConfigs, nlpConfig, this, clientFactory));

    // this hook defaults to null, but in tests we add a hook so we don't have to sleep
    if (routesUpdatedConsumer != null) {
      routesUpdatedConsumer.run();
    }
  }

  private void startQueue() {
    while (true) {
      try {
        blockingQueue.take().run();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
