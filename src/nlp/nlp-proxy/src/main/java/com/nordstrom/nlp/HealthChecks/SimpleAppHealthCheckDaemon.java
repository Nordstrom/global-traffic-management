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
package com.nordstrom.nlp.HealthChecks;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * SimpleAppHealthCheckDaemon is used to do the following at a regular interval 1) Get the current
 * list of health check candidates 2) Apply the healthchecks against the candidate list 3) update
 * the healthyHostListUpdater with the current healthy map of accountId/IpAddresses
 */
@Slf4j
public class SimpleAppHealthCheckDaemon {
  static final int HTTPS_PORT = 443;
  static long updateInterval = 10;

  private enum HealthCheckState {
    READY,
    IN_PROGRESS
  }

  private HealthCheckState state = HealthCheckState.READY;
  private Consumer<ImmutableMap<String, ImmutableSet<String>>> healthyHostListUpdater;
  private Supplier<ImmutableList<DynamicRouteConfig>> healthCheckCandidateSupplier;
  private final ScheduledExecutorService executor;
  private final AppEndpointHealthCheckService endpointHealthCheckService;
  private AtomicBoolean started = new AtomicBoolean(false);

  /**
   * @param executor This is the service that provides of the background thread execution
   *     environment
   * @param endpointHealthCheckService This service is used to execute the actual healthcheck calls
   * @param healthyHostListUpdater This is the entity that is interested in the results of our
   *     health checks, we update them periodically
   * @param healthCheckCandidateSupplier This is the supplier of the candidate
   *     accountId/IpAddressLists that we will healthcheck
   */
  public SimpleAppHealthCheckDaemon(
      ScheduledExecutorService executor,
      AppEndpointHealthCheckService endpointHealthCheckService,
      Consumer<ImmutableMap<String, ImmutableSet<String>>> healthyHostListUpdater,
      Supplier<ImmutableList<DynamicRouteConfig>> healthCheckCandidateSupplier) {
    this.executor = executor;
    this.endpointHealthCheckService = endpointHealthCheckService;
    this.healthyHostListUpdater = healthyHostListUpdater;
    this.healthCheckCandidateSupplier = healthCheckCandidateSupplier;
  }

  /**
   * This method kicks off the thread to do the listening. It can only be called once and all the
   * required parameters must be set before executing
   */
  public void start() {
    Preconditions.checkNotNull(healthyHostListUpdater, "updater cannot be null");
    Preconditions.checkNotNull(healthCheckCandidateSupplier, "supplier cannot be null");
    Preconditions.checkNotNull(executor, "executor cannot be null");
    Preconditions.checkNotNull(
        endpointHealthCheckService, "endpointHealthCheckService cannot be null");
    Preconditions.checkArgument(!started.get(), "executor cannot be true");

    this.executor.scheduleWithFixedDelay(
        this::neverFailsCheckForUpdates, updateInterval, updateInterval, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  void setTestUpdateInterval(long testUpdateInterval) {
    updateInterval = testUpdateInterval;
  }

  private void neverFailsCheckForUpdates() {
    try {
      checkForUpdates();
    } catch (Exception e) {
      state = HealthCheckState.READY;
      log.error(
          "SimpleHealthCheckDaemon checkForUpdates method ran into a problem: " + e.toString());
    }
  }

  /**
   * This method will be called periodically and if we are currently in the process of doing a
   * previous checkForUpdates, we will just skip this frame and try again at the next interval
   */
  private void checkForUpdates() {
    // don't process if we are still waiting for a health check round to finish
    if (state == HealthCheckState.READY) {
      state = HealthCheckState.IN_PROGRESS;
      Map<String, ImmutableSet<String>> healthyMap = new HashMap<String, ImmutableSet<String>>();
      // grab the candidate ip map from the supplier
      ImmutableList<DynamicRouteConfig> candidateList = healthCheckCandidateSupplier.get();
      for (DynamicRouteConfig routeConfig : candidateList) {
        String path = routeConfig.getPath();
        ImmutableList<DynamicClientConfig> candidateClientConfigs =
            ImmutableList.copyOf(routeConfig.getClientConfigs());
        ImmutableSet<String> healthyIpAddresses =
            endpointHealthCheckService.checkEndpoints(path, candidateClientConfigs);
        healthyMap.put(path, healthyIpAddresses);
      }

      // let the updater know there is a new mapping, it is the responsibility of the delegate
      // to decide if this new update is different
      ImmutableMap<String, ImmutableSet<String>> updatedHealthyMap =
          ImmutableMap.copyOf(healthyMap);
      healthyHostListUpdater.accept(updatedHealthyMap);
      state = HealthCheckState.READY;
    }
  }
}
