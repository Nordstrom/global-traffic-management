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
package com.nordstrom.nfe.config;

import com.typesafe.config.Config;

public class NlpSharedCountConfig {
  public int attemptsToStartMax;
  public int attemptsToUpdateMax;

  public NlpSharedCountConfig(int attemptsToStartMax, int attemptsToUpdateMax) {
    this.attemptsToStartMax = attemptsToStartMax;
    this.attemptsToUpdateMax = attemptsToUpdateMax;
  }

  public NlpSharedCountConfig(Config config) {
    this.attemptsToStartMax = config.getInt("attemptsToStartMax");
    this.attemptsToUpdateMax = config.getInt("attemptsToUpdateMax");
  }
}
