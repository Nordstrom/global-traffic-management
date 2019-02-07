# Running Gatekeeper & DynamoDb Locally - the lazy way


## Build Images 
Note: `docker_build_prep` builds the gatekeeper deb and places it in the Dockerfile context scope
```
./gatekeeper/docker_build_prep.sh
docker-compose build
```

## Rebuild Just Gatekeeper after source changes
```
./gatekeeper/docker_build_prep.sh
docker-compose build --no-cache gatekeeper
```

## Run Containers
```
docker-compose up
```
