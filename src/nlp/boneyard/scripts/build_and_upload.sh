#!/bin/bash

cd ..
#./gradlew pushJarToS3
find ./build/libs -name *-SNAPSHOT-all.jar -execdir cp {} SNAPSHOT.jar \;
python3 ./scripts/upload_artifacts_to_s3.py --local_name ./build/libs/SNAPSHOT.jar --s3_name nlp.jar --application_conf ./int-test-config/application.conf --route_conf ./int-test-config/route.conf
