package com.nordstrom.keymaster;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.google.protobuf.Timestamp;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class CertificateGroup {
  private final X509Certificate[] certChain;
  private final X509CertificateHolder certHolder;
  private final RSAPrivateKey privateKey;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  public X509Certificate[] getCertChain() {
    return certChain;
  }

  public X509CertificateHolder getCertHolder() {
    return certHolder;
  }

  public RSAPrivateKey getPrivateKey() {
    return privateKey;
  }

  public Timestamp getExpiration() {
    Date expirationDate = this.certHolder.getNotAfter();
    Instant instant = expirationDate.toInstant();
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  public CertificateGroup(
      X509Certificate[] certChain, X509CertificateHolder certHolder, RSAPrivateKey privateKey) {
    // TODO(JL): Error handling for when these values are passed in as null?
    this.certChain = certChain;
    this.certHolder = certHolder;
    this.privateKey = privateKey;
  }

  public CertificateGroup(String certFile, String keyFile)
      throws IOException, CertificateException {
    try (FileInputStream keyFileStream = new FileInputStream(keyFile)) {
      PEMParser keyParser = new PEMParser(new InputStreamReader(keyFileStream));
      PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) keyParser.readObject();
      this.privateKey = (RSAPrivateKey) BouncyCastleProvider.getPrivateKey(privateKeyInfo);
    }

    try (FileInputStream certFileStream = new FileInputStream(certFile)) {
      PEMParser certParser = new PEMParser(new InputStreamReader(certFileStream));
      this.certHolder = (X509CertificateHolder) certParser.readObject();

      X509Certificate certificate =
          new JcaX509CertificateConverter().setProvider("BC").getCertificate(this.certHolder);
      this.certChain = new X509Certificate[] {certificate};
    }
  }

  // TODO(JL): Pass in Duration for Expiration/notAfterDate
  public CertificateGroup createClientCertificateGroup(String subjectId)
      throws NoSuchAlgorithmException, OperatorCreationException, CertificateException {
    // TODO(JL): Temporary defaults that need to be pulled from elsewhere
    int keySize = 1024;
    Duration certDuration = Duration.ofMinutes(15);

    // Client key pair
    KeyPair subjectKeyPair = generateKeyPair(keySize);
    PublicKey subjectPublicKey = subjectKeyPair.getPublic();
    RSAPrivateKey subjectPrivateKey = (RSAPrivateKey) subjectKeyPair.getPrivate();

    // Client cert holder
    IFn createKeyPairEntries = Clojure.var("keymaster.storage", "create-keypair-entries");
    BigInteger serialNumber = (BigInteger) createKeyPairEntries.invoke(subjectId);

    Instant currentTime = Instant.now();
    Date notBeforeDate = calculateDate(currentTime, Duration.ofSeconds(0));
    Date notAfterDate = calculateDate(currentTime, certDuration);

    X500NameBuilder x500SubjectBuilder =
        new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, subjectId);
    X500Name subjectName = x500SubjectBuilder.build();
    X500Name issuerName = this.certHolder.getSubject();

    X509v3CertificateBuilder certBuilder =
        new JcaX509v3CertificateBuilder(
            issuerName, serialNumber, notBeforeDate, notAfterDate, subjectName, subjectPublicKey);
    String signatureAlgorithm = "SHA256withRSA";
    ContentSigner signer =
        new JcaContentSignerBuilder(signatureAlgorithm)
            .setProvider("BC")
            .build(this.getPrivateKey());
    X509CertificateHolder clientCertHolder = certBuilder.build(signer);

    // Client cert chain
    X509Certificate clientCert =
        new JcaX509CertificateConverter().setProvider("BC").getCertificate(clientCertHolder);
    X509Certificate[] clientCertChain = this.getPushedCertChain(clientCert);

    return new CertificateGroup(clientCertChain, clientCertHolder, subjectPrivateKey);
  }

  public byte[] getCertEncoded() throws IOException {
    String certString = this.getCertString();
    return certString.getBytes(StandardCharsets.UTF_8);
  }

  public String getCertString() throws IOException {
    StringWriter stringWriter = new StringWriter();
    try (PemWriter certPemWriter = new PemWriter(stringWriter)) {
      Certificate cert = this.certHolder.toASN1Structure();
      byte[] certBytes = cert.getEncoded();
      certPemWriter.writeObject(new PemObject("CERTIFICATE", certBytes));
    }
    return stringWriter.toString();
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

  private X509Certificate[] getPushedCertChain(X509Certificate cert) {
    int newCertChainLength = this.certChain.length + 1;
    int sourcePosition = 0;
    int destinationPosition = 1;
    X509Certificate[] newCertChain = new X509Certificate[newCertChainLength];
    newCertChain[0] = cert;
    System.arraycopy(
        this.certChain, sourcePosition, newCertChain, destinationPosition, this.certChain.length);
    return newCertChain;
  }
}
