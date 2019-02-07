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

import static io.grpc.Status.ALREADY_EXISTS;
import static io.grpc.Status.UNAVAILABLE;

import com.google.common.collect.Lists;
import com.google.protobuf.Empty;
import com.google.rpc.Status;
import com.nordstrom.gtm.apikey.ApiKey;
import com.nordstrom.gtm.apikey.KeyRequest;
import com.xjeffrose.xio.grpc.GrpcRoute;
import com.xjeffrose.xio.grpc.GrpcService;
import com.xjeffrose.xio.http.GrpcRequestHandler;
import io.grpc.protobuf.StatusProto;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiKeyGrpcService implements GrpcService {
  private static final String GENERATE_NEW_API_KEY = "GenerateNewApiKey";
  private static final String REVOKE_API_KEY = "RevokeApiKey";
  private final GatekeeperClientProxy gatekeeperClient;

  public ApiKeyGrpcService(GatekeeperClientProxy gatekeeperClient) {
    this.gatekeeperClient = gatekeeperClient;
  }

  @Override
  public String getPackageName() {
    return "nordstrom.gtm.apikey";
  }

  @Override
  public String getServiceName() {
    return "ApiKeyer";
  }

  @Override
  public List<GrpcRoute> getRoutes() {
    return Collections.unmodifiableList(
        Lists.newArrayList(generateNewApiKeyRoute(), revokeApiKeyRoute()));
  }

  private GrpcRoute generateNewApiKeyRoute() {
    return new GrpcRoute(this, GENERATE_NEW_API_KEY, generateNewApiKeyHandler());
  }

  private GrpcRoute revokeApiKeyRoute() {
    return new GrpcRoute(this, REVOKE_API_KEY, revokeApiKeyHandler());
  }

  private GrpcRequestHandler<KeyRequest, ApiKey> generateNewApiKeyHandler() {

    return new GrpcRequestHandler<>(
        KeyRequest::parseFrom,
        request -> {
          boolean keyExists = false; // todo: (WK) (BR) check coredb for key existence
          if (keyExists) {
            throw StatusProto.toStatusException(
                Status.newBuilder()
                    .setCode(ALREADY_EXISTS.getCode().value())
                    .setMessage(
                        String.format(
                            "%s.%s.%s already in use",
                            request.getTeamName(), request.getServiceName(), request.getKeyName()))
                    .build());
          } else {
            ApiKey response =
                ApiKey.newBuilder()
                    .setTeamName(request.getTeamName())
                    .setKey(UUID.randomUUID().toString())
                    .setServiceName(request.getServiceName())
                    .setKeyName(request.getKeyName())
                    .build();
            try {
              gatekeeperClient.createSubjectPermissions(
                  response.getKey(), apiKeyAsPermission(response));
            } catch (ExecutionException | InterruptedException e) {
              log.error("error creating generated api key permission in gatekeeper", e);
              throw StatusProto.toStatusException(
                  Status.newBuilder()
                      .setCode(UNAVAILABLE.getCode().value())
                      .setMessage("gatekeeper error")
                      .build());
            }
            return response;
          }
        });
  }

  private String apiKeyAsPermission(ApiKey apiKey) {
    return String.format("apikey:%s:%s:*", apiKey.getTeamName(), apiKey.getServiceName())
        .toLowerCase();
  }

  private GrpcRequestHandler<ApiKey, Empty> revokeApiKeyHandler() {
    Empty response = Empty.newBuilder().build();
    return new GrpcRequestHandler<>(
        ApiKey::parseFrom,
        revokeKey -> {
          // todo: (WK) (BR) delete key from coredb
          try {
            gatekeeperClient.removeSubjectPermissions(
                revokeKey.getKey(), apiKeyAsPermission(revokeKey));
          } catch (ExecutionException | InterruptedException e) {
            log.error("error revoking api key permission in gatekeeper", e);
            throw StatusProto.toStatusException(
                Status.newBuilder()
                    .setCode(UNAVAILABLE.getCode().value())
                    .setMessage("gatekeeper error")
                    .build());
          }
          return response;
        });
  }
}
