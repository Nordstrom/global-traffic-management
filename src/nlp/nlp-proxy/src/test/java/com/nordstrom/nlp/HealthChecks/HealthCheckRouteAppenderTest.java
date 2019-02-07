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

import com.xjeffrose.xio.config.DynamicClientConfig;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class HealthCheckRouteAppenderTest extends Assert {
  @Test
  public void testAppend() throws Exception {
    ArrayList<DynamicClientConfig> existingClientConfigList = new ArrayList<>();
    DynamicClientConfig existingClientConfig =
        new DynamicClientConfig("127.0.0.1", 8000, false, null);
    existingClientConfigList.add(existingClientConfig);

    DynamicRouteConfig healthCheckRouteConfig =
        new DynamicRouteConfig("/existing/", existingClientConfigList);
    ArrayList<DynamicRouteConfig> existingList = new ArrayList<>();
    existingList.add(healthCheckRouteConfig);

    HealthCheckRouteConfigFactory factory = new HealthCheckRouteConfigFactory();
    List<DynamicRouteConfig> results =
        new HealthCheckRouteAppender(factory).appendHealthCheck(existingList);

    DynamicRouteConfig expectedHealthCheckRouteConfig = factory.buildHealthCheckRouteConfig();

    assertEquals(2, results.size());
    assertTrue(results.contains(healthCheckRouteConfig));
    assertTrue(results.contains(expectedHealthCheckRouteConfig));
  }
}
