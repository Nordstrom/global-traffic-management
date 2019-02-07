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

import com.xjeffrose.xio.zookeeper.AwsDeployment;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    GatekeeperServiceLocator serviceLocator = new GatekeeperServiceLocator();
    GatekeeperServer gatekeeperServer = serviceLocator.getGatekeeperServer();
    gatekeeperServer.start();
    if (!"1".equals(System.getProperty("LOCAL_DEV"))) {
      AwsDeployment deployment = serviceLocator.getAwsDeployment();
      deployment.start();
    }
    gatekeeperServer.blockUntilShutdown();
  }
}
