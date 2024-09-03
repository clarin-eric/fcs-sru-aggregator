# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
FROM node:16.20.2-bookworm AS web

WORKDIR /work

COPY package.json package-lock.json build.sh /work/
RUN npm install --legacy-peer-deps

COPY src/main/resources/assets /work/src/main/resources/assets
RUN ./build.sh --npm
RUN ./build.sh --jsx-force

# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-11-focal AS jar

WORKDIR /work

# might be required for caching of maven deps?
#ENV MAVEN_OPTS=-Dmaven.repo.local=./.m2/

COPY pom.xml /work/
RUN mvn -q dependency:resolve-plugins
RUN mvn -q dependency:resolve
#RUN mvn -q dependency:go-offline

COPY src /work/src
COPY build.sh /work/
COPY --from=web /work/src/main/resources/assets /work/src/main/resources/assets

RUN ./build.sh --jar
#RUN mvn -o -q clean package

# ---------------------------------------------------------------------------
# https://github.com/docker-library/openjdk/issues/505
# TODO: upgrade to 17
FROM eclipse-temurin:11-jre-jammy AS run

WORKDIR /app

COPY --from=jar /app/target/aggregator-*.jar /app/target/
COPY build.sh /app/
COPY aggregator.yml /app/

EXPOSE 4019

ENTRYPOINT [ "./build.sh", "--run-production" ]
