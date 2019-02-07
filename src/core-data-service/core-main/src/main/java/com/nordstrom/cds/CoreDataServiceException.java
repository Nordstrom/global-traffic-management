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

import lombok.Getter;

@Getter
class CoreDataServiceException extends Exception {
  public enum Type {
    SERVICE_ALREADY_EXISTS,
    API_KEY_ALREADY_EXISTS,
    INVALID_ARGUMENT,
    NO_RESULTS_FOUND,
    INVALID_DATABASE_VALUE
  }

  private final Type type;
  private final String description;

  private CoreDataServiceException(Type type, String description) {
    this.type = type;
    this.description = description;
  }

  static CoreDataServiceException serviceAlreadyExists(String serviceName) {
    return new CoreDataServiceException(
        Type.SERVICE_ALREADY_EXISTS, "Service with name " + quote(serviceName) + " already exists");
  }

  static CoreDataServiceException apiKeyAlreadyExists(String apiKeyName) {
    return new CoreDataServiceException(
        Type.API_KEY_ALREADY_EXISTS, "ApiKey with name " + quote(apiKeyName) + " already exists");
  }

  static CoreDataServiceException invalidArgument(String argumentName, String argumentValue) {
    return new CoreDataServiceException(
        Type.INVALID_ARGUMENT,
        "Argument " + quote(argumentName) + " with value " + quote(argumentValue) + " is invalid");
  }

  static CoreDataServiceException noResultsFound(String argumentName, String argumentValue) {
    return noResultsFound(argumentName, argumentValue, null, null);
  }

  static CoreDataServiceException noResultsFound(
      String argumentName,
      String argumentValue,
      String qualifierArgumentName,
      String qualifierArgumentValue) {
    StringBuilder stringBuilder =
        new StringBuilder()
            .append("No results found where argument ")
            .append(quote(argumentName))
            .append(" has a value of ")
            .append(quote(argumentValue));

    if (qualifierArgumentName != null && qualifierArgumentValue != null) {
      stringBuilder
          .append(" OR does not belong to ")
          .append(quote(qualifierArgumentName))
          .append(" with value of ")
          .append(quote(qualifierArgumentValue));
    }

    return new CoreDataServiceException(Type.NO_RESULTS_FOUND, stringBuilder.toString());
  }

  static CoreDataServiceException invalidDatabaseValue(String fieldName, String fieldValue) {
    return new CoreDataServiceException(
        Type.INVALID_DATABASE_VALUE,
        "Datebase field" + quote(fieldName) + " has unrecognized value " + quote(fieldValue));
  }

  private static String quote(String original) {
    return "'" + original + "'";
  }
}
