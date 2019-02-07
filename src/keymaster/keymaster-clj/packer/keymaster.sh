#!/bin/bash -xe

sudo apt-get update
sudo apt-get install -y -t stretch-backports openjdk-8-jre-headless ca-certificates-java
sudo apt-get install -y supervisor

sudo -u admin mkdir -p ~admin/keymaster
cd ~admin/keymaster
sudo -u admin aws s3 cp --recursive 's3://nfe-bakery/keymaster/0.1.0-SNAPSHOT' .

cat <<EOC > ./keymaster.conf
[program:main]
command = java -cp keymaster-0.1.0-SNAPSHOT.jar:. keymaster.server 0.0.0.0 6666
directory = /home/admin/keymaster
user = admin
environment = AWS_DEFAULT_REGION=us-west-2
autostart = true
priority = 99
startsecs = 5
startretries = 999
[group:keymaster]
programs = main
EOC

sudo install ./keymaster.conf /etc/supervisor/conf.d/keymaster.conf
rm ./keymaster.conf
