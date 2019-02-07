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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Constants {
  private Constants() {}

  public static final long AUTHZ_DB_CACHE_TIME = 15;
  public static final TimeUnit AUTHZ_DB_CACHE_TIME_UNIT = TimeUnit.SECONDS;
  public static final Duration AUTHZ_DURATION = Duration.ofMinutes(15);
}
