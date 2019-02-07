# [Artifactory](https://artifactory.yourdomain.com)

### did you remember to setup your artifactory credentials?

```
$ cat ~/.gradle/gradle.properties
artifactory_user=<your username>
artifactory_api_key=<your key>
```

### verify your artifactory config

```
./gradlew verifyArtifactoryConfig
```

### tag the release and push to artifactory
``` 
./gradlew release
```
