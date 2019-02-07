from concurrent import futures
import time
import grpc

import apikey_pb2
import apikey_pb2_grpc

_ONE_DAY_IN_SECONDS = 60 * 60 * 24


class ApiKeyService(apikey_pb2_grpc.ApiKeyerServicer):

    def GenerateNewApiKey(self, request, context):

        return apikey_pb2.ApiKey(service_name=request.service_name, team_name=request.team_name, key_name=request.key_name, key='1234567890')

    def RevokeApiKey(self, request, context):
        return apikey_pb2.Empty()

    def ListApiKeys(self, request, context):
        return apikey_pb2.ApiKeyList()


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    apikey_pb2_grpc.add_ApiKeyerServicer_to_server(ApiKeyService(), server)
    server.add_insecure_port('[::]:8443')
    server.start()
    try:
        while True:
            time.sleep(_ONE_DAY_IN_SECONDS)
    except KeyboardInterrupt:
        server.stop(0)


if __name__ == '__main__':
    serve()
