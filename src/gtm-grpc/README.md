# GTM gRPC

This repo contains proto definitions for GTM.

[![pipeline status](https://gitlab.yourdomain.com/gtm/grpc/badges/master/pipeline.svg)](https://gitlab.yourdomain.com/gtm/grpc/commits/master)

-----------------------------

### Adding Modules

Modules allow you to release separate artifacts.

1. create a directory and `<dir>/build.gradle` containing `project.artifact_id = <your artifact name>`
1. add entry in `settings.gradle` for your new module
1. add proto files to `<dir>/src/main/proto`

-----------------------------

### Publishing Module Releases

See [RELEASING.md](./RELEASING.md)
```
./gradlew :release
```

##### Publish to Maven Local (try out SNAPSHOTs)

```
./gradlew :apikey:publishToMavenLocal
```

Example build.gradle:
```
repositories {
        jcenter()
        maven {
          credentials {
            username "$artifactory_user"
            password "$artifactory_api_key"
          }
          url = 'https://artifactory.yourdomain.com/artifactory/maven'
        }
}

dependencies {
    implementation group: 'com.nordstrom.gtm', name: 'apikey', version: '0.1.0-SNAPSHOT'
}
```

### Resources

* [proto3 Language Guide](https://developers.google.com/protocol-buffers/docs/proto3)
* [proto3 Naming Conventions](https://cloud.google.com/apis/design/naming_convention)
