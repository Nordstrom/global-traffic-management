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

import com.nordstrom.nfe.bootstrap.NfeApplicationBootstrap;
import com.nordstrom.nfe.bootstrap.NfeServiceLocator;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.nlpmanagement.CustomerAccountNlpDeploymentWatcher;
import com.nordstrom.nfe.nlpmanagement.KubernetesNlpDeploymentWatcher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.core.NullZkClient;
import java.io.File;

public class Main {
  public static void main(String[] args) {
    Config config = loadConfig();

    NfeConfig nfeConfig = new NfeConfig(config);
    NfeState nfeState = new NfeState(nfeConfig);
    NfeApplicationBootstrap bootstrap = new NfeApplicationBootstrap(nfeState);

    // The application created by bootstrap is holding all of the state.
    bootstrap.build();

    boolean haveZkClient = nfeState.getZkClient() instanceof NullZkClient;
    if (haveZkClient) {
      // Start Zookeeper Watcher to be notified of customer account NLP deployments.
      CustomerAccountNlpDeploymentWatcher customerAccountNlpDeploymentWatcher =
          NfeServiceLocator.getInstance().getCustomerAccountNlpDeploymentWatcher();
      customerAccountNlpDeploymentWatcher.start();

      // Start Zookeeper Watcher to be notified of Kubernetes (K8S) NLP deployments.
      KubernetesNlpDeploymentWatcher kubernetesNlpDeploymentWatcher =
          NfeServiceLocator.getInstance().getKubernetesNlpDeploymentWatcher();
      kubernetesNlpDeploymentWatcher.start();
    }
  }

  private static Config loadConfig() {
    String filePath = System.getProperty("config.file");
    File file = new File(filePath);
    Config config = ConfigFactory.parseFile(file);
    return ConfigFactory.load(config);
  }
}
