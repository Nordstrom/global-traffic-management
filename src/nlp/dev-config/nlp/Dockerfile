FROM openjdk:8u171-stretch
COPY nlp-proxy/build/install /opt
COPY dev-config/nlp/application.conf /opt/nlp/config/application.conf
COPY dev-config/nlp/route.json /opt/nlp/config/route.json
EXPOSE 8444
ENV NLP_OPTS="-DNLP-ENV=PROD -DNLP-LOGLEVEL=DEBUG"
CMD ["/opt/nlp/bin/nlp", "/opt/nlp/config/application.conf", "/opt/nlp/config/route.json"]
