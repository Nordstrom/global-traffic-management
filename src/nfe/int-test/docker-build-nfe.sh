#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR/..
./gradlew :nfe-proxy:installDist

docker build -t nfe -f $DIR/Dockerfile_nfe $DIR/..

echo "docker run -p 8443:8443 nfe"
