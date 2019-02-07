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

import com.nordstrom.gtm.nlpdeployment.CustomerAccountNlp;
import lombok.Getter;

/**
 * This object is used to serialize and deserialize Json (using Jackson) to store in zookeeper.
 * CustomerAccountNlp is auto-generated code that also holds this information, but unfortunately
 * does not work with Jackson.
 */
@Getter
public class CustomerAccountNlpZookeeperInfo {
  private final String accountId;
  private final String instanceId;
  private final String ipAddress;

  public CustomerAccountNlpZookeeperInfo(String accountId, String instanceId, String ipAddress) {
    this.accountId = accountId;
    this.instanceId = instanceId;
    this.ipAddress = ipAddress;
  }

  public CustomerAccountNlpZookeeperInfo(CustomerAccountNlp nlp) {
    this.accountId = nlp.getAwsAccountId();
    this.instanceId = nlp.getAwsInstanceId();
    this.ipAddress = nlp.getIpAddress();
  }

  CustomerAccountNlpZookeeperInfo() {
    this.accountId = "";
    this.instanceId = "";
    this.ipAddress = "";
  }
}
