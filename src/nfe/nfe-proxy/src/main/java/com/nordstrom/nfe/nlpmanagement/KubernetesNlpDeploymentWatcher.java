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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

@Slf4j
public class KubernetesNlpDeploymentWatcher {
  private final RouteStates routeStates;
  private final ZkClient zkClient;
  private final ObjectMapper objectMapper;
  private final CoreDataService coreDataService;

  private boolean zkClientHasInitialized = false;

  public KubernetesNlpDeploymentWatcher(
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
        KubernetesNlpDeploymentGrpcService.KUBERNETES_NLP_AWS_BASE_PATH,
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
              handleNodeUpdated(treeCacheEvent);
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
    String baseUrl = KubernetesNlpDeploymentGrpcService.KUBERNETES_NLP_AWS_BASE_PATH;

    List<String> childPaths = new ArrayList<>();
    for (String region :
        ZookeeperHelpers.safeGetChildren(zkClient, ZookeeperHelpers.pathFromComponents(baseUrl))) {
      for (String clusterId :
          ZookeeperHelpers.safeGetChildren(
              zkClient, ZookeeperHelpers.pathFromComponents(baseUrl, region))) {
        for (String nodeId :
            ZookeeperHelpers.safeGetChildren(
                zkClient, ZookeeperHelpers.pathFromComponents(baseUrl, region, clusterId))) {
          childPaths.add(ZookeeperHelpers.pathFromComponents(baseUrl, region, clusterId, nodeId));
        }
      }
    }

    List<KubernetesNodeZookeeperInfo> kubernetesNodeZookeeperInfos =
        childPaths
            .stream()
            .map(
                (info) ->
                    ZookeeperHelpers.infoAtZookeeperPath(
                        zkClient, objectMapper, info, KubernetesNodeZookeeperInfo.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    bulkAddNlps(kubernetesNodeZookeeperInfos);
  }

  private void handleNodeAdded(TreeCacheEvent treeCacheEvent) {
    if (!zkClientHasInitialized) {
      return;
    }

    String zkPath = treeCacheEvent.getData().getPath();
    boolean hasValidPath = isNlpZookeeperChildNodePath(zkPath);

    if (hasValidPath) {
      KubernetesNodeZookeeperInfo zkInfo;
      try {
        zkInfo =
            objectMapper.readValue(
                treeCacheEvent.getData().getData(), KubernetesNodeZookeeperInfo.class);
      } catch (IOException e) {
        log.error("Unable to get info for K8S Node (zkNode added): ", e);
        return;
      }

      bulkAddNlps(Collections.singletonList(zkInfo));
    }
  }

  private void handleNodeRemoved(TreeCacheEvent treeCacheEvent) {
    if (!zkClientHasInitialized) {
      return;
    }

    String zkPath = treeCacheEvent.getData().getPath();
    boolean hasValidPath = isNlpZookeeperChildNodePath(zkPath);

    if (hasValidPath) {
      KubernetesNodeZookeeperInfo zkInfo;
      try {
        zkInfo =
            objectMapper.readValue(
                treeCacheEvent.getData().getData(), KubernetesNodeZookeeperInfo.class);
      } catch (IOException e) {
        log.error("Unable to get info for K8S Node (zkNode deleted): ", e);
        return;
      }

      routeStates.removeKubernetesNlpInstance(uniqueIdForK8sNode(zkInfo));
    }
  }

  private void handleNodeUpdated(TreeCacheEvent treeCacheEvent) {
    if (!zkClientHasInitialized) {
      return;
    }

    String zkPath = treeCacheEvent.getData().getPath();
    boolean hasValidPath = isNlpZookeeperChildNodePath(zkPath);

    if (hasValidPath) {
      KubernetesNodeZookeeperInfo zkInfo;
      try {
        zkInfo =
            objectMapper.readValue(
                treeCacheEvent.getData().getData(), KubernetesNodeZookeeperInfo.class);
      } catch (IOException e) {
        log.error("Unable to get info for K8S Node (zkNode updated): ", e);
        return;
      }

      bulkAddNlps(Collections.singletonList(zkInfo));
    }
  }

  private void bulkAddNlps(List<KubernetesNodeZookeeperInfo> kubernetesNodeZookeeperInfos) {
    routeStates.addKubernetesNlpInstances(
        () -> {
          List<String> uniqueServiceNames =
              kubernetesNodeZookeeperInfos
                  .stream()
                  .flatMap((zkInfo) -> zkInfo.getKubernetesServiceZookeeperInfos().stream())
                  .map(KubernetesServiceZookeeperInfo::getName)
                  .distinct()
                  .collect(Collectors.toList());

          Map<String, String> fetchedServiceToPathMap;
          try {
            fetchedServiceToPathMap = coreDataService.getPathsForServices(uniqueServiceNames);
          } catch (InterruptedException | ExecutionException e) {
            log.error("Could not get paths for service names: ", e);
            return Collections.emptyList();
          }

          return kubernetesNodeZookeeperInfos
              .stream()
              .map((zkInfo) -> nodeInfoFromZookeeperInfo(zkInfo, fetchedServiceToPathMap))
              .collect(Collectors.toList());
        });
  }

  private KubernetesNodeInfo nodeInfoFromZookeeperInfo(
      KubernetesNodeZookeeperInfo zkInfo, Map<String, String> fetchedServiceToPathMap) {
    String uniqueId = uniqueIdForK8sNode(zkInfo);
    List<String> paths =
        zkInfo
            .getKubernetesServiceZookeeperInfos()
            .stream()
            .map(KubernetesServiceZookeeperInfo::getName)
            .map(fetchedServiceToPathMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return new KubernetesNodeInfo(uniqueId, zkInfo.getNodeIpAddress(), paths);
  }

  private String uniqueIdForK8sNode(KubernetesNodeZookeeperInfo zkInfo) {
    return String.join(
        "-", Arrays.asList(zkInfo.getRegion(), zkInfo.getClusterId(), zkInfo.getNodeId()));
  }

  private boolean isNlpZookeeperChildNodePath(String path) {
    List<String> segments =
        Arrays.stream(path.split("/"))
            .filter(segment -> !segment.isEmpty())
            .collect(Collectors.toList());

    // "/nlps/k8s/region/clusterId/nodeId" => 5 expected components
    return segments.size() == 5
        && path.startsWith(KubernetesNlpDeploymentGrpcService.KUBERNETES_NLP_AWS_BASE_PATH);
  }
}
