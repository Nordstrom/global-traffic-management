(ns keymaster.client
  (:import [io.grpc ManagedChannelBuilder])
  (:import [io.grpc.netty NettyChannelBuilder])
  (:import [io.netty.handler.ssl
            ApplicationProtocolConfig
            ApplicationProtocolConfig$Protocol
            ApplicationProtocolConfig$SelectorFailureBehavior
            ApplicationProtocolConfig$SelectedListenerFailureBehavior
            SslContextBuilder])
  (:import [io.netty.handler.ssl.util InsecureTrustManagerFactory])

  (:import [com.nordstrom.keymaster
            AuthenticationRequest
            AuthenticationResponse
            BasicCredentials
            KeymasterGrpc]
           [io.grpc Server ServerBuilder])
  (:gen-class))


;; this is a bunch of boilerplate for making a gRPC request to keymaster

(defn build-request [username password]
  (-> (AuthenticationRequest/newBuilder)
      (.setBasic (-> (BasicCredentials/newBuilder)
                     (.setUsername username)
                     (.setPassword password)
                     (.build)))
      (.build)))

(defn build-client [host port]
  (let [channel (-> (ManagedChannelBuilder/forAddress host port)
                    (.usePlaintext true)
                    (.build))
        client (KeymasterGrpc/newBlockingStub channel)]
    client))

(defn alpn-config []
  (ApplicationProtocolConfig.
   ApplicationProtocolConfig$Protocol/ALPN
   ApplicationProtocolConfig$SelectorFailureBehavior/NO_ADVERTISE
   ApplicationProtocolConfig$SelectedListenerFailureBehavior/ACCEPT
   ["h2"]))

(defn build-ssl-context []
  (-> (SslContextBuilder/forClient)
      (.applicationProtocolConfig (alpn-config))
      (.trustManager InsecureTrustManagerFactory/INSTANCE)
      (.build)))

(defn build-secure-client [host port]
  (let [channel (-> (NettyChannelBuilder/forAddress host port)
                    (.sslContext (build-ssl-context))
                    (.build))
        client (KeymasterGrpc/newBlockingStub channel)]
    client))

(defn make-request [client request]
  (let [response (.authenticate client request)]
    response))

(defn handle-response [response]
  (println "Response " response)
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [host (first args)
        port (read-string (second args))
        client (build-secure-client host port)
        ]
    (println (str "Client! " host port) )
    (handle-response (make-request client (build-request "user" "pass")))))
