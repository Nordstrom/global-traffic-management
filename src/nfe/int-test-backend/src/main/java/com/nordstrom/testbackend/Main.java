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
package com.nordstrom.testbackend;

import io.bootique.Bootique;
import io.bootique.jersey.JerseyModule;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/** https://bootique.io */
public class Main {
  public static void main(String[] args) {
    Bootique.app(args)
        .module(binder -> JerseyModule.extend(binder).addResource(HelloResource.class))
        .autoLoadModules()
        .exec()
        .exit();
  }

  @Path("/api/v1/fives/")
  public static class HelloResource {
    @GET
    public String getHello() {
      return "high five GET";
    }

    @POST
    public String postHello() {
      return "high five POST";
    }
  }
}
