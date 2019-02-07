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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.nordstrom.nlp.HealthChecks.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.config.DynamicRouteConfig;
import com.xjeffrose.xio.config.DynamicRouteConfigsFactory;
import com.xjeffrose.xio.config.RouteReloader;
import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Main {
  private static RouteReloader<ImmutableList<DynamicRouteConfig>> reloader;
  private static SimpleAppHealthCheckDaemon healthChecker;

  public static void main(String[] args) {
    checkArgument(
        args.length == 2,
        "Please supply the application.conf and the route.json file as commandline arguments. Example: java -jar myjar.jar ./application.conf ./route.json");
    val applicationConfig = args[0];
    val watchFile = args[1];

    ImmutableList<DynamicRouteConfig> initialDynamicRouteConfigs = setupRouteReloader(watchFile);

    Config config = loadConfig(applicationConfig);
    NlpConfig nlpConfig = new NlpConfig(config);

    ProxyRouteConfigFactory proxyRouteConfigFactory = buildProxyRouteConfigFactory(nlpConfig);
    NlpState nlpState =
        new NlpState(nlpConfig, initialDynamicRouteConfigs, proxyRouteConfigFactory);

    // start the route reloading daemon that watches for changes in route configs
    reloader.start(nlpState::reloadRouteStates);

    // create and start the healthcheck daemon that uses candidate endpoints from the nlpstate
    // and runs health checks against them
    healthChecker = buildHealthCheckDaemon(nlpState);
    healthChecker.start();

    NlpApplicationBootstrap bootstrap = new NlpApplicationBootstrap(nlpState);
    // The application created by bootstrap is holding all of the state.
    bootstrap.build();

    NlpDeployment nlpDeployment = new NlpDeployment();
    nlpDeployment.phoneHome(nlpState.getNlpConfig());
  }

  private static ProxyRouteConfigFactory buildProxyRouteConfigFactory(NlpConfig nlpConfig) {
    SimpleProxyRouteConfigFactory simpleProxyRouteConfigFactory =
        new SimpleProxyRouteConfigFactory(nlpConfig);
    HealthCheckRouteConfigFactory healthCheckRouteConfigFactory =
        new HealthCheckRouteConfigFactory();
    HealthCheckRouteAppender healthCheckRouteAppender =
        new HealthCheckRouteAppender(healthCheckRouteConfigFactory);
    ProxyRouteConfigFactory proxyRouteConfigFactory =
        new HealthCheckAppenderProxyRouteConfigFactory(
            simpleProxyRouteConfigFactory, healthCheckRouteAppender);
    return proxyRouteConfigFactory;
  }

  private static SimpleAppHealthCheckDaemon buildHealthCheckDaemon(NlpState nlpState) {
    OkHttpClientWrapper okHttpClientWrapper = OkHttpClientWrapperFactory.createClientWrapper();
    AppEndpointHealthCheckService healthCheckService =
        new AppEndpointHealthCheckService(okHttpClientWrapper);
    SimpleAppHealthCheckDaemon healthCheckDaemon =
        new SimpleAppHealthCheckDaemon(
            createExecutor(),
            healthCheckService,
            nlpState::updateHealthyHostMap,
            nlpState::getCandidateDynamicRouteConfigs);
    return healthCheckDaemon;
  }

  private static ScheduledExecutorService createExecutor() {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    return executor;
  }

  private static Config loadConfig(String filePath) {
    File file = new File(filePath);
    Config config = ConfigFactory.parseFile(file);
    return ConfigFactory.load(config);
  }

  private static ImmutableList<DynamicRouteConfig> setupRouteReloader(String watchFile) {
    reloader =
        new RouteReloader<ImmutableList<DynamicRouteConfig>>(
            createExecutor(), DynamicRouteConfigsFactory::build);
    return reloader.init(watchFile);
  }
}
