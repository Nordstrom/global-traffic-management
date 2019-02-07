#!/bin/bash -eux

for project in src/*/; do
  pushd $project
  ./gradlew license
  popd
done
