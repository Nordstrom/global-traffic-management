#!/bin/bash
NAME=int-test-backend-server
aws autoscaling delete-auto-scaling-group --auto-scaling-group-name $NAME --force-delete --region us-west-2
aws autoscaling delete-launch-configuration --launch-configuration-name $NAME --region us-west-2
