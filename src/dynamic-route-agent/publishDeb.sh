#!/bin/bash -eux

REPO=gtm
APP=${1:-${CI_PROJECT_NAME}}
UPLOAD=${REPO}-${APP}-upload
STAMP=$(date -u "+%Y-%m-%dT%H:%M:%S%z%Z")
SNAPSHOT="${REPO}-${STAMP}"

DEBS=$(find . -name "*.deb")

for f in ${DEBS}; do
  aptly_api_cli --file_upload ${UPLOAD} $f
done

for f in ${DEBS}; do
  aptly_api_cli --repo_add_package_from_upload ${REPO} ${UPLOAD} $(basename $f)
done

aptly_api_cli --snapshot_create_from_local_repo ${SNAPSHOT} ${REPO}

aptly_api_cli --publish_switch=gtm-deb-repo:debian ${SNAPSHOT} stretch main 1

