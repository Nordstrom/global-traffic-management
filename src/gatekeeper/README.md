# Gatekeeper Overview

Service that validates certificates, returns scopes for tokens, and maintains a CRL (Certificate Revocation List)

* Centralized Auth N
* Centralized Auth Z
* Mutual Auth (Zero Trust High Entropy)
* Support Multi Auth Domains (GCP, AWS)
* Support Least Privilege Authorization
* Cert Expiration Alerts
* Cert Store
* Token Generation and Support
* Support Modern Security
* Secure APIs (WAF)
* Authorization in Parallel
* DDoS Protection
* Bot Protection
* Machine Learning Bot Protection
* Low Latency
* Language Agnostic

-----------------------------------------------------------

## Gatekeeper-Server

#### Run Locally
Run the Gatekeeper Server and DynamoLocal with Docker Compose
```
cd docker
./gatekeeper/docker_build_prep.sh
docker-compose up 
```

OR 

[Run Dynamo locally with Docker](./docker/dynamo-local/README.md)

And then run the Gatekeeper Server via Gradle
```
./gradlew :gatekeeper-server:runServer 
```

-----------------------------------------------------------

## Gatekeeper-Client

Publishes a gRPC client library to Nordstrom Artifactory for use in other GTM components.

-----------------------------------------------------------

## Gatekeeper-Client-CLI

Application for performing Gatekeeper operations via CLI.

Run the Gatekeeper Client CLI
```
./gradlew :gatekeeper-client-cli:installDist

./gatekeeper-client-cli/build/install/gatekeeper-client-cli/bin/gatekeeper-client-cli localhost 7777
```

-----------------------------------------------------------

## License

Copyright © 2018 Nordstrom

Distributed under the [Eclipse Public License](LICENSE) either version 1.0 or (at
your option) any later version.
