# Running DynamoDb Locally - the lazy way

Reference: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html

Build
```
docker build -t dynamo .
```

Run
```
docker run -p 8000:8000 dynamo
```

Check
```
aws dynamodb list-tables --endpoint-url http://localhost:8000
```
