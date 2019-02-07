#!/bin/bash -eux

for project in src/*/; do
  pushd $project
  ./gradlew licenseFormat
  popd
done
