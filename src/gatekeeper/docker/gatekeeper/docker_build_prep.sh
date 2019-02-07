#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mkdir -p $DIR/distributions
pushd $DIR/../..
./gradlew :gatekeeper-server:buildDeb
find ./gatekeeper-server/build/distributions -type f -name '*.deb' -exec cp '{}' ./docker/gatekeeper/distributions/gatekeeper.deb ';'
popd
