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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nordstrom.nfe.config.NfeConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.http.RouteState;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(fluent = true)
@Getter
@Slf4j
public class NfeState extends ApplicationState {
  private AtomicReference<ImmutableMap<String, RouteState>> routesRef =
      new AtomicReference<>(ImmutableMap.of());

  private final ImmutableList<String> allPermissions;

  public NfeState(NfeConfig appConfig) {
    super(appConfig);
    this.allPermissions = appConfig.routeConfig().permissionsNeeded().asList();
  }

  public ImmutableMap<String, RouteState> getRoutes() {
    return routesRef.get();
  }

  public void setRoutes(ImmutableMap<String, RouteState> routes) {
    this.routesRef.set(routes);
  }
}
