(ns keymaster.core
  (:require [mount.core :as mount :refer [defstate]])
  (:require [keymaster.authn :refer :all])
  (:require [keymaster.cert :refer :all])
  (:require [keymaster.storage :refer :all])
  (:import [java.security.interfaces RSAPrivateKey RSAPublicKey])
  (:import [java.security.cert X509Certificate])
  (:import [com.nordstrom.keymaster
            AuthenticationError
            AuthenticationRequest
            AuthenticationRequest$CredentialsCase
            AuthenticationResponse
            AuthenticationSuccess
            KeymasterGrpc$KeymasterImplBase]
           [io.grpc Server ServerBuilder]))


;; default key size in bits
(def key-size 1024)

;; default certificate duration
(def cert-duration (java.time.Duration/ofMinutes 15))

;; convenience function to authenticate a subject that has provided basic credentials
(defn authenticate-request-basic [request]
  (let [username (-> request (.getBasic) (.getUsername))
        password (-> request (.getBasic) (.getPassword))]
    ;; this auth function has been imported from keymaster.authn
    (auth username password)))

;; convenience function to authenticate a subject based on the AuthenticateRequest payload
(defn authenticate-request [request]
  (condp = (.getCredentialsCase request)
    AuthenticationRequest$CredentialsCase/BASIC (authenticate-request-basic request)))

;; this atom will hold the CertificatePair that we will use to sign
;; new certificates.  atoms are how clojure deals with mutation and
;; state.  we don't really need to worry about concurrency here since
;; after we load the certificate authority it's read only.  if in the
;; future we need to update a running keymaster instance with a new
;; certificate authority this atom will allow for it.
(def certificate-authority (atom nil))

;; this function will read a CertificatePair from pem files.
(defn load-ca [certfile keyfile]
  (let [cert-group (com.nordstrom.keymaster.CertificateGroup. certfile keyfile)]
    (map->CertificatePair {:private (.getPrivateKey cert-group)
                           :cert (.getCertHolder cert-group)
                           :cert-chain (.getCertChain cert-group)
                           })))

;; this will load the CertificatePair from "snakeoil.cert.pem" and "snakeoil.key.pem"
;; and store it in the certificate-authority atom.  The do macro can probably be
;; removed, I think I was doing something stupid at the time trying to
;; track down some lazy evaluation.
(defn init-certificate-authority []
  (do
    (reset! certificate-authority (load-ca "snakeoil.cert.pem" "snakeoil.key.pem"))
    nil))
