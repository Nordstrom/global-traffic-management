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
package com.nordstrom.nlp;

import com.typesafe.config.Config;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.tls.TlsConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class NlpConfig extends ApplicationConfig {
  private final NlpDeploymentConfig nlpDeploymentConfig;
  private final TlsConfig tlsConfig;
  private final ProxyRouteConfig defaultProxyRouteConfig;
  private final ClientConfig defaultClientConfig;
  private final ClientConfig noSSLDefaultClientConfig;

  public NlpConfig(Config config) {
    super(config.getConfig("nlp.application"));

    this.defaultProxyRouteConfig = new ProxyRouteConfig(config.getConfig("nlp.proxyRouteTemplate"));
    this.defaultClientConfig = ClientConfig.from(config.getConfig("nlp.clientTemplate"));
    this.noSSLDefaultClientConfig = ClientConfig.from(config.getConfig("nlp.noSSLClientTemplate"));
    this.nlpDeploymentConfig = new NlpDeploymentConfig(config.getConfig("nlp.deploymentInfo"));
    this.tlsConfig = TlsConfig.builderFrom(config.getConfig("nlp.clientTls")).build();
  }
}
