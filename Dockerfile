FROM maven:3.8.6-openjdk-18-slim AS build
ARG APP_DIR=/opt/matsim
ARG APP_DIR
WORKDIR ${APP_DIR}
COPY . ./
RUN apt-get update && apt-get install -y \
    figlet \
    && rm -rf /var/lib/apt/lists/*
WORKDIR ${APP_DIR}
RUN mvn -f pom.xml -DskipTests -P dockerready clean package \
    && echo "$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout=true)" > VERSION.txt \
    && figlet -f slant "MATSim $(cat VERSION.txt)" > BANNER.txt \
    && echo "Image build date: $(date --iso-8601=seconds)" >> BANNER.txt
    
FROM openjdk:18-slim
ARG APP_DIR
WORKDIR ${APP_DIR}
USER root
COPY --chmod=0755 docker-entrypoint.sh ./
COPY --from=build ${APP_DIR}/*.txt ./resources/
COPY --from=build ${APP_DIR}/target/matsim-bundle.jar ./matsim.jar
RUN chmod +x ./matsim.jar
ENV MATSIM_HOME=${APP_DIR} \
    MATSIM_INPUT=${APP_DIR}/data/input \
    MATSIM_OUTPUT=${APP_DIR}/data/output
ENV INIRAM '-Xms16g'
ENV MAXRAM '-Xms16g'
ARG COMMIT
ENV COMMIT ${COMMIT}
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${MATSIM_INPUT} \
    && mkdir -p ${MATSIM_OUTPUT}
VOLUME ${APP_DIR}/data
ENTRYPOINT ["./docker-entrypoint.sh"]
