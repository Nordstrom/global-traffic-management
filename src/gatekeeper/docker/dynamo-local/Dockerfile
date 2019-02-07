FROM openjdk:8u171-stretch
RUN mkdir /ddb && \
    cd /ddb && \
    apt-get install curl && \
    curl -o ddb.tar.gz https://s3-us-west-2.amazonaws.com/dynamodb-local/dynamodb_local_latest.tar.gz && \
    tar -xf ddb.tar.gz
CMD ["java", "-Djava.library.path=/ddb/DynamoDBLocal_lib", "-jar", "/ddb/DynamoDBLocal.jar", "-sharedDb"]
