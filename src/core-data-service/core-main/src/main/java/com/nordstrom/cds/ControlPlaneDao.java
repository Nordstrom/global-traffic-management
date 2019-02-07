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
import com.nordstrom.gtm.ipfilter.AddAppIpFilterRequest;
import com.nordstrom.gtm.ipfilter.AddAppIpFilterResponse;
import com.nordstrom.gtm.ipfilter.IpFilter;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersRequest;
import com.nordstrom.gtm.ipfilter.ListAppIpFiltersResponse;
import com.nordstrom.gtm.ipfilter.RemoveAppIpFilterRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControlPlaneDao {
  private static final String IP_FILTER_TYPE_ALLOW = "allow";
  private static final String IP_FILTER_TYPE_DENY = "deny";

  private final String dbUrl;
  private final String dbUser;
  private final String dbPassword;

  public ControlPlaneDao(DatabaseConfig config) {
    this.dbUrl = config.dbUrl();
    this.dbUser = config.dbUser();
    this.dbPassword = config.dbPassword();
  }

  AddAppIpFilterResponse addAppIpFilter(AddAppIpFilterRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      Optional<Integer> serviceId = serviceId(connection, request.getServiceName());
      if (!serviceId.isPresent()) {
        throw CoreDataServiceException.invalidArgument("service name", request.getServiceName());
      }

      String ipFilterKey = UUID.randomUUID().toString();
      insertIpFilter(connection, request, serviceId.get(), ipFilterKey);

      return AddAppIpFilterResponse.newBuilder().setIpFilterKey(ipFilterKey).build();
    }
  }

  Empty removeAppIpFilter(RemoveAppIpFilterRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      Optional<Integer> serviceId = serviceId(connection, request.getServiceName());
      if (!serviceId.isPresent()) {
        throw CoreDataServiceException.invalidArgument("service name", request.getServiceName());
      }

      deleteIpFilter(connection, request, serviceId.get());

      return Empty.getDefaultInstance();
    }
  }

  ListAppIpFiltersResponse listAppIpFilter(ListAppIpFiltersRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      return getIpFilters(connection, request);
    }
  }

  private void insertIpFilter(
      Connection connection, AddAppIpFilterRequest request, int serviceId, String ipFilterKey)
      throws SQLException, CoreDataServiceException {
    String insertQuery =
        "INSERT INTO ip_filter "
            + "(app_id, ip_filter_key, cidr_network_address, type, is_disabled, notes) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    int index = 1;
    try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
      statement.setInt(index++, serviceId);
      statement.setString(index++, ipFilterKey);
      statement.setString(index++, request.getIpFilter().getCidrAddress());
      statement.setString(index++, ipFilterTypeDbString(request.getIpFilter().getType()));
      statement.setBoolean(index++, false);

      if (request.getIpFilter().getNotes() != null) {
        statement.setString(index++, request.getIpFilter().getNotes());
      } else {
        statement.setNull(index++, Types.CHAR);
      }

      statement.execute();
    }
  }

  private void deleteIpFilter(
      Connection connection, RemoveAppIpFilterRequest request, int serviceId)
      throws SQLException, CoreDataServiceException {
    String existsQuery =
        "SELECT ip_filter.ip_filter_id "
            + "FROM ip_filter "
            + "WHERE ip_filter.app_id = ? AND ip_filter.ip_filter_key = ? "
            + "LIMIT 1";
    try (PreparedStatement statement = connection.prepareStatement(existsQuery)) {
      statement.setInt(1, serviceId);
      statement.setString(2, request.getIpFilterKey());

      boolean ipFilterExists = statement.executeQuery().next();

      if (!ipFilterExists) {
        throw CoreDataServiceException.noResultsFound(
            "ip filter key", request.getIpFilterKey(), "service name", request.getServiceName());
      }
    }

    String deleteQuery =
        "DELETE FROM ip_filter "
            + "WHERE ip_filter.app_id = ? AND ip_filter.ip_filter_key = ? "
            + "LIMIT 1";

    try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
      statement.setInt(1, serviceId);
      statement.setString(2, request.getIpFilterKey());

      statement.execute();
    }
  }

  private ListAppIpFiltersResponse getIpFilters(
      Connection connection, ListAppIpFiltersRequest request) throws SQLException {
    String selectQuery =
        "SELECT ip_filter.ip_filter_key, ip_filter.cidr_network_address, ip_filter.type, ip_filter.is_disabled, ip_filter.notes "
            + "FROM app "
            + "LEFT JOIN ip_filter USING (app_id) "
            + "WHERE app.name = ?";

    try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
      statement.setString(1, request.getServiceName());

      ResultSet resultSet = statement.executeQuery();
      HashMap<String, IpFilter> ipFilters = new HashMap<>();

      while (resultSet.next()) {
        String typeString = resultSet.getString("ip_filter.type");

        try {
          IpFilter.Builder ipFilterBuilder =
              IpFilter.newBuilder()
                  .setType(ipFilterType(typeString))
                  .setCidrAddress(resultSet.getString("ip_filter.cidr_network_address"))
                  .setIsDisabled(resultSet.getBoolean("ip_filter.is_disabled"));

          String notes = resultSet.getString("ip_filter.notes");
          if (notes != null) {
            ipFilterBuilder.setNotes(notes);
          }
          String key = resultSet.getString("ip_filter.ip_filter_key");

          ipFilters.put(key, ipFilterBuilder.build());

        } catch (CoreDataServiceException e) {
          log.warn("unable to build filter", e);
        }
      }

      return ListAppIpFiltersResponse.newBuilder().putAllIpFilterKeyToIpFilter(ipFilters).build();
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

  private String ipFilterTypeDbString(IpFilter.Type type) throws CoreDataServiceException {
    switch (type) {
      case ALLOW:
        return IP_FILTER_TYPE_ALLOW;
      case DENY:
        return IP_FILTER_TYPE_DENY;
      default:
        throw CoreDataServiceException.invalidDatabaseValue("ip_filter.type", type.toString());
    }
  }

  private IpFilter.Type ipFilterType(String dbString) throws CoreDataServiceException {
    switch (dbString) {
      case "allow":
        return IpFilter.Type.ALLOW;
      case "deny":
        return IpFilter.Type.DENY;
      default:
        throw CoreDataServiceException.noResultsFound("ip filter type", dbString);
    }
  }
}
