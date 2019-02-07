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

import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import com.google.common.util.concurrent.ListenableFuture;
import com.nordstrom.gatekeeper.AuthorizationResponse;
import com.nordstrom.gatekeeper.AuthorizationSuccess;
import com.xjeffrose.xio.http.DefaultFullResponse;
import com.xjeffrose.xio.http.DefaultHeaders;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.RoutePartial;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Collections;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatekeeperAuthorizer extends SimpleChannelInboundHandler<RoutePartial> {

  private final NfeState nfeState;
  private final GatekeeperClientProxy gatekeeperClientProxy;

  public GatekeeperAuthorizer(NfeState nfeState, GatekeeperClientProxy gatekeeperClientProxy) {
    this.nfeState = nfeState;
    this.gatekeeperClientProxy = gatekeeperClientProxy;
  }

  private String encodedPermissionPath(Request msg) {
    String[] parts = msg.path().split("/+");
    if (parts.length > 3) {
      // https://api.yourdomain.com/<version>/<ou-team>/<service-name>/...
      return Stream.of(parts)
          .skip(2)
          .reduce(
              new StringBuilder("apikey"),
              (sb, s) -> sb.append(':').append(s),
              StringBuilder::append)
          .toString();
    } else {
      return "*";
    }
  }

  private boolean checkIsAuthorized(ChannelHandlerContext ctx, String permission, Request request) {
    if (permission.equals("none")) {
      return true;
    }

    final ListenableFuture<AuthorizationResponse> future;

    if (permission.startsWith("apikey")) {
      String key = request.headers().get("apikey");
      if (permission.startsWith("apikey:encoded_path")) {
        permission = encodedPermissionPath(request);
        future = gatekeeperClientProxy.apiKeyAuth(ctx, key, Collections.singletonList(permission));
      } else {
        future = gatekeeperClientProxy.apiKeyAuth(ctx, key, nfeState.allPermissions());
      }
    } else {
      future = gatekeeperClientProxy.getMutualAuthResponse(ctx);
    }

    if (future == null) {
      log.debug("No Gatekeeper future authorization failed");
      return false;
    } else {
      try {
        AuthorizationResponse response = future.get();
        if (response.getResponseCase() == AuthorizationResponse.ResponseCase.SUCCESS) {
          log.debug("Gatekeeper response is a success");
          AuthorizationSuccess success = response.getSuccess();
          log.debug("Gatekeeper response permissions map: {}", success.getPermissionsMap());
          Boolean value = success.getPermissionsMap().get(permission);
          if (value != null) {
            return value;
          }
        }
      } catch (Exception e) {
        log.error("Caught Exception: ", e);
        return false;
      }
    }

    return false;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RoutePartial msg) throws Exception {
    String permission = msg.route().config().permissionNeeded();
    if (!checkIsAuthorized(ctx, permission, msg.request())) {
      String path = msg.route().path();
      log.debug("user not authorized for permission: {} at path: {}", permission, path);
      ctx.writeAndFlush(
          DefaultFullResponse.builder()
              .status(UNAUTHORIZED)
              .streamId(msg.request().streamId())
              .body(Unpooled.EMPTY_BUFFER)
              .headers(new DefaultHeaders())
              .build());
      return;
    }

    ctx.fireChannelRead(msg);
  }
}
