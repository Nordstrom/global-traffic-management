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

import com.google.common.util.concurrent.ListenableFuture;
import com.nordstrom.gatekeeper.AuthorizationResponse;
import com.nordstrom.gatekeeper.GatekeeperClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class GatekeeperClientProxy extends GatekeeperClient {

  private static final AttributeKey<ListenableFuture<AuthorizationResponse>> RESPONSE_KEY =
      AttributeKey.newInstance("nfe_gatekeeper_response_key");

  private static final AttributeKey<Map<String, ListenableFuture<AuthorizationResponse>>>
      API_KEY_RESPONSE_KEY = AttributeKey.newInstance("nfe_gatekeeper_apikey_response_key");

  public GatekeeperClientProxy(String host, int port) {
    super(host, port);
  }

  /**
   * Get the api key response for a given api key.
   *
   * @param ctx the channel context where to cache responses.
   * @param apiKey the api key.
   * @return the future authentication response or null.
   */
  @Nullable
  ListenableFuture<AuthorizationResponse> apiKeyAuth(
      ChannelHandlerContext ctx, @Nullable String apiKey, List<String> permissions) {
    ListenableFuture<AuthorizationResponse> future = getApiKeyCachedChannelResponse(ctx, apiKey);
    if (future == null && apiKey != null) {
      future = authorizeAsync(apiKey, permissions);
      getCachedChannelApiKeyResponses(ctx).put(apiKey, future);
    }
    return future;
  }

  /**
   * Get the mutual auth future response.
   *
   * @param ctx the channel context where the response is stored.
   * @return the future authentication response.
   */
  @Nullable
  ListenableFuture<AuthorizationResponse> getMutualAuthResponse(ChannelHandlerContext ctx) {
    return ctx.channel().attr(RESPONSE_KEY).get();
  }

  /**
   * Perform the gatekeeper mutual authentication request storing the response on the channel
   * attributes.
   *
   * @param ctx the channel context to store the response future.
   * @param subjectId the client TLS peer identity.
   * @param permissions the permissions requested.
   */
  public void mutualAuth(ChannelHandlerContext ctx, String subjectId, List<String> permissions) {
    ctx.channel().attr(RESPONSE_KEY).set(authorizeAsync(subjectId, permissions));
  }

  /**
   * Get the cached channel api key response for a given api key.
   *
   * @param ctx the channel context where the responses are stored.
   * @param apiKey the api key.
   * @return the future authentication response or null if one has not been performed.
   */
  @Nullable
  private ListenableFuture<AuthorizationResponse> getApiKeyCachedChannelResponse(
      ChannelHandlerContext ctx, @Nullable String apiKey) {
    return getCachedChannelApiKeyResponses(ctx).get(apiKey);
  }

  /**
   * Get the cached channel api key responses.
   *
   * @param ctx the channel context where the responses are stored.
   * @return a map of apikey to future responses.
   */
  private Map<String, ListenableFuture<AuthorizationResponse>> getCachedChannelApiKeyResponses(
      ChannelHandlerContext ctx) {
    Map<String, ListenableFuture<AuthorizationResponse>> responses =
        ctx.channel().attr(API_KEY_RESPONSE_KEY).get();
    if (responses == null) {
      responses = new HashMap<>();
      ctx.channel().attr(API_KEY_RESPONSE_KEY).set(responses);
    }
    return responses;
  }
}
