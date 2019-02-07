#!/bin/bash

HERE=$(cd $(dirname $0) && /bin/pwd)

DRA_VERSION=$(cat $HERE/VERSION)
DRA_BASENAME=dynamicrouteagent

die() {
  echo "$@"
  exit 1
}

make-deb() {
  pkg_version=${DRA_VERSION}-1
  pkg_root=${DRA_BASENAME}_${pkg_version}

  # install the files in the target directory hierarchy
  target_app_dir=$pkg_root/opt/${DRA_BASENAME}
  mkdir -p $target_app_dir
  cp $HERE/dist/*.pex $target_app_dir

  cronjob_dir=etc/cron.d
  target_cronjob_dir=$pkg_root/$cronjob_dir
  mkdir -p $target_cronjob_dir
  cp $HERE/$cronjob_dir/dra $target_cronjob_dir

  # add the package metadata
  mkdir $pkg_root/DEBIAN
  cat >$pkg_root/DEBIAN/control <<EOF
Package: ${DRA_BASENAME}
Version: $pkg_version
Section: base
Priority: optional
Architecture: amd64
Maintainer: YOU <YOU@yourdomain.com>
Description: Dynamic Route Agent (DRA)
 DRA is python based side car that looks up ip addresses of ec2 instances by tag/name 
EOF

  # build the package
  fakeroot dpkg-deb --build $pkg_root || die "Failed creating package"
}


mkdir -p $HERE/dra-build
cd $HERE/dra-build
make-deb
