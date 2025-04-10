#!/bin/bash

set -eu

# prepare
mvn dependency:resolve-plugins
mvn dependency:resolve

pushd aggregator-webui
npm install
popd

# build core
mvn -pl aggregator-core clean package

# build webui
pushd aggregator-webui
npm run build
popd

# copy webui resources into app
rm -r aggregator-app/src/main/resources/assets/webapp
mkdir aggregator-app/src/main/resources/assets/webapp
cp -R aggregator-webui/dist/lib/. aggregator-app/src/main/resources/assets/webapp/

# manual update
# ls -1 aggregator-webui/dist/index.html

# build app
mvn -pl aggregator-app clean package
