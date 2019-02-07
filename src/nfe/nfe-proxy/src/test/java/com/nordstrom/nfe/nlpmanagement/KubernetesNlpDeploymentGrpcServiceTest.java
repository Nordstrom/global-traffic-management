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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.kubernetesdeployment.NodeInfo;
import com.nordstrom.nfe.testhelpers.GrpcTestHelpers;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import org.junit.Before;
import org.junit.Test;

public class KubernetesNlpDeploymentGrpcServiceTest {
  private KubernetesNlpDeploymentGrpcService subject;
  private ZkClient zkClient;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void beforeEach() {
    zkClient = mock(ZkClient.class);
    subject = new KubernetesNlpDeploymentGrpcService(zkClient, objectMapper);
  }

  @Test
  public void testRouteCount() {
    assertEquals(1, subject.getRoutes().size());
  }

  @Test
  public void testPackageName() {
    assertEquals("nordstrom.gtm.kubernetesdeployment", subject.getPackageName());
  }

  @Test
  public void testServiceName() {
    assertEquals("KubernetesDeployment", subject.getServiceName());
  }

  @Test
  public void testDeployedPodRoute() throws Exception {
    GrpcRoute route = GrpcTestHelpers.findRoute(subject.getRoutes(), "UpdateNodeInfo");

    // test basic info
    assertEquals("UpdateNodeInfo", route.methodName);
    assertEquals(subject, route.service);

    // test handler
    GrpcRequestHandler<NodeInfo, Empty> handler = route.handler;
    NodeInfo request =
        NodeInfo.newBuilder()
            .setRegion("region")
            .setClusterId("cluster-id")
            .setNodeId("node-id")
            .setNodeIpAddress("node-ip-address")
            .build();

    Empty response = handler.getAppLogic().apply(request);

    // should update zookeeper
    String expectedPath =
        "/nlps/k8s/"
            + request.getRegion()
            + "/"
            + request.getClusterId()
            + "/"
            + request.getNodeId();
    KubernetesNodeZookeeperInfo kubernetesNodeZookeeperInfo =
        new KubernetesNodeZookeeperInfo(request);
    String expectedJson = objectMapper.writeValueAsString(kubernetesNodeZookeeperInfo);
    verify(zkClient, times(1)).set(expectedPath, expectedJson);

    // should return a response (currently the response has no associated data)
    assertNotNull(response);
  }
}
