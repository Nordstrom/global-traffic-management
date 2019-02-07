#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DEST_CLIENT=$DIR/gatekeeper-client/src/main/resources
DEST_SERVER=$DIR/gatekeeper-server/src/main/resources

# https://github.com/grpc/grpc-java/blob/master/examples/README.md#generating-self-signed-certificates-for-use-with-grpc

# Changes these CN's to match your hosts in your environment if needed.
SERVER_CN="*"
CLIENT_CN="*" # Used when doing mutual TLS
PASSWORD="1^thvugigh"


echo Generate CA key:
openssl genrsa -passout pass:${PASSWORD} -des3 -out ${DEST_SERVER}/ca-x509.key 4096


echo Generate CA certificate:
# Generates ca-trust-x509.pem which is the trustCertCollectionFile
openssl req -passin pass:${PASSWORD} -new -x509 -days 365 -key ${DEST_SERVER}/ca-x509.key -out ${DEST_SERVER}/ca-trust-x509.pem -subj "/CN=${SERVER_CN}"
cp ${DEST_SERVER}/ca-trust-x509.pem ${DEST_CLIENT}/ca-trust-x509.pem
cp ${DEST_SERVER}/ca-x509.key ${DEST_CLIENT}/ca-x509.key


echo Generate server key:
openssl genrsa -passout pass:${PASSWORD} -des3 -out ${DEST_SERVER}/server-private.key 4096
echo Generate server signing request:
openssl req -passin pass:${PASSWORD} -new -key ${DEST_SERVER}/server-private.key -out ${DEST_SERVER}/server.csr -subj "/CN=${SERVER_CN}"
echo Self-signed server certificate:
# Generates server-chain-x509.pem which is the certChainFile for the server
openssl x509 -req -passin pass:${PASSWORD} -days 365 -in ${DEST_SERVER}/server.csr -CA ${DEST_SERVER}/ca-trust-x509.pem -CAkey ${DEST_SERVER}/ca-x509.key -set_serial 01 -out ${DEST_SERVER}/server-chain-x509.pem
echo Remove passphrase from server key:
openssl rsa -passin pass:${PASSWORD} -in ${DEST_SERVER}/server-private.key -out ${DEST_SERVER}/server-private.key
# Generates server.pem which is the privateKeyFile for the Server
openssl pkcs8 -topk8 -nocrypt -in ${DEST_SERVER}/server-private.key -passin pass:${PASSWORD} -out ${DEST_SERVER}/server-private-key-pkcs8.pem

echo Generate client key
openssl genrsa -passout pass:${PASSWORD} -des3 -out ${DEST_CLIENT}/client-private.key 4096
echo Generate client signing request:
openssl req -passin pass:${PASSWORD} -new -key ${DEST_CLIENT}/client-private.key -out ${DEST_CLIENT}/client.csr -subj "/CN=${CLIENT_CN}"
echo Self-signed client certificate:
# Generates client-chain-x509.pem which is the clientCertChainFile for the client (need for mutual TLS only)
openssl x509 -passin pass:${PASSWORD} -req -days 365 -in ${DEST_CLIENT}/client.csr -CA ${DEST_CLIENT}/ca-trust-x509.pem -CAkey ${DEST_CLIENT}/ca-x509.key -set_serial 01 -out ${DEST_CLIENT}/client-chain-x509.pem
echo Remove passphrase from client key:
openssl rsa -passin pass:${PASSWORD} -in ${DEST_CLIENT}/client-private.key -out ${DEST_CLIENT}/client-private.key
echo Converting the private keys to X.509:
# Generates client.pem which is the clientPrivateKeyFile for the Client (needed for mutual TLS only)
openssl pkcs8 -topk8 -nocrypt -in ${DEST_CLIENT}/client-private.key -out ${DEST_CLIENT}/client-private-key-pkcs8.pem
