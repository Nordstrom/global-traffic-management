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
package com.nordstrom.nfe.servicedeployment;

import com.google.protobuf.Empty;
import com.nordstrom.gtm.servicedeployment.DeployedService;
import com.nordstrom.gtm.servicedeployment.StartRoutingRequest;
import com.nordstrom.nfe.config.ServiceDeploymentConfig;
import io.grpc.Status;
import io.grpc.StatusException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ServiceDeploymentDao {
  private ServiceDeploymentConfig serviceDeploymentConfig;

  public ServiceDeploymentDao(ServiceDeploymentConfig serviceDeploymentConfig) {
    this.serviceDeploymentConfig = serviceDeploymentConfig;
  }

  DeployedService addDeployedService(
      StartRoutingRequest request, CoreServiceDeploymentInfo coreServiceDeploymentInfo)
      throws SQLException, StatusException {
    try (Connection connection =
        DriverManager.getConnection(
            serviceDeploymentConfig.getDbUrl(),
            serviceDeploymentConfig.getDbUser(),
            serviceDeploymentConfig.getDbPassword())) {
      String deploymentId = insertDeployedService(connection, request, coreServiceDeploymentInfo);

      return DeployedService.newBuilder().setDeploymentId(deploymentId).build();
    }
  }

  Empty removeDeployedService(DeployedService deployedService) throws SQLException {
    try (Connection connection =
        DriverManager.getConnection(
            serviceDeploymentConfig.getDbUrl(),
            serviceDeploymentConfig.getDbUser(),
            serviceDeploymentConfig.getDbPassword())) {

      String deleteQuery =
          "BEGIN;"
              + "SET @route_parameter_id = (SELECT route_parameter.route_parameter_id FROM route_parameter WHERE route_parameter.deployment_id = ?);"
              + "DELETE FROM ip_address WHERE ip_address.route_parameter_id = @route_parameter_id;"
              + "DELETE FROM route_parameter WHERE route_parameter.route_parameter_id = @route_parameter_id;"
              + "COMMIT;";

      try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
        statement.setString(1, deployedService.getDeploymentId());

        statement.execute();
        return Empty.getDefaultInstance();
      }
    }
  }

  /**
   * Inserts a new record into the service deployment database.
   *
   * <p>Returns the deployment Id of the inserted record.
   */
  private String insertDeployedService(
      Connection connection,
      StartRoutingRequest request,
      CoreServiceDeploymentInfo coreServiceDeploymentInfo)
      throws SQLException, StatusException {
    String deploymentId = UUID.randomUUID().toString();

    try (PreparedStatement statement =
        sqlStatementForInsertingDeployedService(
            connection, request, coreServiceDeploymentInfo, deploymentId)) {
      statement.execute();
      return deploymentId;
    }
  }

  private PreparedStatement sqlStatementForInsertingDeployedService(
      Connection connection,
      StartRoutingRequest request,
      CoreServiceDeploymentInfo coreServiceDeploymentInfo,
      String deploymentId)
      throws SQLException, StatusException {
    switch (request.getModeCase()) {
      case ASG_INFO:
        return sqlStatementForAsgInsertion(
            connection, request, coreServiceDeploymentInfo, deploymentId);
      case IP_ADDRESSES_INFO:
        return sqlStatementForIpAddressListInsertion(
            connection, request, coreServiceDeploymentInfo, deploymentId);
    }

    throw new StatusException(Status.INVALID_ARGUMENT);
  }

  private PreparedStatement sqlStatementForAsgInsertion(
      Connection connection,
      StartRoutingRequest request,
      CoreServiceDeploymentInfo coreServiceDeploymentInfo,
      String deploymentId)
      throws SQLException {
    int index = 1;
    String routeParameterQueryString =
        "INSERT INTO route_parameter "
            + "(route_parameter.tag_key, route_parameter.tag_value, route_parameter.path, route_parameter.port_number, route_parameter.tls_enabled, route_parameter.cloud_account_id, route_parameter.service_description, route_parameter.deployment_id, route_parameter.mode) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    PreparedStatement statement = connection.prepareStatement(routeParameterQueryString);
    statement.setString(index++, request.getAsgInfo().getTagKey());
    statement.setString(index++, request.getAsgInfo().getTagValue());
    statement.setString(index++, coreServiceDeploymentInfo.getPath());
    statement.setInt(index++, request.getPortNumber());
    statement.setBoolean(index++, request.getTlsEnabled());
    statement.setString(index++, coreServiceDeploymentInfo.getCloudAccountId());
    statement.setString(index++, coreServiceDeploymentInfo.getServiceDescription());
    statement.setString(index++, deploymentId);
    statement.setString(index++, "asg");

    return statement;
  }

  private PreparedStatement sqlStatementForIpAddressListInsertion(
      Connection connection,
      StartRoutingRequest request,
      CoreServiceDeploymentInfo coreServiceDeploymentInfo,
      String deploymentId)
      throws SQLException {
    int index = 1;

    // This query inserts a single record for `route_parameter` table and n rows into `ip_address`
    // table where n is all the ip addresses.
    // This query is wrapped in a COMMIT transaction so that if any part fails then all changes are
    // aborted.
    List<String> ipAddressValueMarkers =
        Collections.nCopies(
            request.getIpAddressesInfo().getIpAddressList().size(), "(@route_parameter_id, ?)");
    String insertQuery =
        "BEGIN;"
            + "INSERT INTO route_parameter "
            + "(route_parameter.path, route_parameter.port_number, route_parameter.tls_enabled, route_parameter.cloud_account_id, route_parameter.service_description, route_parameter.deployment_id, route_parameter.mode) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?);"
            + "SELECT LAST_INSERT_ID() INTO @route_parameter_id;"
            + "INSERT INTO ip_address "
            + "(ip_address.route_parameter_id, ip_address.value) "
            + "VALUES "
            + String.join(", ", ipAddressValueMarkers)
            + ";"
            + "COMMIT;";

    PreparedStatement statement = connection.prepareStatement(insertQuery);

    // Adds values for `route_parameter` table.
    statement.setString(index++, coreServiceDeploymentInfo.getPath());
    statement.setInt(index++, request.getPortNumber());
    statement.setBoolean(index++, request.getTlsEnabled());
    statement.setString(index++, coreServiceDeploymentInfo.getCloudAccountId());
    statement.setString(index++, coreServiceDeploymentInfo.getServiceDescription());
    statement.setString(index++, deploymentId);
    statement.setString(index++, "ip_address_list");

    // Adds values for `ip_address` table.
    for (String ipaddress : request.getIpAddressesInfo().getIpAddressList()) {
      statement.setString(index++, ipaddress);
    }

    return statement;
  }
}
