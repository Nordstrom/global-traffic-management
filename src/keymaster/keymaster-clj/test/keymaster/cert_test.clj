(ns keymaster.cert-test
  (:require [clojure.test :refer :all]
            [keymaster.cert :refer :all]))

(deftest generate-rsa-key-map-test
  (let [key-map (generate-rsa-key-map 1024)]
    (testing "has a public key"
      (is (not (nil? (:public key-map)))))
    (testing "has a private key"
      (is (not (nil? (:private key-map)))))))

(deftest key-map->cipher-key-pair-test
  (testing "converts"
    (let [key-map (generate-rsa-key-map 1024)]
      (is (not (nil? (key-map->cipher-key-pair key-map)))))))

(deftest buil-root-cert-test
  (testing "builds"
    (is (not (nil? (build-root-cert (key-map->cipher-key-pair (generate-rsa-key-map 1024))))))))
