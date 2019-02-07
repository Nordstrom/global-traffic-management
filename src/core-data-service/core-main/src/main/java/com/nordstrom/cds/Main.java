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
package com.nordstrom.cds;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.zookeeper.AwsDeployment;
import com.xjeffrose.xio.zookeeper.AwsDeploymentConfig;
import java.io.File;
import javax.annotation.Nullable;

public class Main {
  public static Application application;

  public static void main(String[] args) throws Exception {
    String configFileLocation = args.length > 0 ? args[0] : null;
    Config baseConfig = loadBaseConfig(configFileLocation);
    ApplicationConfig applicationConfig = loadApplicationConfig(baseConfig);
    CoreApplicationState applicationState = new CoreApplicationState(applicationConfig);
    CoreApplicationBootstrap applicationBootstrap = new CoreApplicationBootstrap(applicationState);
    AwsDeploymentConfig awsDeploymentConfig = loadAwsDeploymentConfig(baseConfig);
    int cdsMainPort = baseConfig.getInt("core.application.servers.cds-main.settings.bindPort");
    new AwsDeployment(awsDeploymentConfig, cdsMainPort).start();
    application = applicationBootstrap.build();
  }

  private static Config loadBaseConfig(@Nullable String configFileLocation) {
    Config appConfig = getBaseConfig(configFileLocation, "application");
    return ConfigFactory.load(appConfig);
  }

  private static ApplicationConfig loadApplicationConfig(Config baseConfig) {
    return new ApplicationConfig(baseConfig.getConfig("core.application"));
  }

  private static AwsDeploymentConfig loadAwsDeploymentConfig(Config baseConfig) {
    return new AwsDeploymentConfig(baseConfig.getConfig("core.awsDeployment"));
  }

  private static Config getBaseConfig(
      @Nullable String configFileLocation, String fallbackResourceBasename) {
    String filePath =
        configFileLocation != null ? configFileLocation : System.getProperty("config.file");
    Config appConfig;
    if (filePath != null) {
      File file = new File(filePath);
      appConfig = ConfigFactory.parseFileAnySyntax(file);
    } else {
      appConfig = ConfigFactory.load(fallbackResourceBasename);
    }
    return appConfig;
  }
}
