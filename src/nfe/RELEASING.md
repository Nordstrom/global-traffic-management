```
$ (export VERSION=1.0.0
export NEXT=1.1.0-SNAPSHOT
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=${VERSION} -Prelease.newVersion=${NEXT})
```
