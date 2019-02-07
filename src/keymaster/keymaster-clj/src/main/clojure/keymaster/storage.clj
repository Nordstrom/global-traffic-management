(ns keymaster.storage
  (:import [com.amazonaws.services.dynamodbv2.model ConditionalCheckFailedException])
  (:import [com.amazonaws.auth DefaultAWSCredentialsProviderChain])
  (:require [taoensso.faraday :as faraday]))

;; lots of dynamodb boilerplate in here, most of this should be moved
;; to cloudformation. Some of it needs to be updated (create-subjects-to-keypair-table)

(defn env-with-default [key default]
  (let [value (System/getenv key)]
    (if value value default)))

(def client-opts
  (if (System/getenv "LOCAL_DEV")
  {;;; For DDB Local just use some random strings here, otherwise include your
   ;;; production IAM keys:
   :access-key "..."
   :secret-key "..."
   :endpoint "http://127.0.0.1:8000"
   }
  {;;; in production AWS can infer the endpoint from environment variables
   :provider (DefaultAWSCredentialsProviderChain/getInstance)
   :endpoint (apply str (concat "https://dynamodb."
                     (env-with-default "AWS_DEFAULT_REGION" "us-west-2")
                     ".amazonaws.com"))
   }))

(defn create-subjects-table []
  (faraday/create-table client-opts :subjects
                        [:username :s]
                        {:gsindexes [{:name "subject-id-index"
                                      :hash-keydef [:subject-id :s]
                                      :throughput {:read 1 :write 1}}]
                         :throughput {:read 1 :write 1}
                         :block? true}))

;; this might be overkill we could just store
;; {:subject-id ... :keypair-id ... }
;; and index the subject-id
(defn create-subjects-to-keypairs-table []
  (faraday/create-table client-opts :subjects-to-keypairs
                        [:subject-id :s]
                        {:throughput {:read 1 :write 1}
                         :block? true}))

(defn create-keypair-counters-table []
  (faraday/create-table client-opts :keypair-counters
                        [:subject-id :s]
                        {:throughput {:read 1 :write 1}
                         :block? true}))

(defn create-keypairs-table []
  (faraday/create-table client-opts :keypairs
                        [:keypair-id :s]
                        {:throughput {:read 1 :write 1}
                         :block? true}))

(comment
  {:subject-id :s
   :keypair-ids :ss
   :keypair-counter :n
   })

(defn guarantee-table [table-name]
  (if (nil? (faraday/describe-table client-opts table-name))
    (condp = table-name
      :keypairs (create-keypairs-table)
      :keypair-counters (create-keypair-counters-table)
      :subjects (create-subjects-table)
      :subjects-to-keypairs (create-subjects-to-keypairs-table))))

(defn create-keypair-counter [subject-id]
  (do (guarantee-table :keypair-counters))
  (try
    (faraday/put-item client-opts :keypair-counters
                      {:subject-id subject-id
                       :keypair-counter 0}
                      {:expr-attr-names {"#subject" "subject-id"}
                       :cond-expr "attribute_not_exists(#subject)"})
    [true nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn create-subject-to-keypair [subject-id keypair-id]
  (do (guarantee-table :subjects-to-keypairs))
  (try
    (faraday/put-item client-opts :subjects-to-keypairs
                      {:subject-id subject-id
                       :keypair-ids #{(str keypair-id)}}
                      {:expr-attr-names {"#subject" "subject-id"}
                       :cond-expr "attribute_not_exists(#subject)"}
                      )
    [true nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn update-subject-to-keypair [subject-id keypair-id]
  (try
    [(faraday/update-item client-opts :subjects-to-keypairs
                           {:subject-id subject-id}
                           {:expr-attr-names {"#subject" "subject-id"
                                              "#keypairset" "keypair-ids"}
                            :expr-attr-vals {":item" #{(str keypair-id)}}
                            :cond-expr "attribute_exists(#subject)"
                            :update-expr "ADD #keypairset :item"
                            :return :all-new}
                           ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn create-subject [username password-hash]
  (do (guarantee-table :subjects))
  (let [subject-id (.toString (java.util.UUID/randomUUID))]
    (try
      (faraday/put-item client-opts :subjects
                        {:subject-id subject-id
                         :username username
                         :password-hash password-hash}
                        ;; https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html
                        {:cond-expr "attribute_not_exists(username)"})
      [{:username username :subject-id subject-id} nil]
      (catch ConditionalCheckFailedException e [nil (str "username '" username "' already exists")]))))


(defn setup-subject [username password-hash]
  (let [result (create-subject username password-hash)]
    (if (nil? (second result))
      (do (print "creating keypair counter")
          (create-keypair-counter (:subject-id (first result)))))))


(defn update-keypair-counter [subject-id]
  (do (guarantee-table :keypair-counters))
  (try
    [(faraday/update-item client-opts :keypair-counters
                          {:subject-id subject-id}
                          {:expr-attr-names {"#counter" "keypair-counter"}
                           :expr-attr-vals {":incr" 1}
                           :update-expr "SET #counter = #counter + :incr"
                           :return :all-new}) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn keypair-serial [uuid counter]
  (let [base-serial (.multiply (java.math.BigInteger.
                                (str (Long/toHexString (.getMostSignificantBits uuid))
                                     (Long/toHexString (.getLeastSignificantBits uuid))) 16)
                               (java.math.BigInteger. "100000000"))]
    [(.add base-serial (java.math.BigInteger. (str counter))) counter]))

(defn create-keypair-serial [subject-id]
  (let [uuid (java.util.UUID/fromString subject-id)
        result (update-keypair-counter subject-id)]
    (if (nil? (second result))
      (keypair-serial uuid (:keypair-counter (first result))))))

(defn create-keypair-entries [subject-id]
  (let [[serial counter] (create-keypair-serial subject-id)]
    (if (= counter 1)
      ;; create the entry with serial as the initial member of the keypair-ids set
      (create-subject-to-keypair subject-id serial)
      ;; update the entry with serial as the next member of the keypair-ids set
      (update-subject-to-keypair subject-id serial)
      )
    serial
    )
  )

(defn store-keypair [subject-id keypair-id pem]
  (do (guarantee-table :keypairs))
  (try
    (faraday/put-item client-opts :keypairs
                      {:subject-id subject-id
                       :keypair-id keypair-id
                       :pem pem}
                      {:expr-attr-names {"#keypair" "keypair-id"}
                       :cond-expr "attribute_not_exists(#keypair)"})
    [true nil]
    (catch ConditionalCheckFailedException e [nil e]))
  )

(defn get-subject [username]
  (do (guarantee-table :subjects))
  (faraday/get-item client-opts :subjects {:username username}))
