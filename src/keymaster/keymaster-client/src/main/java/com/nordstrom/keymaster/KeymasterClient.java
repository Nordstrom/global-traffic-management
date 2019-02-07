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
package com.nordstrom.keymaster;

import com.nordstrom.keymaster.CertificateRequest.DataClassification;
import com.nordstrom.keymaster.KeymasterGrpc.KeymasterFutureStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KeymasterClient {

  private final String host;
  private final int port;

  private KeymasterFutureStub keymasterFutureStub;
  private ManagedChannel channel;

  private ManagedChannel getChannel() {
    if (channel == null) {
      channel =
          ManagedChannelBuilder.forAddress(host, port)
              .usePlaintext(true) // TODO(CK): use ssl
              .build();
    }
    return channel;
  }

  private KeymasterFutureStub getFutureStub() {
    if (keymasterFutureStub == null) {
      keymasterFutureStub = KeymasterGrpc.newFutureStub(getChannel());
    }
    return keymasterFutureStub;
  }

  public static CertificateRequest buildRequest(
      String subjectId, DataClassification dataClassification) {
    CertificateRequest.Builder builder =
        CertificateRequest.newBuilder()
            .setSubjectId(subjectId)
            .setDataClassification(dataClassification);
    return builder.build();
  }

  public CertificateResponse certificate(String subjectId, DataClassification dataClassification)
      throws ExecutionException, InterruptedException {
    return getFutureStub().certificate(buildRequest(subjectId, dataClassification)).get();
  }

  public void stop() throws InterruptedException {
    if (channel != null) {
      channel.shutdownNow();
      channel.awaitTermination(10, TimeUnit.SECONDS);
    }
  }
}
