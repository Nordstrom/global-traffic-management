(defproject keymaster "0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["gtm-grpc" {:url "https://yourdomain.com/grpc-maven/"}]
                 ["jcenter" {:url "https://jcenter.bintray.com/"}]]
  :local-repo ~(str (System/getProperty "user.home") "/localMaven/public") ;;for gitlab-ci
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [mount "0.1.10"]
                 [pocheshiro "0.1.1"]]
  :extensions [[kr.motd.maven/os-maven-plugin "1.5.0.Final"]]
  :pom-addition [:properties ([:project.build.sourceEncoding "UTF-8"])]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :main ^:skip-aot keymaster.core
  :target-path "target/%s"
  :profiles
  {:provided {:dependencies [[io.netty/netty-codec-http2 "4.1.16.Final"]
                             [com.amazonaws/aws-java-sdk "1.11.166"]
                             [com.taoensso/faraday "1.9.0"]
                             [javax.servlet/servlet-api "2.5"]
                             [com.google.protobuf/protobuf-java "3.6.0"]
                             [io.netty/netty-tcnative-boringssl-static "2.0.6.Final"]
                             [com.nordstrom.gtm/keymaster "0.0.1"]
                             [io.grpc/grpc-core "1.7.0"]
                             [io.grpc/grpc-netty
                              "1.7.0"
                              :exclusions
                              [[io.grpc/grpc-core] [io.netty/netty-codec-http2]]]
                             [io.grpc/grpc-protobuf "1.7.0"]
                             [io.grpc/grpc-stub "1.7.0"]
                             [org.bouncycastle/bcpkix-jdk15on "1.60"]]}
   :uberjar  {:aot :all}})
