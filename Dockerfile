FROM maven:3.8.6-openjdk-18-slim AS build
ARG APP_DIR=/opt/matsim
ARG APP_DIR
WORKDIR ${APP_DIR}
COPY . ./

