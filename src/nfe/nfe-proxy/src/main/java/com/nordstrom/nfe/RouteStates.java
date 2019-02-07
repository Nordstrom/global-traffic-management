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
package com.nordstrom.nfe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.nlpmanagement.AccountInfo;
import com.nordstrom.nfe.nlpmanagement.KubernetesNodeInfo;
import com.nordstrom.nfe.nlpmanagement.NlpInstanceEntry;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.*;
import io.netty.handler.codec.http.HttpMethod;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RouteStates {
  private final NfeState nfeState;
  private final NfeConfig nfeConfig;
  private final ProxyClientFactory proxyClientFactory;
  private final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
  private final AtomicReference<ImmutableMap<String, AccountInfo>>
      customerAccountNlpInstanceMap; // key is account id
  private final AtomicReference<ImmutableMap<String, KubernetesNodeInfo>>
      kubernetesNlpInstanceMap; // key is node's unique id: (region, clusterId, nodeId)
  private List<RouteState> grpcRouteStates = Collections.emptyList();
  private List<RouteState> configRouteStates = Collections.emptyList();

  public RouteStates(
      NfeState nfeState, NfeConfig nfeConfig, ProxyClientFactory proxyClientFactory) {

    this.nfeState = nfeState;
    this.nfeConfig = nfeConfig;
    this.proxyClientFactory = proxyClientFactory;
    this.customerAccountNlpInstanceMap = new AtomicReference<>(ImmutableBiMap.of());
    this.kubernetesNlpInstanceMap = new AtomicReference<>(ImmutableMap.of());

    new Thread(this::startQueue).start();
  }

  @VisibleForTesting
  ImmutableMap<String, AccountInfo> getCustomerAccountNlpMap() {
    return customerAccountNlpInstanceMap.get();
  }

  @VisibleForTesting
  ImmutableMap<String, KubernetesNodeInfo> getK8sNlpMap() {
    return kubernetesNlpInstanceMap.get();
  }
  /**
   * Builds the initial routes. The initial routes include all the routes in the configuration
   * files. The initial routes include all the routes from the GrpcServices.
   */
  public void buildInitialRoutes(List<GrpcService> grpcServices) {
    addToQueue(
        () -> {
          this.grpcRouteStates = buildGrpcRouteStates(grpcServices);
          this.configRouteStates = buildConfigRouteStates();

          updateRouteMap();
        });
  }

  /**
   * Adds the new customer account NLPs to the list of known NLPs. Recomputes all the routes for all
   * NLPs based on newly updated NLP list.
   */
  public void addCustomerAccountNlpInstances(
      Supplier<List<NlpInstanceEntry>> nlpInstanceEntriesSupplier) {
    addToQueue(
        () -> {
          List<NlpInstanceEntry> nlpInstanceEntries = nlpInstanceEntriesSupplier.get();

          if (nlpInstanceEntries.isEmpty()) {
            return;
          }

          Map<String, AccountInfo> combinedNlpInstanceMap =
              new LinkedHashMap<>(this.customerAccountNlpInstanceMap.get());

          for (NlpInstanceEntry nlpInstanceEntry : nlpInstanceEntries) {
            AccountInfo originalAccountInfo =
                combinedNlpInstanceMap.get(nlpInstanceEntry.getAccountId());
            AccountInfo updatedAccountInfo;

            if (originalAccountInfo != null) {
              List<String> combinedIpAddresses =
                  new ArrayList<>(originalAccountInfo.getIpAddresses());
              combinedIpAddresses.add(nlpInstanceEntry.getIpAddress());
              updatedAccountInfo =
                  new AccountInfo(combinedIpAddresses, nlpInstanceEntry.getPaths());
            } else {
              updatedAccountInfo =
                  new AccountInfo(
                      Collections.singletonList(nlpInstanceEntry.getIpAddress()),
                      nlpInstanceEntry.getPaths());
            }

            combinedNlpInstanceMap.put(nlpInstanceEntry.getAccountId(), updatedAccountInfo);
          }

          customerAccountNlpInstanceMap.set(ImmutableMap.copyOf(combinedNlpInstanceMap));

          // Now that the customer account NLPs map is updated, we need to update the route map.
          updateRouteMap();
        });
  }

  /**
   * Removes the customer account NLP with the provided information from the list of known NLPs.
   * Recomputes all the routes for all NLPs based on newly updated NLP list.
   *
   * <p>NOTE: If an NLP with the provided information is not found then the map of routes is not
   * recomputed.
   */
  public void removeCustomerAccountNlpInstance(String accountId, String ipAddress) {
    addToQueue(
        () -> {
          Map<String, AccountInfo> nlpInstanceMap =
              new HashMap<>(this.customerAccountNlpInstanceMap.get());
          AccountInfo accountInfo = nlpInstanceMap.get(accountId);

          if (accountInfo == null || !accountInfo.getIpAddresses().contains(ipAddress)) {
            return;
          }

          if (accountInfo.getIpAddresses().size() == 1) {
            nlpInstanceMap.remove(accountId);
          } else {
            accountInfo = accountInfo.deepCopy();
            accountInfo.getIpAddresses().remove(ipAddress);
            nlpInstanceMap.put(accountId, accountInfo);
          }

          this.customerAccountNlpInstanceMap.set(ImmutableMap.copyOf(nlpInstanceMap));
          updateRouteMap();
        });
  }

  /**
   * Used to manually update the customer account NLP map.
   *
   * @param nlpInstanceMapUpdater The function that is used to update the NLP map. Provides the
   *     current customer account NLP map. The returned map is used as the new NLP map.
   */
  public void updateCustomerAccountNlpInstanceMap(
      Function<ImmutableMap<String, AccountInfo>, Map<String, AccountInfo>> nlpInstanceMapUpdater) {
    addToQueue(
        () -> {
          Map<String, AccountInfo> newNlpInstanceMap =
              nlpInstanceMapUpdater.apply(customerAccountNlpInstanceMap.get());
          customerAccountNlpInstanceMap.set(ImmutableMap.copyOf(newNlpInstanceMap));
          updateRouteMap();
        });
  }

  /**
   * Adds the new kubernetes NLPs to the list of known NLPs. If a new KubernetesNodeInfo is provided
   * that has the same uniqueId then it will override the old one. Recomputes all the routes for all
   * NLPs based on newly updated NLP list.
   */
  public void addKubernetesNlpInstances(
      Supplier<List<KubernetesNodeInfo>> kubernetesNodeInfosSupplier) {
    addToQueue(
        () -> {
          List<KubernetesNodeInfo> kubernetesNodeInfos = kubernetesNodeInfosSupplier.get();

          if (kubernetesNodeInfos.isEmpty()) {
            return;
          }

          Map<String, KubernetesNodeInfo> combinedKubernetesNodeInfos =
              new LinkedHashMap<>(this.kubernetesNlpInstanceMap.get());

          for (KubernetesNodeInfo kubernetesNodeInfo : kubernetesNodeInfos) {
            combinedKubernetesNodeInfos.put(kubernetesNodeInfo.getUniqueId(), kubernetesNodeInfo);
          }

          kubernetesNlpInstanceMap.set(ImmutableMap.copyOf(combinedKubernetesNodeInfos));

          // Now that the kubernetes NLPs map is updated, we need to update the route map.
          updateRouteMap();
        });
  }

  /**
   * Removes the kubernetes NLP with the provided information from the list of known NLPs.
   * Recomputes all the routes for all NLPs based on newly updated NLP list.
   *
   * <p>NOTE: If an NLP with the provided information is not found then the map of routes is not
   * recomputed.
   */
  public void removeKubernetesNlpInstance(String uniqueId) {
    addToQueue(
        () -> {
          Map<String, KubernetesNodeInfo> nlpInstanceMap =
              new HashMap<>(this.kubernetesNlpInstanceMap.get());
          if (nlpInstanceMap.containsKey(uniqueId)) {
            nlpInstanceMap.remove(uniqueId);
            this.kubernetesNlpInstanceMap.set(ImmutableMap.copyOf(nlpInstanceMap));

            updateRouteMap();
          }
        });
  }

  /**
   * Recreates the route map.
   *
   * <p>Uses the already created routes from the config file and gRPC services. Also, rebuilds the
   * routes from the account info map.
   *
   * <p>Routes built from account info map will override config file routes. It can potentially
   * override gRPC routes, but these should not conflict as the route path format is different.
   */
  private void updateRouteMap() {
    List<RouteState> combinedRouteStates = new ArrayList<>();
    combinedRouteStates.addAll(configRouteStates);
    combinedRouteStates.addAll(grpcRouteStates);
    combinedRouteStates.addAll(buildCustomerAccountNlpRouteStates());
    combinedRouteStates.addAll(buildKubernetesNlpRouteStates());

    LinkedHashMap<String, RouteState> routeMap = new LinkedHashMap<>();
    combinedRouteStates.forEach(
        (RouteState state) -> {
          // put this state into the routeMap with the path as the key
          routeMap.put(state.path(), state);
        });

    nfeState.setRoutes(ImmutableMap.copyOf(routeMap));
  }

  private List<RouteState> buildConfigRouteStates() {
    return nfeConfig
        .proxyRoutes()
        // iterate over a stream of ProxyRouteConfig
        .stream()
        // for each ProxyRouteConfig create a ProxyRouteState
        .map(this::buildProxyRouteState)
        // collect into a List<ProxyRouteState>
        .collect(Collectors.toList());
  }

  private ProxyRouteState buildProxyRouteState(ProxyRouteConfig config) {
    return new ProxyRouteState(
        nfeState,
        config,
        new PersistentProxyHandler(proxyClientFactory, config, new SocketAddressHelper()));
  }

  private List<RouteState> buildGrpcRouteStates(List<GrpcService> grpcServices) {
    List<RouteState> routeStates = Lists.newArrayList();

    for (GrpcService service : grpcServices) {
      for (GrpcRoute route : service.getRoutes()) {
        List<HttpMethod> methods = Collections.singletonList(HttpMethod.POST);
        String host = "";
        // todo: (WK) grpc routes need to use mutual auth with a sane permissionNeeded s- this is
        // dangerous
        log.error(
            "service {} is using a dangerous 'permissionNeeded=None' {}.{}",
            route.service.getPackageName(),
            route.service.getServiceName());
        String permissionNeeded = "none";

        RouteConfig config = new RouteConfig(methods, host, route.buildPath(), permissionNeeded);
        routeStates.add(new RouteState(config, route.handler));
      }
    }

    return routeStates;
  }

  private List<RouteState> buildCustomerAccountNlpRouteStates() {
    Map<String, AccountInfo> nlpInstanceMap = this.customerAccountNlpInstanceMap.get();
    List<RouteState> routeStates = new ArrayList<>();

    for (String accountId : nlpInstanceMap.keySet()) {
      AccountInfo accountInfo = nlpInstanceMap.get(accountId);

      for (String path : accountInfo.getPaths()) {
        List<String> pathComponents = Arrays.asList(path.split("/"));
        String serviceName = pathComponents.get(pathComponents.size() - 1);
        routeStates.add(
            buildNlpRouteState(
                path,
                serviceName,
                accountInfo.getIpAddresses(),
                nfeConfig.defaultClientConfig().remote().getPort()));
      }
    }

    return routeStates;
  }

  private List<RouteState> buildKubernetesNlpRouteStates() {
    Map<String, KubernetesNodeInfo> nodeIdToKubernetesNlpInstanceMap =
        this.kubernetesNlpInstanceMap.get();
    List<RouteState> routeStates = new ArrayList<>();

    // Reverse map the K8S nodes to group them by path
    Map<String, List<KubernetesNodeInfo>> pathToKubernetesNlpInstancesMap = new LinkedHashMap<>();
    for (String nodeId : nodeIdToKubernetesNlpInstanceMap.keySet()) {
      KubernetesNodeInfo kubernetesNodeInfo = nodeIdToKubernetesNlpInstanceMap.get(nodeId);

      for (String path : kubernetesNodeInfo.getPaths()) {
        List<KubernetesNodeInfo> newInfoList;

        if (pathToKubernetesNlpInstancesMap.get(path) == null) {
          newInfoList = new ArrayList<>();
        } else {
          newInfoList = pathToKubernetesNlpInstancesMap.get(path);
        }

        newInfoList.add(kubernetesNodeInfo);
        pathToKubernetesNlpInstancesMap.put(path, newInfoList);
      }
    }

    // Create a RouteState per path, with a Client per K8S node
    for (String path : pathToKubernetesNlpInstancesMap.keySet()) {
      List<KubernetesNodeInfo> kubernetesNodeInfos = pathToKubernetesNlpInstancesMap.get(path);
      List<String> pathComponents = Arrays.asList(path.split("/"));
      String serviceName = pathComponents.get(pathComponents.size() - 1);

      List<String> ipAddresses =
          kubernetesNodeInfos
              .stream()
              .map(KubernetesNodeInfo::getIpAddress)
              .collect(Collectors.toList());

      // TODO(br): use correct port (should be the port reserved by K8S for NLPs)
      routeStates.add(
          buildNlpRouteState(
              path,
              serviceName,
              ipAddresses,
              nfeConfig.kubernetesRoutingConfig().getReservedNlpPort()));
    }

    return routeStates;
  }

  private ProxyRouteState buildNlpRouteState(
      String path, String serviceName, List<String> ipAddresses, int port) {
    List<ClientConfig> clientConfigs = new ArrayList<>();

    for (String ipAddress : ipAddresses) {
      clientConfigs.add(
          ClientConfig.newBuilder(nfeConfig.defaultClientConfig())
              .setRemote(new InetSocketAddress(ipAddress, port))
              .build());
    }

    ProxyRouteConfig proxyRouteConfig =
        ProxyRouteConfig.newBuilder(nfeConfig.defaultProxyRouteConfig())
            .setPath(path)
            .setProxyPath("/" + serviceName + "/")
            .setClientConfigs(clientConfigs)
            .build();

    return buildProxyRouteState(proxyRouteConfig);
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

  @VisibleForTesting
  public void addToQueue(Runnable runnable) {
    try {
      blockingQueue.put(runnable);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
