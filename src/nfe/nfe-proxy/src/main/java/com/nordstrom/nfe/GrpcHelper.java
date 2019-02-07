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

public class GrpcHelper {
  /**
   * Builds the gRPC path based on information from the `.proto` file.
   *
   * @param packageName the name of the gRPC package. Found in the `.proto` file, at the top of the
   *     file, after the `package` keyword.
   * @param serviceName the name of the gRPC service. Found in the `.proto` file, in the service
   *     definition, after the `service` keyword.
   * @param methodName the name of the gRPC method. Found in the `.proto` file, in the service
   *     definition, after the `rpc` keyword.
   */
  public static String buildGrpcPath(String packageName, String serviceName, String methodName) {
    return "/" + packageName + "." + serviceName + "/" + methodName;
  }

  public static String buildGrpcOu(String packageName, String serviceName) {
    return packageName + "." + serviceName;
  }
}
