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

import com.nordstrom.nlp.ProxyRouteConfigFactory;
import com.nordstrom.nlp.SimpleProxyRouteConfigFactory;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import java.util.List;

public class HealthCheckAppenderProxyRouteConfigFactory implements ProxyRouteConfigFactory {
  private final HealthCheckRouteAppender healthCheckRouteAppender;
  private final SimpleProxyRouteConfigFactory simpleProxyRouteConfigFactory;

  public HealthCheckAppenderProxyRouteConfigFactory(
      SimpleProxyRouteConfigFactory simpleProxyRouteConfigFactory,
      HealthCheckRouteAppender healthCheckRouteAppender) {
    this.simpleProxyRouteConfigFactory = simpleProxyRouteConfigFactory;
    this.healthCheckRouteAppender = healthCheckRouteAppender;
  }

  public List<ProxyRouteConfig> build(List<DynamicRouteConfig> input) {
    List<DynamicRouteConfig> dynamicRouteConfigs = addHealthCheckToDynamicRouteConfigList(input);
    return simpleProxyRouteConfigFactory.build(dynamicRouteConfigs);
  }

  private List<DynamicRouteConfig> addHealthCheckToDynamicRouteConfigList(
      List<DynamicRouteConfig> input) {
    return healthCheckRouteAppender.appendHealthCheck(input);
  }
}
