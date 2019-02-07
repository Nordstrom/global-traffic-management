# Helpful scripts for integration testing (WIP)

The userData.sh downloads the jar file, application.conf, route.conf, and dynamicrouteagent.pex from S3.
If you want to override the values that we use you should run the './gradlew buildShadowJarAndUploadTestArtifactsToS3'
this will build a new shadowJar based on the current nlp and upload it to our private S3 bucket.
This command will also take the application.conf and route.conf in the int-test-config
folder and upload that to S3 as well.  If you want to update the dynamicrouteagent you can change that from the dynamicrouteagent repo.


Teardown existing infrastructure for test nlp
---
* ./destroyNLP.sh
  or
* ./teardown.sh int-test-nlp

Teardown existing infrastructure for test backend
---
* ./destroyBackend.sh
  or
* ./teardown.sh int-test-backend-server

Create new test nlp launchconfiguration and asg
---
* ./createNLP.sh
  or
* python create_asg.py --key_name kratosmac --name int-test-nlp --user_data ./userData.sh

Create new test backend launchconfiguration and asg
---
* ./createBackend.sh
  or
* python create_asg.py --key_name kratosmac --name int-test-backend-server

ScaleDown test nlp
---
* ./resize_asg.sh 0 0 int-test-nlp

ScaleUp test nlp
---
* ./resize_asg.sh 1 1 int-test-nlp

ScaleDown test backend server
---
* ./resize_asg.sh 0 0 int-test-backend-server

ScaleUp test backend server
---
* ./resize_asg.sh 1 1 int-test-backend-server

Example of how to launch nlp
---
* java -cp ./int-test-config:./build/libs/nlp-0.1.1-SNAPSHOT-all.jar: com.nordstrom.nlp.Main int-test-config/application.conf int-test-config/route.conf
