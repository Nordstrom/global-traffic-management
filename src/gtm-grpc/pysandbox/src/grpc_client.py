from __future__ import print_function

import grpc

import apikey_pb2
import apikey_pb2_grpc


def run():
    channel = grpc.insecure_channel('localhost:8443')
    stub = apikey_pb2_grpc.ApiKeyerStub(channel)
    response = stub.GenerateNewApiKey(apikey_pb2.KeyRequest(team_name='my team', service_name='awesome service', key_name='wonderful key'))
    print("Client received: " + response.team_name)
    print("Client received: " + response.service_name)
    print("Client received: " + response.key_name)
    print("Client received: " + response.key)


if __name__ == '__main__':
    run()
