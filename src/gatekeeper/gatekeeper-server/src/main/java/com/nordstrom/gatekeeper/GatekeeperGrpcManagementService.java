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

import com.nordstrom.gatekeeper.grpc.*;
import io.grpc.stub.StreamObserver;
import java.util.Set;

public class GatekeeperGrpcManagementService
    extends GatekeeperManagementGrpc.GatekeeperManagementImplBase {

  private final GatekeeperDao gatekeeperDao;

  public GatekeeperGrpcManagementService(GatekeeperDao gatekeeperDao) {
    this.gatekeeperDao = gatekeeperDao;
  }

  @Override
  public void createSubjectPermissions(
      AuthZ request, StreamObserver<ChangeResult> responseObserver) {
    gatekeeperDao.createSubjectPermissions(
        request.getId(), request.getPermissionsList(), request.getRolesList());
    responseObserver.onNext(
        ChangeResult.newBuilder().setSuccess(true).setMessage("created").build());
    responseObserver.onCompleted();
  }

  @Override
  public void addSubjectPermissions(
      AuthZPermissions request, StreamObserver<ChangeResult> responseObserver) {
    String[] permissions =
        request.getPermissionsList().toArray(new String[request.getPermissionsCount()]);
    gatekeeperDao.addSubjectPermissions(request.getId(), permissions);
    responseObserver.onNext(ChangeResult.newBuilder().setSuccess(true).setMessage("added").build());
    responseObserver.onCompleted();
  }

  @Override
  public void addSubjectRoles(AuthZRoles request, StreamObserver<ChangeResult> responseObserver) {
    String[] roles = request.getRolesList().toArray(new String[request.getRolesCount()]);
    gatekeeperDao.addSubjectRoles(request.getId(), roles);
    responseObserver.onNext(ChangeResult.newBuilder().setSuccess(true).setMessage("added").build());
    responseObserver.onCompleted();
  }

  @Override
  public void removeSubjectPermissions(
      AuthZPermissions request, StreamObserver<ChangeResult> responseObserver) {
    String[] permissions =
        request.getPermissionsList().toArray(new String[request.getPermissionsCount()]);
    gatekeeperDao.removeSubjectPermissions(request.getId(), permissions);
    responseObserver.onNext(
        ChangeResult.newBuilder().setSuccess(true).setMessage("removed").build());
    responseObserver.onCompleted();
  }

  @Override
  public void removeSubjectRoles(
      AuthZRoles request, StreamObserver<ChangeResult> responseObserver) {
    String[] roles = request.getRolesList().toArray(new String[request.getRolesCount()]);
    gatekeeperDao.removeSubjectRoles(request.getId(), roles);
    responseObserver.onNext(
        ChangeResult.newBuilder().setSuccess(true).setMessage("removed").build());
    responseObserver.onCompleted();
  }

  @Override
  public void createRolePermissions(
      AuthZPermissions request, StreamObserver<ChangeResult> responseObserver) {
    String[] permissions =
        request.getPermissionsList().toArray(new String[request.getPermissionsCount()]);
    gatekeeperDao.createRolesPermissions(request.getId(), permissions);
    responseObserver.onNext(
        ChangeResult.newBuilder().setSuccess(true).setMessage("created").build());
    responseObserver.onCompleted();
  }

  @Override
  public void addRolePermissions(
      AuthZPermissions request, StreamObserver<ChangeResult> responseObserver) {
    String[] permissions =
        request.getPermissionsList().toArray(new String[request.getPermissionsCount()]);
    gatekeeperDao.addRolePermissions(request.getId(), permissions);
    responseObserver.onNext(ChangeResult.newBuilder().setSuccess(true).setMessage("added").build());
    responseObserver.onCompleted();
  }

  @Override
  public void removeRolePermissions(
      AuthZPermissions request, StreamObserver<ChangeResult> responseObserver) {
    String[] permissions =
        request.getPermissionsList().toArray(new String[request.getPermissionsCount()]);
    gatekeeperDao.removeRolePermissions(request.getId(), permissions);
    responseObserver.onNext(
        ChangeResult.newBuilder().setSuccess(true).setMessage("removed").build());
    responseObserver.onCompleted();
  }

  @Override
  public void listSubjectAuthZ(IdRequest request, StreamObserver<AuthZ> responseObserver) {
    Authorization authorization = gatekeeperDao.getSubjectAuthorization(request.getId());
    responseObserver.onNext(
        AuthZ.newBuilder()
            .setId(request.getId())
            .addAllPermissions(authorization.getPermissions())
            .addAllRoles(authorization.getRoles())
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void listRolePermissions(
      IdRequest request, StreamObserver<AuthZPermissions> responseObserver) {
    Set<String> permissions = gatekeeperDao.getRolePermissions(request.getId());
    responseObserver.onNext(
        AuthZPermissions.newBuilder()
            .setId(request.getId())
            .addAllPermissions(permissions)
            .build());
    responseObserver.onCompleted();
  }
}
