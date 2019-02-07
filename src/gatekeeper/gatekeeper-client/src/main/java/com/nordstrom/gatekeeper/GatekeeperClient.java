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

import com.google.common.util.concurrent.ListenableFuture;
import com.nordstrom.gatekeeper.GatekeeperGrpc.GatekeeperFutureStub;
import com.nordstrom.gatekeeper.grpc.*;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.tls.SslContextFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GatekeeperClient {

  private final String host;
  private final int port;
  private final TlsConfig tlsConfig;

  private GatekeeperFutureStub gatekeeperFutureStub;
  private GatekeeperManagementGrpc.GatekeeperManagementFutureStub gatekeeperManagementFutureStub;
  private ManagedChannel channel;

  public GatekeeperClient(String host, int port) {
    this(host, port, "tls.conf");
  }

  public GatekeeperClient(String host, int port, TlsConfig tlsConfig) {
    this.host = host;
    this.port = port;
    this.tlsConfig = tlsConfig;
  }

  public GatekeeperClient(String host, int port, String tlsConfigPath) {
    this(host, port, TlsConfig.builderFrom(ConfigFactory.load(tlsConfigPath)).build());
  }

  private ManagedChannel getChannel() {
    if (channel == null) {
      channel =
          NettyChannelBuilder.forAddress(host, port)
              .negotiationType(NegotiationType.TLS)
              .sslContext(SslContextFactory.buildClientContext(tlsConfig))
              .build();
    }
    return channel;
  }

  private GatekeeperFutureStub getFutureStub() {
    if (gatekeeperFutureStub == null) {
      gatekeeperFutureStub = GatekeeperGrpc.newFutureStub(getChannel());
    }
    return gatekeeperFutureStub;
  }

  private GatekeeperManagementGrpc.GatekeeperManagementFutureStub getManagementFutureStub() {
    if (gatekeeperManagementFutureStub == null) {
      gatekeeperManagementFutureStub = GatekeeperManagementGrpc.newFutureStub(getChannel());
    }
    return gatekeeperManagementFutureStub;
  }

  public static AuthorizationRequest buildRequest(String subjectId, List<String> permissions) {
    AuthorizationRequest.Builder builder = AuthorizationRequest.newBuilder();
    if (permissions.size() == 1) {
      builder.setSingle(SinglePermission.newBuilder().setPermission(permissions.get(0)));
    } else if (permissions.size() > 1) {
      builder.setMultiple(MultiplePermissions.newBuilder().addAllPermission(permissions));
    }
    return builder.setSubjectId(subjectId).build();
  }

  public AuthorizationResponse authorize(String subjectId, String... permissions)
      throws ExecutionException, InterruptedException {
    return authorize(subjectId, Stream.of(permissions).collect(Collectors.toList()));
  }

  public AuthorizationResponse authorize(String subjectId, List<String> permissions)
      throws ExecutionException, InterruptedException {
    return getFutureStub().authorize(buildRequest(subjectId, permissions)).get();
  }

  public ListenableFuture<AuthorizationResponse> authorizeAsync(
      String subjectId, List<String> permissions) {
    return getFutureStub().authorize(buildRequest(subjectId, permissions));
  }

  public ChangeResult createSubjectPermissions(String subjectId, String... permissions)
      throws ExecutionException, InterruptedException {
    return createSubjectPermissions(
        subjectId, Stream.of(permissions).collect(Collectors.toList()), Collections.emptyList());
  }

  public ChangeResult createSubjectPermissions(
      String subjectId, Iterable<String> permissions, Iterable<String> roles)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .createSubjectPermissions(
            AuthZ.newBuilder()
                .setId(subjectId)
                .addAllPermissions(permissions)
                .addAllRoles(roles)
                .build())
        .get();
  }

  public ChangeResult addSubjectPermissions(String subjectId, String... permissions)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .addSubjectPermissions(
            AuthZPermissions.newBuilder()
                .setId(subjectId)
                .addAllPermissions(Stream.of(permissions).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult addSubjectRoles(String subjectId, String... roles)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .addSubjectRoles(
            AuthZRoles.newBuilder()
                .setId(subjectId)
                .addAllRoles(Stream.of(roles).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult removeSubjectPermissions(String subjectId, String... permissions)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .removeSubjectPermissions(
            AuthZPermissions.newBuilder()
                .setId(subjectId)
                .addAllPermissions(Stream.of(permissions).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult removeSubjectRoles(String subjectId, String... roles)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .removeSubjectRoles(
            AuthZRoles.newBuilder()
                .setId(subjectId)
                .addAllRoles(Stream.of(roles).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult createRolePermissions(String roleId, String... permissions)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .createRolePermissions(
            AuthZPermissions.newBuilder()
                .setId(roleId)
                .addAllPermissions(Stream.of(permissions).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult addRolePermissions(String roleId, String... permissions)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .addRolePermissions(
            AuthZPermissions.newBuilder()
                .setId(roleId)
                .addAllPermissions(Stream.of(permissions).collect(Collectors.toList()))
                .build())
        .get();
  }

  public ChangeResult removeRolePermissions(String roleId, String... permissions)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .removeRolePermissions(
            AuthZPermissions.newBuilder()
                .setId(roleId)
                .addAllPermissions(Stream.of(permissions).collect(Collectors.toList()))
                .build())
        .get();
  }

  public AuthZ listSubjectAuthZ(String subjectId) throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .listSubjectAuthZ(IdRequest.newBuilder().setId(subjectId).build())
        .get();
  }

  public AuthZPermissions listRolePermissions(String roleId)
      throws ExecutionException, InterruptedException {
    return getManagementFutureStub()
        .listRolePermissions(IdRequest.newBuilder().setId(roleId).build())
        .get();
  }

  public void stop() throws InterruptedException {
    if (channel != null) {
      channel.shutdownNow();
      channel.awaitTermination(10, TimeUnit.SECONDS);
    }
  }
}
