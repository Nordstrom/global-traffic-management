# NLP Nordstrom Local Proxy

### Building and Running

* list gradle tasks: $
`./gradlew tasks`

* build jar: $
`./gradlew shadowJar`

* run tests: $
`./gradlew test`

# Running the NLP
./gradlew :nlp-proxy:distTar
./gradlew :nlp-proxy:installDist
cd ./nlp-proxy/build/distributions/nlp-x.x.x-SNAPSHOT/bin
./nlp application.conf route.json
** the route.json is in this format

[
  {
      "name": "route1",
      "path": "/path1/",
      "client_name": "client1",
      "port_number": 1234,
      "tls_enabled": false,
      "ip_addresses": ["1.2.3.4", "1.2.3.5"]
  },
  {
      "name": "route2",
      "path": "/path2/",
      "client_name": "client2",
      "port_number": 5678,
      "tls_enabled": true,
      "ip_addresses": ["2.2.3.4", "2.2.3.5"]
  },
  {
      "name": "route3",
      "path": "/path3/",
      "client_name": "client3",
      "port_number": 9012,
      "tls_enabled": false,
      "ip_addresses": []
  }
]


