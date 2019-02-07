FROM openjdk:8u171-stretch
COPY distributions/gatekeeper.deb /tmp/gatekeeper.deb
RUN apt-get update && apt install -y /tmp/gatekeeper.deb
ENV JAVA_OPTS="-DLOCAL_DEV=1"
CMD ["/opt/gatekeeper-server/bin/gatekeeper-server"]
