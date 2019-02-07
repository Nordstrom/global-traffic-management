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

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.operator.OperatorCreationException;

@RequiredArgsConstructor
public class KeymasterGrpcService extends KeymasterGrpc.KeymasterImplBase {
  private final CertificateGroupCollection certGroupCollection;

  @Override
  public void certificate(
      CertificateRequest request, StreamObserver<CertificateResponse> responseObserver) {
    try {
      responseObserver.onNext(handleRequest(request));
    } catch (Exception e) {
      e.printStackTrace();
    }
    responseObserver.onCompleted();
  }

  private CertificateResponse handleRequest(CertificateRequest request)
      throws OperatorCreationException, NoSuchAlgorithmException, InterruptedException,
          IOException {
    CertificateGroup parentCertGroup =
        certGroupCollection.getCertificateGroup(request.getDataClassification());
    CertificateGroup clientCertGroup =
        parentCertGroup.createClientCertificateGroup(request.getSubjectId());
    ByteString privateKey = ByteString.copyFrom(clientCertGroup.getPrivateKeyEncoded());
    if (privateKey != null && clientCertGroup.getCertChainSize() > 0) {
      CertificateSuccess.Builder certSuccessBuilder =
          CertificateSuccess.newBuilder()
              .setPrivateKey(privateKey)
              .setExpiration(clientCertGroup.getExpiration());

      // cert chain
      int indexOfLastCert = clientCertGroup.getCertChainSize() - 1;
      for (int i = 0; i <= indexOfLastCert; i++) {
        ByteString cert = ByteString.copyFrom(clientCertGroup.getCertEncoded(i));
        CertificateSuccess.CertChain.Builder certChainBuilder =
            CertificateSuccess.CertChain.newBuilder().setCertPem(cert);
        if (i == indexOfLastCert) {
          certChainBuilder.setChainLevel(CertificateSuccess.CertChain.ChainLevel.ROOT);
        } else if (i == 0) {
          certChainBuilder.setChainLevel(CertificateSuccess.CertChain.ChainLevel.LEAF);
        } else {
          certChainBuilder.setChainLevel(CertificateSuccess.CertChain.ChainLevel.INTERMEDIATE);
        }
        certSuccessBuilder.addCertChain(certChainBuilder.build());
      }

      return CertificateResponse.newBuilder().setSuccess(certSuccessBuilder).build();
    } else {
      return CertificateResponse.newBuilder()
          .setError(CertificateError.newBuilder().setMessage("Could not certify request"))
          .build();
    }
  }
}
