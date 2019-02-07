(ns gatekeeper.authz
  (:require [gatekeeper.storage :as storage])
  (:require [gatekeeper.constants :as constants])
  (:require [pocheshiro.core :as shiro])
  (:import [org.apache.shiro.subject Subject Subject$Builder])
  (:import org.apache.shiro.SecurityUtils)
  (:import [org.apache.shiro.authc
            AuthenticationToken IncorrectCredentialsException UnknownAccountException])
  (:import org.apache.shiro.mgt.DefaultSecurityManager)
  (:import org.apache.shiro.authz.permission.RolePermissionResolver)
  (:import org.apache.shiro.authc.credential.SimpleCredentialsMatcher)
  (:import [com.nordstrom.gatekeeper AuthorizationRequest$PermissionCase]))

(defn role-resolver-proxy [realm resolver]
  (let [permission-resolver (.getPermissionResolver realm)]
    (proxy [RolePermissionResolver] []
      (resolvePermissionsInRole [roleString]
        (map (fn [permission] (.resolvePermission permission-resolver permission))
             (resolver roleString))))))

(defn subject-token-realm
  [& {:keys [get-authentication get-authorization get-role-permissions realm-name]
      :or {realm-name "subject-token-realm"}}]
  (let [realm (shiro/defrealm-fn realm-name (SimpleCredentialsMatcher.)
           :supports? (constantly true)
           :get-authentication get-authentication
                :get-authorization get-authorization)]
    (doto realm
      (.setRolePermissionResolver (role-resolver-proxy realm get-role-permissions)))))

(def dynamodb-realm (subject-token-realm
                 :get-authentication #(if-let [subject (storage/get-subject (.getPrincipal %))]
                                        {:principal (:subject-id subject)
                                         :credentials (:subject-id subject)})
                 :get-authorization #(if-let [subject (storage/get-subject (.getPrimaryPrincipal %))]
                                       (select-keys subject [:roles :permissions]))
                 :get-role-permissions #(if-let [role (storage/get-role (keyword %))]
                                          (map name (:permissions role)))))

(def security-manager (DefaultSecurityManager. [dynamodb-realm]))

(def subject-builder (Subject$Builder. security-manager))

;; (defn register-user! [{:keys [username password]}]
;;   (let [password-hash (.encryptPassword bcrypted-passwords password)]
;;     (storage/setup-subject username password-hash)))

(defn subject-token [subject-id]
  (proxy [AuthenticationToken] []
    (getCredentials [] subject-id)
    (getPrincipal [] subject-id)))

(defn auth-with-subject-token [subject-id]
  (let [subject (.buildSubject subject-builder)]
    (.login subject (subject-token subject-id))
    [(.isAuthenticated subject) subject]))

(defn auth [subject-id]
  (try
    (auth-with-subject-token subject-id)
    (catch IncorrectCredentialsException e [false nil])
    (catch UnknownAccountException e [false nil])))

(defn authorize-single-permission [subject request]
  (let [permission (-> request (.getSingle) (.getPermission))]
    {permission (.isPermitted subject permission)}))

(defn authorize-multiple-permissions [subject request]
  (let [permissions (-> request (.getMultiple) (.getPermissionList))]
    (zipmap permissions (.isPermitted subject (into-array String permissions)))))

(defn authorize [request current-time]
  (let [subject-id (.getSubjectId request)
        result (auth subject-id)
        subject (second result)]
    (if (first result)
      [true {:permissions (condp = (.getPermissionCase request)
                AuthorizationRequest$PermissionCase/SINGLE (authorize-single-permission subject request)
                AuthorizationRequest$PermissionCase/MULTIPLE (authorize-multiple-permissions subject request))
             :cache-ttl (.plus current-time constants/authz-duration)}]
      [false nil])))


;; TODO maybe we should consider embedding permissions into the cert that keymaster issues?
