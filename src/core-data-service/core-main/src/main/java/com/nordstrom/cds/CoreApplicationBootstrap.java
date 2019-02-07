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

import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;

public class CoreApplicationBootstrap extends ApplicationBootstrap {
  private CoreApplicationState state;

  public CoreApplicationBootstrap(CoreApplicationState state) {
    super(state);
    this.state = state;
  }

  private SmartHttpPipeline pipelineFragment() {
    return new SmartHttpPipeline() {

      @Override
      public ChannelHandler getTlsAuthenticationHandler() {
        return null;
      }

      @Override
      public ChannelHandler getApplicationRouter() {
        return new PipelineRouter(state.routes());
      }

      @Override
      public ChannelHandler getAuthenticationHandler() {
        return null;
      }

      @Override
      public ChannelHandler getAuthorizationHandler() {
        return null;
      }
    };
  }

  public Application build() {
    addServer(
        "cds-main", xioServerBootstrap -> xioServerBootstrap.addToPipeline(pipelineFragment()));
    return super.build();
  }
}
