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
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class DatabaseConfig {
  @Getter private final String dbName;
  @Getter private final String dbHost;
  @Getter private final String dbUser;
  @Getter private final String dbPassword;

  public static DatabaseConfig fromConfig(Config config) {
    String dbName = config.getString("dbName");
    String dbHost = config.getString("host");
    String dbUser = config.getString("user");
    String dbPassword = config.getString("password");
    return new DatabaseConfig(dbName, dbHost, dbUser, dbPassword);
  }

  public DatabaseConfig(String dbName, String dbHost, String dbUser, String dbPassword) {
    this.dbName = dbName;
    this.dbHost = dbHost;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
  }

  public String dbUrl() {
    return "jdbc:mysql://"
        + dbHost()
        + ":3306/"
        + dbName()
        + "?useSSL=false&serverTimezone=UTC&useJDBCCompliantTimezoneShift=true";
  }
}
