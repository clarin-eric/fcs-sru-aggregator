# Dockerfile that build the whole application from ground up
# - create webui (to account for customization)
# - build application (note that webui version MUST NOT change!)
# - create minimal run image with default configuration

# ---------------------------------------------------------------------------
FROM node:22.19.0-trixie-slim AS web

RUN set -ex; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        git \
        xmlstarlet \
    ; \
	rm -rf /var/lib/apt/lists/*

WORKDIR /work

# initialize .git folder structure for submodule checkout
COPY .git /work/.git
COPY .gitmodules /work/
COPY aggregator-webui /work/aggregator-webui

# submodule init (in case not yet done, why we also need the .git* stuff)
RUN set -ex; \
    git config --global --add safe.directory /work/ ; \
    git submodule update --init --recursive aggregator-webui/ ; \
    git config --global --add safe.directory /work/aggregator-webui

# copy source code
RUN mkdir -p /work/aggregator-app/src/main/resources/assets/webapp/
COPY scripts/update-webui.sh /work/scripts/update-webui.sh
COPY pom.xml /work/
# "conditional" copy, will not fail if file does not exist
COPY webui.env* /work/

RUN ./scripts/update-webui.sh

# ---------------------------------------------------------------------------
FROM maven:3.9.11-eclipse-temurin-21-noble AS jar

WORKDIR /work

# might be required for caching of maven deps?
#ENV MAVEN_OPTS=-Dmaven.repo.local=./.m2/

# pre-install dependencies
COPY pom.xml /work/
COPY aggregator-core/pom.xml /work/aggregator-core/pom.xml
COPY aggregator-app/pom.xml /work/aggregator-app/pom.xml

RUN mvn -B dependency:resolve-plugins
RUN mvn -B dependency:resolve
#RUN mvn -q dependency:go-offline

# copy source code
COPY aggregator-core/src /work/aggregator-core/src
COPY aggregator-app/src /work/aggregator-app/src
COPY scripts/build.sh /work/scripts/build.sh

# copy webui artefacts
COPY --from=web /work/aggregator-app/src/main/resources/assets/webapp /work/aggregator-app/src/main/resources/assets/webapp

RUN ./scripts/build.sh

# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble AS run

WORKDIR /app

# application
COPY --from=jar /work/aggregator-app/target/aggregator-app-*.jar /app/aggregator-app.jar
# default configuration
COPY aggregator-app/aggregator.yml /app/

EXPOSE 4019

ENTRYPOINT [ "java", "-Xmx4096m", "-jar", "aggregator-app.jar", "server", "aggregator.yml" ]
