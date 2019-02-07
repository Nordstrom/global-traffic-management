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
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.coredb.DeleteApiKeyRequest;
import com.nordstrom.gtm.coredb.ListApiKeysRequest;
import com.nordstrom.gtm.coredb.ListApiKeysResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class ApiKeyDao {
  private String dbUrl;
  private String dbUser;
  private String dbPassword;

  ApiKeyDao(DatabaseConfig config) {
    this.dbUrl = config.dbUrl();
    this.dbUser = config.dbUser();
    this.dbPassword = config.dbPassword();
  }

  Empty saveApiKey(ApiKey apiKey) throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      int appId = getAppId(connection, apiKey.getServiceName());
      insertApiKey(connection, appId, apiKey);
      return Empty.newBuilder().build();
    }
  }

  ListApiKeysResponse listApiKeys(ListApiKeysRequest request)
      throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      String selectQuery =
          "SELECT api_key.key, api_key.name, ou.name "
              + "FROM app "
              + "LEFT JOIN api_key USING (app_id) "
              + "LEFT JOIN organizational_unit AS ou USING (organizational_unit_id) "
              + "WHERE app.name = ?";

      try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
        preparedStatement.setString(1, request.getServiceName());

        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          ListApiKeysResponse.Builder responseBuilder = ListApiKeysResponse.newBuilder();

          while (resultSet.next()) {
            ApiKey apiKey =
                ApiKey.newBuilder()
                    .setTeamName(resultSet.getString("ou.name"))
                    .setServiceName(request.getServiceName())
                    .setKeyName(resultSet.getString("api_key.name"))
                    .setKey(resultSet.getString("api_key.key"))
                    .build();
            responseBuilder.addApiKeys(apiKey);
          }

          return responseBuilder.build();
        }
      }
    }
  }

  Empty deleteApiKey(DeleteApiKeyRequest request) throws SQLException, CoreDataServiceException {
    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      String deleteQuery = "DELETE FROM api_key " + "WHERE api_key.key = ? " + "LIMIT 1";

      try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
        preparedStatement.setString(1, request.getKey());
        preparedStatement.execute();

        return Empty.newBuilder().build();
      }
    }
  }

  private int getAppId(Connection connection, String serviceName)
      throws SQLException, CoreDataServiceException {
    String selectQuery = "SELECT app.app_id " + "FROM app " + "WHERE app.name = ? " + "LIMIT 1";

    try (PreparedStatement statement = connection.prepareStatement(selectQuery)) {
      statement.setString(1, serviceName);

      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("app_id");
        } else {
          throw CoreDataServiceException.invalidArgument("service name", serviceName);
        }
      }
    }
  }

  private void insertApiKey(Connection connection, int appId, ApiKey apiKey) throws SQLException {
    String insertQuery =
        "INSERT INTO api_key "
            + "(api_key.app_id, api_key.key, api_key.name) "
            + "VALUES (?, ?, ?)";

    int index = 1;
    try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
      statement.setInt(index++, appId);
      statement.setString(index++, apiKey.getKey());
      statement.setString(index++, apiKey.getKeyName());

      statement.execute();
    }
  }
}
