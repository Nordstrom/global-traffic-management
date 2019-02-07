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
import com.xjeffrose.xio.SSL.TlsConfig;

public class CoreDatabaseConfig {
  private final String host;
  private final int port;
  private final TlsConfig tlsConfig;

  public CoreDatabaseConfig(Config config) {
    this.host = config.getString("host");
    this.port = config.getInt("port");
    this.tlsConfig = TlsConfig.fromConfig("tls", config);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public TlsConfig getTlsConfig() {
    return tlsConfig;
  }
}
