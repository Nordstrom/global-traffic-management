#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR/..
./gradlew :nfe-proxy:installDist
./gradlew :int-test-backend:installDist

cd $DIR
docker-compose build
