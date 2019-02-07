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
package com.nordstrom.nfe.nlpmanagement;

import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class NlpInstanceEntry {
  private String accountId;
  private String ipAddress;
  private List<String> paths;

  public NlpInstanceEntry(String accountId, String ipAddress, List<String> paths) {
    this.accountId = accountId;
    this.ipAddress = ipAddress;
    this.paths = paths;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public List<String> getPaths() {
    return paths;
  }
}
