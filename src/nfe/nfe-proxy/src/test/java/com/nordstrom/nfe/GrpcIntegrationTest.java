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

import static com.nordstrom.apikey.Result.resultFromFuture;
import static org.junit.Assert.assertNotNull;

import com.google.protobuf.Empty;
import com.nordstrom.apikey.GrpcClient;
import com.nordstrom.apikey.Result;
import com.nordstrom.gtm.nlpdeployment.CustomerAccountNlp;
import com.nordstrom.gtm.nlpdeployment.NlpDeploymentGrpc;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationRequest;
import com.nordstrom.gtm.serviceregistration.CreateServiceRegistrationResponse;
import com.nordstrom.gtm.serviceregistration.ServiceRegistrationGrpc;
import com.nordstrom.nfe.bootstrap.NfeApplicationBootstrap;
import com.nordstrom.nfe.config.NfeConfig;
import com.nordstrom.nfe.testhelpers.MockExternalCoreDataService;
import com.nordstrom.nfe.testhelpers.RunnableWithException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import com.xjeffrose.xio.application.Application;
import io.grpc.ManagedChannel;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// This smoke tests a few gRPC calls to ensure all the objects are correctly coordinating with each
// other.
public class GrpcIntegrationTest {
  private List<RunnableWithException> afterEachBlocks;

  @Before
  public void beforeEach() throws Exception {
    afterEachBlocks = new ArrayList<>();
  }

  @After
  public void afterEach() throws Exception {
    for (RunnableWithException afterEachBlock : afterEachBlocks) {
      afterEachBlock.run();
    }

    afterEachBlocks = null;
  }

  @Test
  public void testRegisterServiceCall() throws Exception {
    // Get CoreDataService up and running
    MockExternalCoreDataService coreDataService = new MockExternalCoreDataService();
    coreDataService.start();
    coreDataService.setCreateServiceRegistrationLogic(
        request -> CreateServiceRegistrationResponse.newBuilder().build());
    afterEachBlocks.add(coreDataService::stop);

    // Get NFE up and running
    Config config =
        ConfigFactory.load()
            .withValue("nfe.coreDatabase.host", ConfigValueFactory.fromAnyRef("127.0.0.1"))
            .withValue(
                "nfe.coreDatabase.port", ConfigValueFactory.fromAnyRef(coreDataService.getPort()));

    NfeConfig nfeConfig = new NfeConfig(config);
    NfeState nfeState = new NfeState(nfeConfig);
    Application application = new NfeApplicationBootstrap(nfeState).build();
    InetSocketAddress socketAddress = application.instrumentation("nfe-main").boundAddress();
    afterEachBlocks.add(application::close);

    // Get gRPC client up and running
    ManagedChannel clientChannel = GrpcClient.buildChannel("127.0.0.1", socketAddress.getPort());
    ServiceRegistrationGrpc.ServiceRegistrationFutureStub futureStub =
        ServiceRegistrationGrpc.newFutureStub(clientChannel);
    GrpcClient client = GrpcClient.run(clientChannel);
    afterEachBlocks.add(client::shutdown);

    // Make a basic service registration call
    CreateServiceRegistrationRequest createServiceRegistrationRequest =
        CreateServiceRegistrationRequest.getDefaultInstance();
    Result<CreateServiceRegistrationResponse> result =
        resultFromFuture(
            () -> futureStub.createServiceRegistration(createServiceRegistrationRequest));

    // Ensure that a gRPC response was returned
    assertNotNull(result.getValue());
  }

  @Test
  public void testDeployedNlpCall() throws Exception {
    // Get NFE up and running
    Config config = ConfigFactory.load();
    NfeConfig nfeConfig = new NfeConfig(config);
    NfeState nfeState = new NfeState(nfeConfig);
    Application application = new NfeApplicationBootstrap(nfeState).build();
    InetSocketAddress socketAddress = application.instrumentation("nfe-main").boundAddress();
    afterEachBlocks.add(application::close);

    // Get gRPC client up and running
    ManagedChannel clientChannel = GrpcClient.buildChannel("127.0.0.1", socketAddress.getPort());
    NlpDeploymentGrpc.NlpDeploymentFutureStub futureStub =
        NlpDeploymentGrpc.newFutureStub(clientChannel);
    GrpcClient client = GrpcClient.run(clientChannel);
    afterEachBlocks.add(client::shutdown);

    // Make a basic service registration call
    CustomerAccountNlp registerNlpRequest =
        CustomerAccountNlp.newBuilder()
            .setAwsAccountId("abc")
            .setAwsInstanceId("123")
            .setIpAddress("1.2.3.4:5678")
            .build();
    Result<Empty> result = resultFromFuture(() -> futureStub.deployedNlp(registerNlpRequest));

    // Ensure that a gRPC response was returned
    assertNotNull(result.getValue());
  }
}
