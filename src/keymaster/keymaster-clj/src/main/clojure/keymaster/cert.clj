(ns keymaster.cert
  (:import [java.security.interfaces RSAPrivateKey RSAPublicKey])
  (:import [java.security KeyPair KeyPairGenerator])
  (:import [java.security.cert Certificate])
  (:import [java.util Date])
  (:import [java.io StringWriter])
  (:import [org.bouncycastle.jce.provider BouncyCastleProvider])
  (:import [org.bouncycastle.crypto.params RSAKeyParameters])
  (:import [org.bouncycastle.crypto AsymmetricCipherKeyPair])
  (:import [org.bouncycastle.asn1.x500 X500Name])
  (:import [org.bouncycastle.operator DefaultDigestAlgorithmIdentifierFinder DefaultSignatureAlgorithmIdentifierFinder])
  (:import [org.bouncycastle.operator.bc BcRSAContentSignerBuilder])
  (:import [org.bouncycastle.crypto.util SubjectPublicKeyInfoFactory])
  (:import [org.bouncycastle.openssl PEMWriter])
  (:import [org.bouncycastle.cert X509v1CertificateBuilder]))

;; define CertificatePair, this is how we represent our ca and any
;; cert pairs we generate. It's comprised of the following:

;; public - the public key
;; private - the private key
;; cert - the certificate
;; cert-chain - the chain of certificates that lead back to an authority
(defrecord CertificatePair [private cert cert-chain])

;; convenience function to build a CertificatePair
(defn certificate-pair [keypair cert cert-chain]
  (map->CertificatePair {:private (cast RSAPrivateKey (.getPrivate keypair))
                         :cert cert
                         :cert-chain cert-chain}))

;; define CertificateRequest, this is how we represent a keypair that
;; needs to be signed. it's comprised of the following:

;; public - the public key
;; private - the private key
;; subject-id - the subject-id of the requester
;; serial - the serial # for the cert (auto incrementing from dynamodb)
;(defrecord CertificateRequest [public private subject-id serial])

;; convenience function to build a CertificateRequest
;(defn certificate-request [keypair subject serial]
;  (map->CertificateRequest {:public (cast RSAPublicKey (.getPublic keypair))
;                            :private (cast RSAPrivateKey (.getPrivate keypair))
;                            :subject-id (:subject-id subject)
;                            :serial serial
;                            }))


;; define CertificateResponse, this is how we represent a signed
;; certificate pair that we're about to return to a client. it's
;; comprised of the following:

;; cert-pair - a CertificatePair record
;; format - the requested format (PEM/pkcs12) to encode the response in
;; expiration - a string timestamp of when the certificate expires (in case the client can't grok x509)
;(defrecord CertificateResponse [cert-pair format expiration])

;; convenience function to build a CertificateResponse
;(defn certificate-response [cert-pair format expiration]
;  (map->CertificateResponse {:cert-pair cert-pair
;                             :format format
;                             :expiration expiration}))

;; What follows is a bunch of boilerplate to work with bouncey
;; castle. Some of this needs to be cleaned up and either removed or
;; placed into an examples module.

;;(defrecord KeyPair [public private])
