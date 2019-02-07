(ns gatekeeper.client
  (:import [io.grpc ManagedChannelBuilder])
  (:import [com.nordstrom.gatekeeper
            AuthorizationRequest
            AuthorizationResponse
            SinglePermission
            MultiplePermissions
            GatekeeperGrpc])
  (:gen-class))

(defn build-single-permission [permission]
  (-> (SinglePermission/newBuilder)
      (.setPermission permission)
      (.build)))

(defn build-multiple-permissions [permissions]
  (-> (MultiplePermissions/newBuilder)
      (.addAllPermission permissions)
      (.build)))

(defn build-permission-request [builder permissions]
  (if (instance? String permissions)
    ;; string
    (.setSingle builder (build-single-permission permissions))
    ;; collection
    (if (= (count permissions) 1)
      ;; size 1
      (.setSingle builder (build-single-permission (first permissions)))
      ;; size n
      (.setMultiple builder (build-multiple-permissions permissions))
      )))

(defn build-request [subject-id permissions]
  (-> (AuthorizationRequest/newBuilder)
      (.setSubjectId subject-id)
      (build-permission-request permissions)
      (.build)))

(defn build-client [host port]
  (let [channel (-> (ManagedChannelBuilder/forAddress host port)
                    (.usePlaintext true)
                    (.build))
        client (GatekeeperGrpc/newBlockingStub channel)]
    client))

(defn make-request [client request]
  (let [response (.authorize client request)]
    response))

(defn handle-response [response]
  (println "Response " response)
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [host (first args)
        port (read-string (second args))
        client (build-client host port)
        subject-id (nth args 2)
        permissions (nthrest args 3)
        ]
    (println (str "Client! " host " " port " subject " subject-id " permissions " permissions))
    (handle-response (make-request client (build-request subject-id permissions)))))
