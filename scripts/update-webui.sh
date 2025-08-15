#!/bin/bash

# --------------------------------------------------------------------------
# https://stackoverflow.com/a/246128/9360161

SOURCE=${BASH_SOURCE[0]}
while [ -L "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
  SOURCE=$(readlink "$SOURCE")
  [[ $SOURCE != /* ]] && SOURCE=$DIR/$SOURCE # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )

cd $DIR/..

# --------------------------------------------------------------------------

set -eu

# --------------------------------------------------------------------------

# update WebUI submodule
#git submodule update --remote aggregator-webui/

# --------------------------------------------------------------------------

# copy configuration (environment variables) to modify build
FN_ENV=webui.env
FN_CUSTOMIZATION_ENV=aggregator-webui/.env

# TODO: clean up any .env file to avoid unwanted overrides?
[[ -f "${FN_CUSTOMIZATION_ENV}" ]] && rm -v ${FN_CUSTOMIZATION_ENV}
[[ -f "${FN_ENV}" ]] && cp -v "${FN_ENV}" ${FN_CUSTOMIZATION_ENV}

# --------------------------------------------------------------------------

pushd aggregator-webui

# build webui
npm install
npm run build

popd

# copy webui resources into app
rm -r aggregator-app/src/main/resources/assets/webapp
mkdir aggregator-app/src/main/resources/assets/webapp
cp -R aggregator-webui/dist/lib/. aggregator-app/src/main/resources/assets/webapp/

# cleanup map files
find aggregator-app/src/main/resources/assets/webapp/ -name "*.map" -delete

# manual update
#ls -1 aggregator-webui/dist/index.html

# --------------------------------------------------------------------------

# build core + app
#mvn clean package
