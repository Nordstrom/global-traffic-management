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
import com.xjeffrose.xio.config.HealthCheckConfig;
import java.util.ArrayList;

public class HealthCheckRouteConfigFactory {
  private final String ipAddress;
  private final int port;
  private final boolean tlsEnabled;
  private final String path;

  public HealthCheckRouteConfigFactory() {
    this(
        HealthCheckConfig.healthCheckHost,
        HealthCheckConfig.healthCheckPort,
        HealthCheckConfig.healthCheckTlsEnabled,
        HealthCheckConfig.healthCheckPath);
  }

  public HealthCheckRouteConfigFactory(
      String ipAddress, int port, boolean tlsEnabled, String path) {
    this.ipAddress = ipAddress;
    this.port = port;
    this.tlsEnabled = tlsEnabled;
    this.path = path;
  }

  public DynamicRouteConfig buildHealthCheckRouteConfig() {
    ArrayList<DynamicClientConfig> healthChecksClientConfigs = new ArrayList<>();
    // This healthcheck path is a proxy to the healthCheck side car
    DynamicClientConfig healthCheckClientConfig =
        new DynamicClientConfig(ipAddress, port, tlsEnabled, null);
    healthChecksClientConfigs.add(healthCheckClientConfig);
    DynamicRouteConfig healthCheckRouteConfig =
        new DynamicRouteConfig(path, healthChecksClientConfigs);
    return healthCheckRouteConfig;
  }
}
