#!/bin/bash

mkdir app
cd app
aws s3 cp s3://nonprod-taco-integration-testing-helpers/nlp.jar nlp.jar
aws s3 cp s3://nonprod-taco-integration-testing-helpers/dynamicrouteagent.pex dynamicrouteagent.pex
chmod 755 dynamicrouteagent.pex
aws s3 cp s3://nonprod-taco-integration-testing-helpers/application.conf application.conf
aws s3 cp s3://nonprod-taco-integration-testing-helpers/route.conf route.conf

watch -n 10 ./dynamicrouteagent.pex --asgn int-test-backend-server --rcp ./route.conf --rn banner --cn banner_name --pn 8080 > route.log &

java -cp nlp.jar:. com.nordstrom.nlp.Main application.conf route.conf &
