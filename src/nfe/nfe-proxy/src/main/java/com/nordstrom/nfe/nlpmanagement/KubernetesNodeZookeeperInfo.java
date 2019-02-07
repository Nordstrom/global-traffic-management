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

import com.nordstrom.gtm.kubernetesdeployment.NodeInfo;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * This object is used to serialize and deserialize Json (using Jackson) to store in zookeeper.
 * KubernetesNlp is auto-generated code that also holds this information, but unfortunately does not
 * work with Jackson.
 */
@Getter
public class KubernetesNodeZookeeperInfo {
  private final String region;
  private final String clusterId;
  private final String nodeId;
  private final String nodeIpAddress;
  private final List<KubernetesServiceZookeeperInfo> kubernetesServiceZookeeperInfos;

  public KubernetesNodeZookeeperInfo(
      String region,
      String clusterId,
      String nodeId,
      String nodeIpAddress,
      List<KubernetesServiceZookeeperInfo> kubernetesServiceZookeeperInfos) {
    this.region = region;
    this.clusterId = clusterId;
    this.nodeId = nodeId;
    this.nodeIpAddress = nodeIpAddress;
    this.kubernetesServiceZookeeperInfos = kubernetesServiceZookeeperInfos;
  }

  public KubernetesNodeZookeeperInfo(NodeInfo nodeInfo) {
    this.region = nodeInfo.getRegion();
    this.clusterId = nodeInfo.getClusterId();
    this.nodeId = nodeInfo.getNodeId();
    this.nodeIpAddress = nodeInfo.getNodeIpAddress();
    this.kubernetesServiceZookeeperInfos =
        nodeInfo
            .getServiceInfoList()
            .stream()
            .map(KubernetesServiceZookeeperInfo::new)
            .collect(Collectors.toList());
  }

  // This initializer is required for jackson
  KubernetesNodeZookeeperInfo() {
    this.region = "";
    this.clusterId = "";
    this.nodeId = "";
    this.nodeIpAddress = "";
    this.kubernetesServiceZookeeperInfos = Collections.emptyList();
  }
}
