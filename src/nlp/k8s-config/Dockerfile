# This is the production docker file used in Kubernetes
FROM openjdk:8-jre
COPY nlp-proxy/build/install /opt
COPY k8s-config/application.conf /opt/nlp/config/application.conf
COPY k8s-config/route.json /opt/nlp/config/route.json
# todo: (WK) change log level for releases
ENV NLP_OPTS="-DNLP-ENV=PROD -DNLP-LOGLEVEL=DEBUG"
CMD ["/opt/nlp/bin/nlp", "/opt/nlp/config/application.conf", "/opt/nlp/config/route.json"]
