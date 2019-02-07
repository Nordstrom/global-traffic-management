#!/usr/bin/env bash

eval $(minikube docker-env)
./gradlew installDist && docker build -f k8s-config/Dockerfile -t nlp .
