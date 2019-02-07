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
package com.nordstrom.nfe.nlpmanagement;

import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.RouteStates;
import com.nordstrom.nfe.config.NlpSharedCountConfig;
import com.xjeffrose.xio.core.ZkClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.framework.recipes.shared.SharedCountListener;
import org.apache.curator.framework.recipes.shared.SharedCountReader;
import org.apache.curator.framework.recipes.shared.VersionedValue;
import org.apache.curator.framework.state.ConnectionState;

@Slf4j
public class ServiceRegistrationUpdatedWatcher {
  private static final String SERVICE_REGISTRATION_UPDATED_PATH =
      "/service_registration/updated_counter";

  private final RouteStates routeStates;
  private final ZkClient zkClient;
  private final CoreDataService coreDataService;
  private final NlpSharedCountConfig nlpSharedCountConfig;
  private SharedCount sharedCount;

  public ServiceRegistrationUpdatedWatcher(
      RouteStates routeStates,
      ZkClient zkClient,
      CoreDataService coreDataService,
      NlpSharedCountConfig nlpSharedCountConfig) {
    this.routeStates = routeStates;
    this.zkClient = zkClient;
    this.coreDataService = coreDataService;
    this.nlpSharedCountConfig = nlpSharedCountConfig;
  }

  public void start() {
    sharedCount = zkClient.createSharedCounter(SERVICE_REGISTRATION_UPDATED_PATH, 0);
    sharedCount.addListener(listener());

    boolean successful = false;
    int attempts = 0;

    while (!successful && attempts < nlpSharedCountConfig.attemptsToStartMax) {
      attempts++;

      try {
        sharedCount.start();
        successful = true;
      } catch (Exception e) {
        // TODO(br): figure out what to do in those scenarios
        log.error("Something went wrong trying to start the SharedCount: ", e);
      }
    }

    if (!successful) {
      log.error(
          "Unable to start the zookeeper shared count. This counter is used to watch/post notifications to service registration updates.");
    }
  }

  public void postServiceRegistrationDataUpdated() {
    boolean successful = false;
    int attempts = 0;

    while (!successful && attempts < nlpSharedCountConfig.attemptsToUpdateMax) {
      attempts++;

      try {
        VersionedValue<Integer> previousValue = sharedCount.getVersionedValue();
        successful = sharedCount.trySetCount(previousValue, previousValue.getValue() + 1);
        log.info("ServiceRegistration Data Updated successfully");
      } catch (Exception e) {
        // TODO(br): figure out what scenarios throw exceptions, and what to do in those scenarios
        log.error("Something went wrong trying to update the SharedCount: ", e);
      }
    }
  }

  private SharedCountListener listener() {
    return new SharedCountListener() {
      @Override
      public void countHasChanged(SharedCountReader sharedCountReader, int i) throws Exception {
        handleServiceRegistrationChanged();
      }

      @Override
      public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        log.info("Zookeeper SharedCount's state changed: ", connectionState);
      }
    };
  }

  private void handleServiceRegistrationChanged() {
    routeStates.updateCustomerAccountNlpInstanceMap(
        originalNlpInstanceMap -> {
          List<String> accountIds = new ArrayList<>(originalNlpInstanceMap.keySet());
          Map<String, List<String>> fetchedRoutePathsMap =
              coreDataService.getCustomerAccountNlpRoutePaths(accountIds);

          Map<String, AccountInfo> nlpInstanceMap = new HashMap<>();

          for (String accountId : fetchedRoutePathsMap.keySet()) {
            AccountInfo originalAccountInfo = originalNlpInstanceMap.get(accountId);
            AccountInfo newAccountInfo =
                new AccountInfo(
                    originalAccountInfo.getIpAddresses(), fetchedRoutePathsMap.get(accountId));
            nlpInstanceMap.put(accountId, newAccountInfo);
          }

          log.info("ServiceRegistrationUpdated with new instance mapping: " + nlpInstanceMap);
          return nlpInstanceMap;
        });
  }
}
