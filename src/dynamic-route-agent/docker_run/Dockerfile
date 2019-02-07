FROM dynamicconfig-python-base:latest
WORKDIR /dynamicrouteagent
ADD ./../ /dynamicrouteagent
RUN . -v -e dynamicrouteagent:main -o dynamicrouteagent.pex --disable-cache
CMD ["./dynamicrouteagent.pex", "--asgn", "int-test-backend-server", "--rcp", " ./route.conf", "--rn", "banner_client", "--cn", "banner_client_name", "--pn", "8080"] 
