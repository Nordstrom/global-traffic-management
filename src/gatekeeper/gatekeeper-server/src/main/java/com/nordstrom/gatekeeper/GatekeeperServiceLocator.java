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

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.zookeeper.AwsDeployment;
import com.xjeffrose.xio.zookeeper.AwsDeploymentConfig;
import java.io.File;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.mgt.DefaultSecurityManager;

@Slf4j
public class GatekeeperServiceLocator {
  @Getter(lazy = true)
  private final GatekeeperDao dao = gatekeeperDao();

  @Getter(lazy = true)
  private final SubjectTokenRealm subjectTokenRealm = new SubjectTokenRealm();

  @Getter(lazy = true)
  private final GatekeeperDbRealm dynamoDbRealm = dynamoDbRealm();

  @Getter(lazy = true)
  private final RoleResolver roleResolver = new RoleResolver(getDao());

  @Getter(lazy = true)
  private final DefaultSecurityManager securityManager =
      new DefaultSecurityManager(getDynamoDbRealm());

  @Getter(lazy = true)
  private final Authorizer authZ = new Authorizer(getSecurityManager());

  @Getter(lazy = true)
  private final GatekeeperServer gatekeeperServer =
      new GatekeeperServer(
          getGatekeeperGrpcService(), getGatekeeperGrpcManagementService(), getGatekeeperConfig());

  @Getter(lazy = true)
  private final GatekeeperGrpcService gatekeeperGrpcService = new GatekeeperGrpcService(getAuthZ());

  @Getter(lazy = true)
  private final GatekeeperGrpcManagementService gatekeeperGrpcManagementService =
      new GatekeeperGrpcManagementService(getDao());

  @Getter(lazy = true)
  private final GatekeeperDbCacheManager gatekeeperDbCacheManager = new GatekeeperDbCacheManager();

  @Getter(lazy = true)
  private final GatekeeperConfig gatekeeperConfig = createGatekeeperConfig();

  @Getter(lazy = true)
  private final AwsDeployment awsDeployment = createAwsDeployment();

  private GatekeeperConfig createGatekeeperConfig() {
    return Optional.ofNullable(System.getProperty("CONFIG_PATH"))
        .map(
            configPath -> {
              File file = new File(configPath);
              log.debug("creating gatekeeper config from file: {}", file.getAbsolutePath());
              return new GatekeeperConfig(ConfigFactory.load(ConfigFactory.parseFile(file)));
            })
        .orElseGet(
            () -> {
              log.debug("creating default gatekeeper config");
              return new GatekeeperConfig();
            });
  }

  private AwsDeployment createAwsDeployment() {
    GatekeeperConfig gConfig = getGatekeeperConfig();
    AwsDeploymentConfig deploymentConfig = gConfig.getAwsDeploymentConfig();
    int port = gConfig.getPort();
    try {
      return new AwsDeployment(deploymentConfig, port);
    } catch (Exception e) {
      log.error(
          "could not create aws deployment with config {} and port {}", deploymentConfig, port);
      throw new RuntimeException(e);
    }
  }

  private GatekeeperDbRealm dynamoDbRealm() {
    GatekeeperDbRealm realm = new GatekeeperDbRealm(getSubjectTokenRealm(), getDao());
    realm.setRolePermissionResolver(getRoleResolver());
    realm.setCacheManager(getGatekeeperDbCacheManager());
    return realm;
  }

  private GatekeeperDao gatekeeperDao() {
    @Nullable
    String localDynamoHost =
        Optional.ofNullable(System.getenv("LOCAL_DYNAMO_HOST"))
            .filter(it -> !it.isEmpty())
            .orElse(null);
    return new GatekeeperDao(localDynamoHost, getGatekeeperDbCacheManager());
  }
}
