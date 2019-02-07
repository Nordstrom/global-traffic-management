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

import com.google.protobuf.Timestamp;
import com.nordstrom.keymaster.CertificateRequest.DataClassification;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

@ParametersAreNonnullByDefault
@Slf4j
public class CertificateGroup {
  private final KeymasterDao keymasterDao;
  private final ArrayList<X509CertificateHolder> certChain;
  private final RSAPrivateKey privateKey;
  private final DataClassification dataClassification;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public RSAPrivateKey getPrivateKey() {
    return privateKey;
  }

  public Timestamp getExpiration() {
    Date expirationDate = this.certChain.get(0).getNotAfter();
    Instant instant = expirationDate.toInstant();
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  public CertificateGroup(
      DataClassification dataClassification,
      KeymasterDao keymasterDao,
      ArrayList<X509CertificateHolder> certChain,
      RSAPrivateKey privateKey) {
    this.dataClassification = dataClassification;
    this.keymasterDao = keymasterDao;
    this.certChain = certChain;
    this.privateKey = privateKey;
  }

  public CertificateGroup(
      DataClassification dataClassification,
      KeymasterDao keymasterDao,
      String certChainFile,
      String keyFile)
      throws IOException {
    this.dataClassification = dataClassification;
    this.keymasterDao = keymasterDao;

    try (FileInputStream keyFileStream = new FileInputStream(keyFile)) {
      PEMParser keyParser = new PEMParser(new InputStreamReader(keyFileStream));
      PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) keyParser.readObject();
      this.privateKey = (RSAPrivateKey) BouncyCastleProvider.getPrivateKey(privateKeyInfo);
    }

    try (FileInputStream certChainFileStream = new FileInputStream(certChainFile)) {
      PEMParser certChainParser = new PEMParser(new InputStreamReader(certChainFileStream));
      this.certChain = new ArrayList<>();
      Object object = certChainParser.readObject();
      while (object != null) {
        if (object instanceof X509CertificateHolder) {
          this.certChain.add((X509CertificateHolder) object);
          object = certChainParser.readObject();
        } else {
          log.debug("invalid cert chain file received");
          this.certChain.clear();
          break;
        }
      }
    }
  }

  // TODO(JL): Pass in duration as a parameter
  public CertificateGroup createClientCertificateGroup(String subjectId)
      throws NoSuchAlgorithmException, OperatorCreationException, InterruptedException {
    if (subjectId.equals("")) {
      log.debug("invalid subject id received");
      return null;
    }
    // TODO(JL): Temporary defaults that need to be pulled from elsewhere
    int keySize = 1024;
    Duration certDuration = Duration.ofMinutes(15);

    // Client key pair
    KeyPair subjectKeyPair = generateKeyPair(keySize);
    PublicKey subjectPublicKey = subjectKeyPair.getPublic();
    RSAPrivateKey subjectPrivateKey = (RSAPrivateKey) subjectKeyPair.getPrivate();

    // Client cert holder
    BigInteger serialNumber =
        this.keymasterDao.handleNewKeypair(subjectId, this.dataClassification);

    Instant currentTime = Instant.now();
    Date notBeforeDate = calculateDate(currentTime, Duration.ofSeconds(0));
    Date notAfterDate = calculateDate(currentTime, certDuration);

    X500NameBuilder x500SubjectBuilder =
        new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, subjectId);
    X500Name subjectName = x500SubjectBuilder.build();
    X500Name issuerName = this.certChain.get(0).getSubject();

    X509v3CertificateBuilder certBuilder =
        new JcaX509v3CertificateBuilder(
            issuerName, serialNumber, notBeforeDate, notAfterDate, subjectName, subjectPublicKey);
    String signatureAlgorithm = "SHA256withRSA";
    ContentSigner signer =
        new JcaContentSignerBuilder(signatureAlgorithm)
            .setProvider("BC")
            .build(this.getPrivateKey());
    X509CertificateHolder clientCert = certBuilder.build(signer);

    // Client cert chain
    ArrayList<X509CertificateHolder> clientCertChain = this.getPushedCertChain(clientCert);
    return new CertificateGroup(
        this.dataClassification, this.keymasterDao, clientCertChain, subjectPrivateKey);
  }

  public byte[] getCertEncoded(int index) throws IOException {
    if (index > 0 || index < getCertChainSize()) {
      String certString = this.getCertString(index);
      return certString.getBytes(StandardCharsets.UTF_8);
    } else {
      return null;
    }
  }

  public String getCertString(int index) throws IOException {
    if (index > 0 || index < getCertChainSize()) {
      StringWriter stringWriter = new StringWriter();
      try (PemWriter certPemWriter = new PemWriter(stringWriter)) {
        Certificate cert = this.certChain.get(index).toASN1Structure();
        byte[] certBytes = cert.getEncoded();
        certPemWriter.writeObject(new PemObject("CERTIFICATE", certBytes));
      }
      return stringWriter.toString();
    } else {
      return null;
    }
  }

  public int getCertChainSize() {
    return this.certChain.size();
  }

  public byte[] getPrivateKeyEncoded() throws IOException {
    String privateKeyString = this.getPrivateKeyString();
    return privateKeyString.getBytes(StandardCharsets.UTF_8);
  }

  public String getPrivateKeyString() throws IOException {
    StringWriter stringWriter = new StringWriter();
    try (PemWriter keyPemWriter = new PemWriter(stringWriter)) {
      byte[] privateKeyBytes = this.privateKey.getEncoded();

      // Convert to PrivateKeyInfo first
      PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKeyBytes);
      ASN1Encodable privateKeyEncodable = privateKeyInfo.parsePrivateKey();
      ASN1Primitive privateKeyPrimitive = privateKeyEncodable.toASN1Primitive();
      byte[] privateKeyPrimitiveBytes = privateKeyPrimitive.getEncoded();
      keyPemWriter.writeObject(new PemObject("RSA PRIVATE KEY", privateKeyPrimitiveBytes));
    }
    return stringWriter.toString();
  }

  private KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    SecureRandom secureRandom = new SecureRandom();
    keyPairGenerator.initialize(keySize, secureRandom);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair;
  }

  private Date calculateDate(Instant currentTime, Duration duration) {
    return Date.from(currentTime.plus(duration));
  }

  private ArrayList<X509CertificateHolder> getPushedCertChain(X509CertificateHolder cert) {
    ArrayList<X509CertificateHolder> newCertChain = new ArrayList<>(this.certChain);
    newCertChain.add(0, cert);
    return newCertChain;
  }
}
