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
package com.nordstrom.gatekeeper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import com.xjeffrose.xio.zookeeper.AwsDeploymentConfig;
import lombok.Getter;

@Getter
public class GatekeeperConfig {

  private final TlsConfig tlsConfig;
  private final int port;
  private final AwsDeploymentConfig awsDeploymentConfig;

  public GatekeeperConfig() {
    this(ConfigFactory.load("gatekeeper.conf"));
  }

  public GatekeeperConfig(Config config) {
    Config gatekeeperConfig = config.getConfig("gatekeeper");
    port = gatekeeperConfig.getInt("port");
    tlsConfig = TlsConfig.builderFrom(gatekeeperConfig.getConfig("tls")).build();
    awsDeploymentConfig = new AwsDeploymentConfig(gatekeeperConfig.getConfig("awsDeployment"));
  }
}
