#!/bin/bash -xe

sudo apt-get update
sudo apt-get install -y -t stretch-backports openjdk-8-jre-headless ca-certificates-java
sudo apt-get install -y supervisor

sudo -u admin mkdir -p ~admin/gatekeeper
cd ~admin/gatekeeper
sudo -u admin aws s3 cp --recursive 's3://nfe-bakery/gatekeeper/0.1.0-SNAPSHOT' .

cat <<EOC > ./gatekeeper.conf
[program:main]
command = java -cp gatekeeper-0.1.0-SNAPSHOT.jar:. gatekeeper.server 0.0.0.0 6666
directory = /home/admin/gatekeeper
user = admin
environment = AWS_DEFAULT_REGION=us-west-2
autostart = true
priority = 99
startsecs = 5
startretries = 999
[group:gatekeeper]
programs = main
EOC

sudo install ./gatekeeper.conf /etc/supervisor/conf.d/gatekeeper.conf
rm ./gatekeeper.conf
