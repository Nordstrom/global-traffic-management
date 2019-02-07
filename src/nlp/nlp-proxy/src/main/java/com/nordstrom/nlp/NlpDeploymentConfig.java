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
package com.nordstrom.nlp;

import com.typesafe.config.Config;

public class NlpDeploymentConfig {
  private static final String AWS_IP_ADDRESS_PATH = "/latest/meta-data/public-ipv4";
  private static final String AWS_INSTANCE_IDENTITY_PATH =
      "/latest/dynamic/instance-identity/document";

  private final String host;
  private final int port;
  private final String awsIpAddressPath;
  private final String awsInstanceIdentityPath;

  NlpDeploymentConfig(Config config) {
    this.host = config.getString("host");
    this.port = config.getInt("port");

    String awsInfoIpAddress = config.getString("awsInfoUri");
    this.awsIpAddressPath = awsInfoIpAddress + AWS_IP_ADDRESS_PATH;
    this.awsInstanceIdentityPath = awsInfoIpAddress + AWS_INSTANCE_IDENTITY_PATH;
  }

  String getHost() {
    return host;
  }

  int getPort() {
    return port;
  }

  String getAwsIpAddressPath() {
    return awsIpAddressPath;
  }

  String getAwsInstanceIdentityPath() {
    return awsInstanceIdentityPath;
  }
}
