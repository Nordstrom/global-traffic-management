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

import com.google.protobuf.Empty;
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoRequest;
import com.nordstrom.gtm.coredb.GetNlpRoutingInfoResponse;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoRequest;
import com.nordstrom.gtm.coredb.GetServiceDeployTargetInfoResponse;
import com.nordstrom.gtm.coredb.PathComponents;
import com.nordstrom.gtm.servicedeploytarget.AwsInfo;
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.CreateServiceDeployTargetResponse;
import com.nordstrom.gtm.servicedeploytarget.DeleteServiceDeployTargetRequest;
import com.nordstrom.gtm.servicedeploytarget.DeploymentPlatformInfo;
import com.nordstrom.gtm.servicedeploytarget.Environment;
import com.nordstrom.gtm.serviceregistration.AuthType;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

class ServiceRegistrationDao {
  private static final String DEPLOY_TARGET_PLATFORM_AWS = "aws";
  private static final String DEPLOY_TARGET_PLATFORM_GCP = "gcp";
  private static final String DEPLOY_TARGET_PLATFORM_ON_PREM = "onprem";

  private static final String DEPLOY_TARGET_ENVIRONMENT_PROD = "prod";
  private static final String DEPLOY_TARGET_ENVIRONMENT_NON_PROD = "nonprod";

  private static final String SERVICE_REGISTRATION_AUTH_TYPE_NONE = "none";
  private static final String SERVICE_REGISTRATION_AUTH_TYPE_APIKEY = "apikey";
  private static final String SERVICE_REGISTRATION_AUTH_TYPE_MUTUAL_AUTH = "mutual_auth";

  private final String dbUrl;
  private final String dbUser;
  private final String dbPassword;

  ServiceRegistrationDao(DatabaseConfig config) {
    this.dbUrl = config.dbUrl();
    this.dbUser = config.dbUser();
    this.dbPassword = config.dbPassword();
  }

