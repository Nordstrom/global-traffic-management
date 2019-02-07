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

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GatekeeperGrpcService extends GatekeeperGrpc.GatekeeperImplBase {

  private final Authorizer authZ;

  public GatekeeperGrpcService(Authorizer authZ) {
    this.authZ = authZ;
  }

  @Override
  public void authorize(
      AuthorizationRequest request, StreamObserver<AuthorizationResponse> responseObserver) {
    responseObserver.onNext(handleRequest(request));
    responseObserver.onCompleted();
  }

  private AuthorizationResponse handleRequest(AuthorizationRequest request) {
    Instant now = Instant.now();
    Instant ttl = now.plusSeconds(Constants.AUTHZ_DURATION.getSeconds());
    Timestamp.Builder timestamp = Timestamp.newBuilder().setSeconds(ttl.getEpochSecond());
    final List<String> permissionsRequested;
    if (request.getMultiple().getPermissionCount() > 1) {
      permissionsRequested =
          IntStream.range(0, request.getMultiple().getPermissionCount())
              .mapToObj(i -> request.getMultiple().getPermission(i))
              .collect(Collectors.toList());

    } else {
      permissionsRequested = Collections.singletonList(request.getSingle().getPermission());
    }

    return authZ
        .authorize(request.getSubjectId(), permissionsRequested)
        .map(
            authz ->
                AuthorizationResponse.newBuilder()
                    .setSuccess(
                        AuthorizationSuccess.newBuilder()
                            .putAllPermissions(authz)
                            .setCacheTtl(timestamp))
                    .build())
        .orElseGet(
            () ->
                AuthorizationResponse.newBuilder()
                    .setError(
                        AuthorizationError.newBuilder().setMessage("Could not authorize request"))
                    .build());
  }
}
