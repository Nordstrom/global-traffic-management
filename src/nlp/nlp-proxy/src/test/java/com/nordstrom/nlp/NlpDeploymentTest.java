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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.protobuf.Empty;
import com.nordstrom.gtm.nlpdeployment.CustomerAccountNlp;
import com.nordstrom.gtm.nlpdeployment.NlpDeploymentGrpc;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.xjeffrose.xio.tls.SslContextFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NlpDeploymentTest {
  private final String expectedAwsAccountId = "123456789012";
  private final String expectedAwsInstanceId = "i-1234567890abcdefg";
  private final String expectedIpAddress = "12.34.56.78";

  private TestNfeServer nfeServer;
  private NlpConfig nlpConfig;
  private MockWebServer awsServer;

  private MockWebServer makeAwsServer() {

    Dispatcher dispatcher =
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if (request.getPath().equals("/latest/dynamic/instance-identity/document")) {
              JsonObject jsonObject = new JsonObject();
              jsonObject.addProperty("accountId", expectedAwsAccountId);
              jsonObject.addProperty("instanceId", expectedAwsInstanceId);

              return new MockResponse().setBody(jsonObject.toString()).setResponseCode(200);
            } else if (request.getPath().equals("/latest/meta-data/public-ipv4")) {
              return new MockResponse().setBody(expectedIpAddress).setResponseCode(200);
            }

            return new MockResponse().setResponseCode(404);
          }
        };

    MockWebServer server = new MockWebServer();
    server.setDispatcher(dispatcher);

    return server;
  }

  @Before
  public void beforeEach() throws Exception {
    Config topLevelConfig = ConfigFactory.load();

    nfeServer = new TestNfeServer(9797, topLevelConfig);
    nfeServer.start();

    awsServer = makeAwsServer();
    awsServer.start(8888);

    // TODO(br): setup MockWebSerer

    HashMap<String, Object> configMap = new HashMap<>();
    configMap.put("host", "127.0.0.1");
    configMap.put("port", 9797);
    configMap.put("awsInfoUri", "http://127.0.0.1:8888");

    Config config =
        topLevelConfig.withValue("nlp.deploymentInfo", ConfigValueFactory.fromMap(configMap));
    nlpConfig = new NlpConfig(config);
  }

  @After
  public void afterEach() throws Exception {
    nfeServer.stop();
    awsServer.shutdown();
  }

  @Test
  public void testPhoneHome() throws Exception {
    // Configure the nfeServer to succeed the call
    nfeServer.shouldSucceedCall = true;

    // Tell NlpDeployment to phone home (in the real world this is done in Main.main())
    NlpDeployment subject = new NlpDeployment();
    ListenableFuture<Empty> future = subject.phoneHome(nlpConfig);

    // Sanity Check / Is the connection settings correct: Did we get a response from the nfeServer
    Empty response = future.get(1000, TimeUnit.MILLISECONDS);
    assertNotNull(response);

    // Did we send the correct request
    CustomerAccountNlp actualCustomerAccountNlp = nfeServer.lastRequestRecieved;
    assertNotNull(actualCustomerAccountNlp);

    assertEquals(expectedIpAddress, actualCustomerAccountNlp.getIpAddress());
    assertEquals(expectedAwsInstanceId, actualCustomerAccountNlp.getAwsInstanceId());
    assertEquals(expectedAwsAccountId, actualCustomerAccountNlp.getAwsAccountId());
  }
}

class TestNfeServer extends NlpDeploymentGrpc.NlpDeploymentImplBase {
  boolean shouldSucceedCall = false;
  CustomerAccountNlp lastRequestRecieved = null;

  private Server grpcServer;

  TestNfeServer(int port, Config topLevelConfig) {
    TlsConfig tlsConfig =
        TlsConfig.builderFrom(topLevelConfig.getConfig("xio.serverSettings.tls")).build();
    SslContext sslContext =
        SslContextFactory.buildServerContext(tlsConfig, InsecureTrustManagerFactory.INSTANCE);
    this.grpcServer =
        NettyServerBuilder.forPort(port).sslContext(sslContext).addService(this).build();
  }

  void start() {
    try {
      grpcServer.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void stop() {
    grpcServer.shutdownNow();
  }

  @Override
  public void deployedNlp(CustomerAccountNlp request, StreamObserver<Empty> responseObserver) {
    lastRequestRecieved = request;

    if (shouldSucceedCall) {
      Empty response = Empty.getDefaultInstance();
      responseObserver.onNext(response);
    } else {
      Status status = Status.INTERNAL.withDescription("bad things happened");
      responseObserver.onError(new StatusException(status));
    }

    responseObserver.onCompleted();
  }
}
