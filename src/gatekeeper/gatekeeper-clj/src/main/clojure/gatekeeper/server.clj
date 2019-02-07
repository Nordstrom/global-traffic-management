(ns gatekeeper.server
  (:require [mount.core :as mount :refer [defstate]])
  (:require [gatekeeper.storage :refer :all])
  (:require [gatekeeper.authz :as authz])
  (:import [com.nordstrom.gatekeeper
            AuthorizationError
            AuthorizationRequest
            AuthorizationRequest$PermissionCase
            AuthorizationResponse
            AuthorizationSuccess
            GatekeeperGrpc$GatekeeperImplBase]
           [io.grpc Server ServerBuilder])
  (:gen-class))

(defn Instant->Timestamp [instant]
  (-> (com.google.protobuf.Timestamp/newBuilder)
      (.setSeconds (.getEpochSecond instant))
      (.setNanos (.getNano instant))
      (.build)))

(defn handle-request [request]
  (println "handle-request " request)
  (let [response-builder (AuthorizationResponse/newBuilder)
        current-time (java.time.Instant/now)
        result (authz/authorize request current-time)]
    (.build
     (if (first result)
       ;; success
       (doto response-builder
         (.setSuccess (-> (AuthorizationSuccess/newBuilder)
                          (.putAllPermissions (:permissions (second result)))
                          (.setCacheTtl (Instant->Timestamp (:cache-ttl (second result))))
                          (.build))))

       ;; error
       (doto response-builder
         (.setError (-> (AuthorizationError/newBuilder)
                        (.setMessage "Could not authorize request")
                        (.build))))))))

(defn make-service []
  (proxy [GatekeeperGrpc$GatekeeperImplBase] []
    (authorize [^AuthorizationRequest request responseObserver]
      (doto responseObserver
        (.onNext (handle-request request))
        (.onCompleted)))))

(def server-atom (atom nil))

(def server-port-atom (atom nil))

(defstate grpc-server
  :start
  (let [builder (ServerBuilder/forPort @server-port-atom)
        service (make-service)
        _       (.addService builder service)
        server  (.build builder)]
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
    ;;(core/init-certificate-authority)
    (reset! server-port-atom port)
    (mount/start)
    (.addShutdownHook (Runtime/getRuntime) (Thread. #((println "SHUTDOWN") (mount/stop))))
    (.awaitTermination @server-atom)
    ))
