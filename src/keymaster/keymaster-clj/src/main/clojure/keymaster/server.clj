(ns keymaster.server
  (:require [mount.core :as mount :refer [defstate]])
  (:require [keymaster.authn :refer :all])
  (:require [keymaster.cert :refer :all])
  (:require [keymaster.storage :refer :all])
  (:require [keymaster.core :as core])
  (:import [com.google.protobuf ByteString])
  (:import [com.nordstrom.keymaster
            AuthenticationError
            AuthenticationRequest
            AuthenticationRequest$CredentialsCase
            AuthenticationResponse
            AuthenticationSuccess
            KeymasterGrpc$KeymasterImplBase]
           [io.grpc Server ServerBuilder])
  (:gen-class))

;; boilerplate for setting up a gRPC service for keymaster
(defn handle-request [request]
  (println "handle-request " request)
  (let [response-builder (AuthenticationResponse/newBuilder)]
    (.build
     (if (core/authenticate-request request)
       ;; success
       (let [username (-> request (.getBasic) (.getUsername))
             current-time (java.time.Instant/now)
             subject (get-subject username)
             subject-id (:subject-id subject)
             cert-group (com.nordstrom.keymaster.CertificateGroup. "snakeoil.cert.pem" "snakeoil.key.pem")
             client-cert-group (.createClientCertificateGroup cert-group subject-id)]
         (doto response-builder
           (.setSuccess (-> (AuthenticationSuccess/newBuilder)
                            (.setPrivateKey (ByteString/copyFrom (.getPrivateKeyEncoded client-cert-group)))
                            (.setCert (ByteString/copyFrom (.getCertEncoded client-cert-group)))
                            (.setExpiration (.getExpiration client-cert-group))
                            (.build)))))
       ;; error
       (doto response-builder
         (.setError (-> (AuthenticationError/newBuilder)
                        (.setMessage "Could not authenticate request")
                        (.build))))))))

(defn make-service []
  (proxy [KeymasterGrpc$KeymasterImplBase] []
    (authenticate [^AuthenticationRequest request responseObserver]
      (doto responseObserver
        (.onNext (handle-request request))
        (.onCompleted)))))

(def server-atom (atom nil))

(def server-port-atom (atom nil))

(def cert-chain-file (clojure.java.io/file "keymaster-certificate-chain.pem"))

(def private-key-file (clojure.java.io/file "keymaster-private-key.pem"))

(defstate grpc-server
  :start
  (let [server (-> (ServerBuilder/forPort @server-port-atom)
                   (.addService (make-service))
                   (.useTransportSecurity cert-chain-file private-key-file)
                   (.build))]
    (.start server)
    (reset! server-atom server))
  :stop
  (.shutdown @server-atom))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [host (first args)
        port (read-string (second args))
        ]
    (println (str "Server! " host port))
    (core/init-certificate-authority)
    (reset! server-port-atom port)
    (mount/start)
    (.addShutdownHook (Runtime/getRuntime) (Thread. #((println "SHUTDOWN") (mount/stop))))
    (.awaitTermination @server-atom)
    ))
