#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ ! -f $DIR/lein/bin/lein ]; then
    pushd $DIR
    mkdir -p ./lein/bin &&
    curl -o ./lein/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein &&
    chmod a+x ./lein/bin/lein;
    popd
fi

pushd $DIR/gatekeeper-clj
$DIR/lein/bin/lein uberjar
popd

mkdir -p $DIR/gatekeeper/libs
rm $DIR/gatekeeper/libs/gatekeeper-clj.jar
mv $DIR/gatekeeper-clj/target/uberjar/gatekeeper-0-standalone.jar $DIR/gatekeeper/libs/gatekeeper-clj.jar