## This is just a sandbox for mucking around with your gRPC protos

```
mkvirtualenv grpc
#or
workon grpc

pip install -r ./requirements.txt 
```

## Generate Python gRPC Files

```
python -m grpc_tools.protoc --proto_path=../apikey/src/main/proto --python_out=./src/ --grpc_python_out=./src/ ../apikey/src/main/proto/apikey.proto
```

## Resources

[gRPC Python](https://grpc.io/docs/quickstart/python.html)
[Google Protocol Buffers](https://developers.google.com/protocol-buffers/docs/overview "Google Protocol Buffers")
