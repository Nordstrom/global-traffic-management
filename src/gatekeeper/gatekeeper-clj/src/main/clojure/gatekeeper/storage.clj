(ns gatekeeper.storage
  (:import [com.amazonaws.services.dynamodbv2.model ConditionalCheckFailedException])
  (:import [com.amazonaws.auth DefaultAWSCredentialsProviderChain])
  (:require [taoensso.faraday :as faraday]))

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


(defn create-role-permissions-table []
  (faraday/create-table client-opts :role-permissions
                        [:role-id :s]
                        {:throughput {:read 1 :write 1}
                         :block? true}))

(defn create-subject-permissions-table []
  (faraday/create-table client-opts :subject-permissions
                        [:subject-id :s]
                        {:throughput {:read 1 :write 1}
                         :block? true}))

(defn guarantee-table [table-name]
  (if (nil? (faraday/describe-table client-opts table-name))
    (condp = table-name
      :subject-permissions (create-subject-permissions-table)
      :role-permissions (create-role-permissions-table))))

(defn set-or-nil [maybe-set]
  (if (empty? maybe-set) nil (set maybe-set)))

(defn assoc-val [map key val]
  (if (nil? val)
    map
    (assoc map key val)))

(defn create-subject-permissions [subject-id & {:keys [roles permissions]
                                               :or {roles nil permissions nil}}]
  (do (guarantee-table :subject-permissions))
  (try
    (faraday/put-item client-opts :subject-permissions
                      (-> {:subject-id subject-id}
                          (assoc-val :permissions (set-or-nil (map name permissions)))
                          (assoc-val :roles (set-or-nil (map name roles))))
                      ;; https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html
                      {:expr-attr-names {"#subject" "subject-id"}
                       :cond-expr "attribute_not_exists(#subject)"})
    [{:subject-id subject-id :permissions permissions :roles roles} nil]
    (catch ConditionalCheckFailedException e [nil (str "subject-id '" subject-id "' already exists")])))

(defn add-subject-permissions [subject-id permissions]
  (try
    [(faraday/update-item client-opts :subject-permissions
                          {:subject-id subject-id}
                          {:expr-attr-names {"#subject" "subject-id"
                                             "#permissionset" "permissions"}
                           :expr-attr-vals {":item" (set (map name permissions))}
                           :cond-expr "attribute_exists(#subject)"
                           :update-expr "ADD #permissionset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn remove-subject-permissions [subject-id permissions]
  (try
    [(faraday/update-item client-opts :subject-permissions
                          {:subject-id subject-id}
                          {:expr-attr-names {"#subject" "subject-id"
                                             "#permissionset" "permissions"}
                           :expr-attr-vals {":item" (set (map name permissions))}
                           :cond-expr "attribute_exists(#subject)"
                           :update-expr "DELETE #permissionset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn add-subject-roles [subject-id roles]
  (try
    [(faraday/update-item client-opts :subject-permissions
                          {:subject-id subject-id}
                          {:expr-attr-names {"#subject" "subject-id"
                                             "#roleset" "roles"}
                           :expr-attr-vals {":item" (set (map name roles))}
                           :cond-expr "attribute_exists(#subject)"
                           :update-expr "ADD #roleset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn remove-subject-roles [subject-id roles]
  (try
    [(faraday/update-item client-opts :subject-permissions
                          {:subject-id subject-id}
                          {:expr-attr-names {"#subject" "subject-id"
                                             "#roleset" "roles"}
                           :expr-attr-vals {":item" (set (map name roles))}
                           :cond-expr "attribute_exists(#subject)"
                           :update-expr "DELETE #roleset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn create-role-permissions [role-id & {:keys [permissions]
                                          :or {permissions nil}}]
  (do (guarantee-table :role-permissions))
  (try
    (faraday/put-item client-opts :role-permissions
                      (-> {:role-id (name role-id)}
                          (assoc-val :permissions (set-or-nil (map name permissions))))
                      ;; https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html
                      {:expr-attr-names {"#role" "role-id"}
                       :cond-expr "attribute_not_exists(#role)"})
    [{:role-id role-id :permissions permissions} nil]
    (catch ConditionalCheckFailedException e [nil (str "role-id '" role-id "' already exists")])))

(defn add-role-permissions [role-id permissions]
  (try
    [(faraday/update-item client-opts :role-permissions
                          {:role-id (name role-id)}
                          {:expr-attr-names {"#role" "role-id"
                                             "#permissionset" "permissions"}
                           :expr-attr-vals {":item" (set (map name permissions))}
                           :cond-expr "attribute_exists(#role)"
                           :update-expr "ADD #permissionset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn remove-role-permissions [role-id permissions]
  (try
    [(faraday/update-item client-opts :role-permissions
                          {:role-id (name role-id)}
                          {:expr-attr-names {"#role" "role-id"
                                             "#permissionset" "permissions"}
                           :expr-attr-vals {":item" (set (map name permissions))}
                           :cond-expr "attribute_exists(#role)"
                           :update-expr "DELETE #permissionset :item"
                           :return :all-new}
                          ) nil]
    (catch ConditionalCheckFailedException e [nil e])))

(defn get-subject [subject-id]
  (let [item (faraday/get-item client-opts :subject-permissions {:subject-id subject-id})]
    {:subject-id (:subject-id item)
     :permissions (set (map keyword (:permissions item)))
     :roles (set (map keyword (:roles item)))}))

(defn get-role [role-id]
  (let [item (faraday/get-item client-opts :role-permissions {:role-id (name role-id)})]
    {:role-id (keyword (:role-id item))
     :permissions (set (map keyword (:permissions item)))}))
