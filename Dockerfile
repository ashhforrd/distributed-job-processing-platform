FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG MODULE

WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY job-api/pom.xml job-api/pom.xml
COPY job-worker/pom.xml job-worker/pom.xml

COPY common/src common/src
COPY job-api/src job-api/src
COPY job-worker/src job-worker/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre

ARG MODULE

WORKDIR /app

COPY --from=build \
    /workspace/${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar \
    app.jar

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
