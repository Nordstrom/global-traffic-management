#!/bin/bash

MIN=$1
MAX=$2
NAME=$3

aws autoscaling update-auto-scaling-group --auto-scaling-group-name $NAME --min-size $MIN --max-size $MAX \
  --region us-west-2 

