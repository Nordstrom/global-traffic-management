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

import java.util.Arrays;
import javax.net.ssl.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/*
 This class is used to build the OkHttpClientWrapper used for healthchecking
*/
@Slf4j
public class OkHttpClientWrapperFactory {

  /*
   This is the factory method that should be used
  */
  public static OkHttpClientWrapper createClientWrapper() {
    OkHttpClient rawClient =
        new OkHttpClient.Builder()
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build();
    return new OkHttpClientWrapper(rawClient, new RequestBuilderFactory());
  }
}
