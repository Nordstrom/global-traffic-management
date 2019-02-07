#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR/..
./gradlew :int-test-backend:installDist

docker build -t bootique -f $DIR/Dockerfile_bootique $DIR/..

echo "docker run -p 8444:8444 bootique"
