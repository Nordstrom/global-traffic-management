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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nordstrom.nfe.CoreDataService;
import com.nordstrom.nfe.RouteStates;
import com.xjeffrose.xio.core.ZkClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

@Slf4j
public class CustomerAccountNlpDeploymentWatcher {
  private RouteStates routeStates;
  private final ZkClient zkClient;
  private final ObjectMapper objectMapper;
  private final CoreDataService coreDataService;

  private boolean zkClientHasInitialized = false;

  public CustomerAccountNlpDeploymentWatcher(
      RouteStates routeStates,
      ZkClient zkClient,
      ObjectMapper objectMapper,
      CoreDataService coreDataService) {
    this.routeStates = routeStates;
    this.zkClient = zkClient;
    this.objectMapper = objectMapper;
    this.coreDataService = coreDataService;
  }

  public void start() {
    zkClient.registerForTreeNodeEvents(
        CustomerAccountNlpDeploymentGrpcService.CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH,
        treeCacheEvent -> {
          switch (treeCacheEvent.getType()) {
            case INITIALIZED:
              handleInitialized();
              break;
            case NODE_ADDED:
              handleNodeAdded(treeCacheEvent);
              break;
            case NODE_REMOVED:
              handleNodeRemoved(treeCacheEvent);
              break;
            case NODE_UPDATED:
              handleNodeUpdated();
              break;
            case CONNECTION_SUSPENDED:
              log.debug("zookeeper sent 'CONNECTION_SUSPENDED: " + treeCacheEvent.toString());
              break;
            case CONNECTION_RECONNECTED:
              log.debug("zookeeper sent 'CONNECTION_RECONNECTED: " + treeCacheEvent.toString());
              break;
            case CONNECTION_LOST:
              log.debug("zookeeper sent 'CONNECTION_LOST: " + treeCacheEvent.toString());
              break;
          }
        });
  }

  private void handleInitialized() {
    zkClientHasInitialized = true;
    String baseUrl = CustomerAccountNlpDeploymentGrpcService.CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH;

    List<String> childPaths = new ArrayList<>();
    for (String accountId :
        ZookeeperHelpers.safeGetChildren(zkClient, ZookeeperHelpers.pathFromComponents(baseUrl))) {
      for (String instanceId :
          ZookeeperHelpers.safeGetChildren(
              zkClient, ZookeeperHelpers.pathFromComponents(baseUrl, accountId))) {
        childPaths.add(ZookeeperHelpers.pathFromComponents(baseUrl, accountId, instanceId));
      }
    }

    List<CustomerAccountNlpZookeeperInfo> customerAccountNlpZookeeperInfos =
        childPaths
            .stream()
            .map(
                (info) ->
                    ZookeeperHelpers.infoAtZookeeperPath(
                        zkClient, objectMapper, info, CustomerAccountNlpZookeeperInfo.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    bulkAddNlps(customerAccountNlpZookeeperInfos);
  }

  private void handleNodeAdded(TreeCacheEvent treeCacheEvent) {
    if (!zkClientHasInitialized) {
      return;
    }

    String zkPath = treeCacheEvent.getData().getPath();
    boolean hasValidPath = isNlpZookeeperChildNodePath(zkPath);

    if (hasValidPath) {
      CustomerAccountNlpZookeeperInfo customerAccountNlpZookeeperInfo;
      try {
        customerAccountNlpZookeeperInfo =
            objectMapper.readValue(
                treeCacheEvent.getData().getData(), CustomerAccountNlpZookeeperInfo.class);
      } catch (IOException e) {
        log.error("Unable to get deployment info for NLP (zkNode added): ", e);
        return;
      }

      bulkAddNlps(Collections.singletonList(customerAccountNlpZookeeperInfo));
    }
  }

  private void handleNodeRemoved(TreeCacheEvent treeCacheEvent) {
    if (!zkClientHasInitialized) {
      return;
    }

    String zkPath = treeCacheEvent.getData().getPath();
    boolean hasValidPath = isNlpZookeeperChildNodePath(zkPath);

    if (hasValidPath) {
      CustomerAccountNlpZookeeperInfo info;
      try {
        info =
            objectMapper.readValue(
                treeCacheEvent.getData().getData(), CustomerAccountNlpZookeeperInfo.class);
      } catch (IOException e) {
        log.error("Unable to get deployment info for NLP (node removed): ", e);
        return;
      }

      routeStates.removeCustomerAccountNlpInstance(info.getAccountId(), info.getIpAddress());
    }
  }

  private void handleNodeUpdated() {
    if (!zkClientHasInitialized) {
      return;
    }

    // TODO(br): do some updating
  }

  private boolean isNlpZookeeperChildNodePath(String path) {
    List<String> segments =
        Arrays.stream(path.split("/"))
            .filter(segment -> !segment.isEmpty())
            .collect(Collectors.toList());

    // "/nlps/aws/accountId/instanceId" => 4 expected components
    return segments.size() == 4
        && path.startsWith(
            CustomerAccountNlpDeploymentGrpcService.CUSTOMER_ACCOUNT_NLP_AWS_BASE_PATH);
  }

  private void bulkAddNlps(List<CustomerAccountNlpZookeeperInfo> customerAccountNlpZookeeperInfos) {
    routeStates.addCustomerAccountNlpInstances(
        () -> {
          // Get the list of unique account Ids.
          List<String> uniqueAccountIds =
              customerAccountNlpZookeeperInfos
                  .stream()
                  .map(CustomerAccountNlpZookeeperInfo::getAccountId)
                  .distinct()
                  .collect(Collectors.toList());

          // Fetch route paths for each account.
          Map<String, List<String>> fetchedRoutePathsMap =
              coreDataService.getCustomerAccountNlpRoutePaths(uniqueAccountIds);

          // Construct the new NLP instance entries.
          List<NlpInstanceEntry> nlpInstanceEntries = new ArrayList<>();
          for (CustomerAccountNlpZookeeperInfo customerAccountNlpZookeeperInfo :
              customerAccountNlpZookeeperInfos) {
            if (fetchedRoutePathsMap.containsKey(customerAccountNlpZookeeperInfo.getAccountId())) {
              List<String> routePaths =
                  fetchedRoutePathsMap.get(customerAccountNlpZookeeperInfo.getAccountId());
              NlpInstanceEntry entry =
                  new NlpInstanceEntry(
                      customerAccountNlpZookeeperInfo.getAccountId(),
                      customerAccountNlpZookeeperInfo.getIpAddress(),
                      routePaths);
              nlpInstanceEntries.add(entry);
            }
          }

          return nlpInstanceEntries;
        });
  }
}