  /**
   * Returns the "core" needed for NFEs to route to an NLP that are described by their deployment
   * environment.
   */
  GetNlpRoutingInfoResponse getNlpRoutingInfo(GetNlpRoutingInfoRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      String selectQuery =
          "SELECT DISTINCT app.name, ou.name, dt.version "
              + "FROM app "
              + "LEFT JOIN organizational_unit AS ou USING (organizational_unit_id) "
              + "LEFT JOIN deploy_target AS dt USING (app_id) "
              + "WHERE dt.cloud_account_id = ? AND dt.platform = ?";

      String platform = deployTargetPlatformDbString(request.getDeploymentPlatformCase());

      try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
        preparedStatement.setString(1, request.getAwsAccountId());
        preparedStatement.setString(2, platform);
        ResultSet resultSet = preparedStatement.executeQuery();

        GetNlpRoutingInfoResponse.Builder responseBuilder = GetNlpRoutingInfoResponse.newBuilder();
        while (resultSet.next()) {
          PathComponents.Builder pathComponentsBuilder =
              PathComponents.newBuilder()
                  .setOrganizationUnit(resultSet.getString("ou.name"))
                  .setServiceName(resultSet.getString("app.name"));

          String version = resultSet.getString("dt.version");
          if (version != null) {
            pathComponentsBuilder.setServiceVersion(version);
          }

          responseBuilder.addPathComponentsArray(pathComponentsBuilder.build());
        }

        return responseBuilder.build();
      }
    }
  }

  /**
   * Returns the "core" info needed for NLPs to route to service instances that are described by a
   * specific deploy target.
   */
  GetServiceDeployTargetInfoResponse getServiceDeployTargetInfo(
      GetServiceDeployTargetInfoRequest request) throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      String selectQuery =
          "SELECT app.name, app.description, ou.name, dt.version, dt.cloud_account_id, dt.health_check_path "
              + "FROM app "
              + "LEFT JOIN organizational_unit AS ou USING (organizational_unit_id) "
              + "LEFT JOIN deploy_target AS dt USING (app_id) "
              + "WHERE dt.deploy_target_key = ? "
              + "LIMIT 1";

      try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
        preparedStatement.setString(1, request.getDeployTargetKey());
        ResultSet resultSet = preparedStatement.executeQuery();

        if (!resultSet.next()) {
          throw CoreDataServiceException.noResultsFound(
              "deploy target key", request.getDeployTargetKey());
        }

        return GetServiceDeployTargetInfoResponse.newBuilder()
            .setPathComponents(
                PathComponents.newBuilder()
                    .setServiceVersion(resultSet.getString("dt.version"))
                    .setOrganizationUnit(resultSet.getString("ou.name"))
                    .setServiceName(resultSet.getString("app.name"))
                    .build())
            .setDeploymentPlatformInfo(
                DeploymentPlatformInfo.newBuilder()
                    .setAwsInfo(
                        AwsInfo.newBuilder()
                            .setAccountId(resultSet.getString("dt.cloud_account_id"))
                            .build())
                    .build())
            .setServiceDescription(resultSet.getString("app.description"))
            .setHealthCheck(resultSet.getString("dt.health_check_path"))
            .build();
      }
    }
  }

  /** Creates a new service and associates it with the provided organizational unit. */
  CreateServiceRegistrationResponse createServiceRegistration(
      CreateServiceRegistrationRequest request) throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      boolean serviceExists = serviceId(connection, request.getServiceName()).isPresent();
      if (serviceExists) {
        throw CoreDataServiceException.serviceAlreadyExists(request.getServiceName());
      }

      int organizationalUnitId = getOrganizationalUnitId(connection, request.getOrganizationUnit());
      insertServiceRegistration(connection, request, organizationalUnitId);

      return CreateServiceRegistrationResponse.newBuilder().build();
    }
  }

  CreateServiceDeployTargetResponse createServiceDeployTarget(
      CreateServiceDeployTargetRequest request) throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

      Optional<Integer> serviceId = serviceId(connection, request.getServiceName());
      if (!serviceId.isPresent()) {
        throw CoreDataServiceException.invalidArgument("service name", request.getServiceName());
      }

      String deployTargetKey = UUID.randomUUID().toString();
      insertServiceDeployTarget(connection, request, serviceId.get(), deployTargetKey);

      return CreateServiceDeployTargetResponse.newBuilder()
          .setDeployTargetKey(deployTargetKey)
          .build();
    }
  }

  Empty deleteServiceDeployTarget(DeleteServiceDeployTargetRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      String deleteQuery =
          "DELETE FROM deploy_target " + "WHERE deploy_target.deploy_target_key = ? " + "LIMIT 1";

      try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
        statement.setString(1, request.getDeployTargetKey());
        statement.execute();

        return Empty.getDefaultInstance();
      }
    }
  }

  private Optional<Integer> serviceId(Connection connection, String serviceName)
      throws SQLException {
    String existsQuery = "SELECT app.app_id " + "FROM app " + "WHERE app.name = ? " + "LIMIT 1";

    try (PreparedStatement preparedStatement = connection.prepareStatement(existsQuery)) {
      preparedStatement.setString(1, serviceName);

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(resultSet.getInt("app.app_id"));
        } else {
          return Optional.empty();
        }
      }
    }
  }

  private int getOrganizationalUnitId(Connection connection, String organizationalUnitName)
      throws SQLException, CoreDataServiceException {
    String selectQuery =
        "SELECT ou.organizational_unit_id "
            + "FROM organizational_unit AS ou "
            + "WHERE ou.name = ? "
            + "LIMIT 1";

    try (PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery)) {
      selectPreparedStatement.setString(1, organizationalUnitName);

      // Check if that organizational unit already exists
      try (ResultSet initialResultSet = selectPreparedStatement.executeQuery()) {
        if (initialResultSet.next()) {
          return initialResultSet.getInt("organizational_unit_id");

        } else {
          throw CoreDataServiceException.invalidArgument(
              "organizational unit name", organizationalUnitName);
        }
      }
    }
  }

  private void insertServiceRegistration(
      Connection connection, CreateServiceRegistrationRequest request, int organizationalUnitId)
      throws SQLException, CoreDataServiceException {

    String queryString =
        "INSERT INTO app "
            + "(app.name, app.organizational_unit_id, app.description, app.service_now_id, app.updated, app.is_default_allow, auth_type) "
            + "VALUES (?, ?, ?, ?, now(), ?, ?)";

    int index = 1;
    try (PreparedStatement statement = connection.prepareStatement(queryString)) {
      statement.setString(index++, request.getServiceName());
      statement.setInt(index++, organizationalUnitId);
      statement.setString(index++, request.getDescription());
      statement.setInt(index++, request.getServiceNowId());
      statement.setBoolean(index++, request.getIsDefaultAllow());
      statement.setString(index++, authTypeDbString(request.getAuthType()));

      statement.execute();
    }
  }

  private void insertServiceDeployTarget(
      Connection connection,
      CreateServiceDeployTargetRequest request,
      int serviceId,
      String deployTargetKey)
      throws SQLException, CoreDataServiceException {

    String insertQuery =
        "INSERT INTO deploy_target "
            + "(app_id, deploy_target_key, version, environment, health_check_path, tls_enabled, platform, cloud_account_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    int index = 1;
    try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
      statement.setInt(index++, serviceId);
      statement.setString(index++, deployTargetKey);

      if (request.getServiceVersion() != null) {
        statement.setString(index++, request.getServiceVersion());
      } else {
        statement.setNull(index++, Types.CHAR);
      }

      statement.setString(index++, deployTargetEnvironmentDbString(request.getEnvironment()));

      if (request.getHealthCheckPath() != null) {
        statement.setString(index++, request.getHealthCheckPath());
      } else {
        statement.setNull(index++, Types.CHAR);
      }

      statement.setBoolean(index++, request.getIsTlsEnabled());
      statement.setString(
          index++, deployTargetPlatformDbString(request.getDeploymentPlatformInfo().getTypeCase()));

      Optional<String> cloudAccountId =
          deployTargetCloudAccountIdDbString(request.getDeploymentPlatformInfo());
      if (cloudAccountId.isPresent()) {
        statement.setString(index++, cloudAccountId.get());
      } else {
        statement.setNull(index++, Types.CHAR);
      }

      statement.execute();
    }
  }

  private String deployTargetEnvironmentDbString(Environment environment)
      throws CoreDataServiceException {
    switch (environment) {
      case PROD:
        return DEPLOY_TARGET_ENVIRONMENT_PROD;
      case NONPROD:
        return DEPLOY_TARGET_ENVIRONMENT_NON_PROD;
      default:
        throw CoreDataServiceException.invalidArgument("environment", environment.toString());
    }
  }

  private String deployTargetPlatformDbString(
      DeploymentPlatformInfo.TypeCase deploymentEnvironmentTypeCase)
      throws CoreDataServiceException {
    switch (deploymentEnvironmentTypeCase) {
      case AWS_INFO:
        return DEPLOY_TARGET_PLATFORM_AWS;
      case GCP_INFO:
        return DEPLOY_TARGET_PLATFORM_GCP;
      case ON_PREM_INFO:
        return DEPLOY_TARGET_PLATFORM_ON_PREM;
      default:
        throw CoreDataServiceException.invalidArgument(
            "deployment platform", deploymentEnvironmentTypeCase.toString());
    }
  }

  private String deployTargetPlatformDbString(
      GetNlpRoutingInfoRequest.DeploymentPlatformCase deploymentPlatformCase)
      throws CoreDataServiceException {
    switch (deploymentPlatformCase) {
      case AWS_ACCOUNT_ID:
        return DEPLOY_TARGET_PLATFORM_AWS;
      default:
        throw CoreDataServiceException.invalidArgument(
            "deployment platform", deploymentPlatformCase.toString());
    }
  }

  private Optional<String> deployTargetCloudAccountIdDbString(
      DeploymentPlatformInfo deploymentPlatformInfo) throws CoreDataServiceException {
    switch (deploymentPlatformInfo.getTypeCase()) {
      case AWS_INFO:
        return Optional.of(deploymentPlatformInfo.getAwsInfo().getAccountId());
      case GCP_INFO:
        return Optional.of(deploymentPlatformInfo.getGcpInfo().getAccountId());
      default:
        return Optional.empty();
    }
  }

  private String authTypeDbString(AuthType authType) throws CoreDataServiceException {
    switch (authType) {
      case NONE:
        return SERVICE_REGISTRATION_AUTH_TYPE_NONE;
      case APIKEY:
        return SERVICE_REGISTRATION_AUTH_TYPE_APIKEY;
      case MUTUAL_AUTH:
        return SERVICE_REGISTRATION_AUTH_TYPE_MUTUAL_AUTH;
      default:
        throw CoreDataServiceException.invalidArgument("auth type", authType.toString());
    }
  }
}
