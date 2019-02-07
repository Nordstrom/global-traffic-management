(ns keymaster.authn
  (:import [org.apache.shiro.subject Subject])
  (:import org.apache.shiro.SecurityUtils)
  (:import [org.apache.shiro.authc
            AuthenticationToken IncorrectCredentialsException UsernamePasswordToken])
  (:import org.apache.shiro.mgt.DefaultSecurityManager)
  (:require [keymaster.storage :as storage])
  (:require [pocheshiro.core :as shiro]))


;; create a bcrypt PasswordService that uses the default options for bcrypt
(def bcrypted-passwords (shiro/bcrypt-passwords {}))

;; define a username-password-realm that validates passwords using the bcrypt PasswordService.
;; this glues together the fetching of the subject from dynamo with the validation of passwords.
(def dynamodb-realm
  (shiro/username-password-realm
   :passwords bcrypted-passwords
   :get-authentication
   ;; fetch the subject from dynamo by calling get-subject, passing in the result of getPrincipal
   #(if-let [subject (storage/get-subject (.getPrincipal %))] ;; .getPrincipal is being called on a Subject (https://shiro.apache.org/static/1.3.2/apidocs/org/apache/shiro/subject/Subject.html)
      ;; the username-password-realm expects the principal and credentials to be in a map like so:
      {:principal (:username subject)
       :credentials (:password-hash subject)})))

;; setup our security manager with the dynamodb-realm
(def security-manager (DefaultSecurityManager. [dynamodb-realm]))

;; setup our subject-builder with the security manager
(def subject-builder (org.apache.shiro.subject.Subject$Builder. security-manager))

;; mutating function to create a new username and password then store
;; them in dynamodb. This should be used for debugging and as a last
;; resort to create certificates. Any one with the right dynamodb
;; permissions can call this function from the repl and create
;; themselves a user.
(defn register-user! [{:keys [username password]}]
  (let [password-hash (.encryptPassword bcrypted-passwords password)]
    (storage/setup-subject username password-hash)))

;; this creates a shiro Subject then tries to login with the provided
;; AuthenticationToken. it returns the result of isAuthenticated.
(defn auth-with-token [^AuthenticationToken token]
  (let [subject (.buildSubject subject-builder)]
    ;;(-> subject .getSession .stop)
    (.login subject token)
    (.isAuthenticated subject)))

;; convenience function to wrap auth-with-token catches
;; IncorrectCredentialsException and returns false, otherwise returns
;; the result of auth-with-token.
(defn auth [username password]
  (try
    (auth-with-token (UsernamePasswordToken. username password))
    (catch IncorrectCredentialsException e false)))
