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
package com.nordstrom.nlp.HealthChecks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.DynamicClientConfig;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

/**
 * EndpointHealthCheckService is used to execute healthcheck checks against lists of ipAddresses The
 * results of these healthChecks are a filtered list of the ipAddresses that passed the heatlhcheck
 */
@Slf4j
public class AppEndpointHealthCheckService {
  private OkHttpClientWrapper clientWrapper;
  static final int SUCCESS = 200;

  public AppEndpointHealthCheckService(OkHttpClientWrapper clientWrapper) {
    this.clientWrapper = clientWrapper;
  }

  /**
   * Thie method is used to heatlh check a list of ipAddresses
   *
   * @param path Path that we are trying to healthcheck
   * @param candidateDynamicClientConfigs List of the dynamicClientConfigs we are healthchecking
   * @return The list of ipAddresses that actually passed the healthcheck
   */
  public ImmutableSet<String> checkEndpoints(
      String path, ImmutableList<DynamicClientConfig> candidateDynamicClientConfigs) {
    Set<String> results = new HashSet<String>();

    for (DynamicClientConfig clientConfig : candidateDynamicClientConfigs) {
      if (clientConfig.getHealthCheckPath() == null) {
        // if healthCheckPath is null we will autopass the healthcheck
        results.add(clientConfig.getIpAddress());
      } else {
        String healthCheckPath = clientConfig.getHealthCheckPath();
        checkEndpoint(healthCheckPath, clientConfig).ifPresent(value -> results.add(value));
      }
    }

    return ImmutableSet.copyOf(results);
  }

  private Optional<String> checkEndpoint(String path, DynamicClientConfig clientConfig) {
    Optional<String> result = Optional.empty();
    String ipAddress = clientConfig.getIpAddress();
    int port = clientConfig.getPort();
    boolean tlsEnabled = clientConfig.isTlsEnabled();
    try {
      Request request = clientWrapper.buildRequest(ipAddress, port, path, tlsEnabled);
      Response response = clientWrapper.executeRequest(request);
      if (response.code() == SUCCESS) {
        result = Optional.of(ipAddress);
      } else {
        log.error(
            "AppEndpointHealthCheckService healthcheck returned a non 200 response while healthchecking: {} at {}",
            path,
            ipAddress);
      }
    } catch (java.net.MalformedURLException e) {
      log.error(
          "AppEndpointHealthCheckService tried to create a bad url while healthchecking: {} at {} with exception message: {}",
          path,
          ipAddress,
          e.toString());
    } catch (java.io.IOException e) {
      log.error(
          "AppEndpointHealthCheckService healthcheck threw an exception while healthchecking: {} at {} with exception message: {}",
          path,
          ipAddress,
          e.toString());
    } catch (Exception e) {
      log.error(
          "AppEndpointHealthCheckService threw an unexpected error while healthchecking: {} at {} with exception message: {}",
          path,
          ipAddress,
          e.toString());
    } finally {
      return result;
    }
  }
}
