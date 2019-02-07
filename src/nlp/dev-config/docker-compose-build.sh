#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd $DIR/..
./gradlew :nlp-proxy:installDist
popd

pushd $DIR
pwd
docker-compose build
popd

