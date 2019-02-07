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
package com.nordstrom.nfe.config;

import com.typesafe.config.Config;

public class GatekeeperConfig {

  public final String host;
  public final int port;

  public GatekeeperConfig(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public static GatekeeperConfig fromConfig(Config config) {
    String host = config.getString("host");
    int port = config.getInt("port");

    return new GatekeeperConfig(host, port);
  }

  public static GatekeeperConfig fromConfig(String key, Config config) {
    return fromConfig(config.getConfig(key));
  }
}
