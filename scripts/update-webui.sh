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

# init on first run
#git submodule update --init --recursive aggregator-webui/

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

# inject some maven project info

#APPL_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
APPL_VERSION=$(xmlstarlet sel -N "pom=http://maven.apache.org/POM/4.0.0" -t -m "/pom:project/pom:version" -v . pom.xml)

# short commit hash
GIT_SHORT_SHA=$(git rev-parse --short HEAD)
# commit date
GIT_DATE=$(git log -1 --pretty="format:%cI")
# short branch name
GIT_SHORT_REF=$(git branch --show-current)
#GIT_SHORT_REF=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
# tag (at HEAD)
GIT_TAG=$(git tag --points-at HEAD)
# last tag in current branch
GIT_LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)


touch ${FN_CUSTOMIZATION_ENV}
sed -i '/^VITE_APPLICATION_VERSION/d' ${FN_CUSTOMIZATION_ENV}
echo "VITE_APPLICATION_VERSION=${APPL_VERSION}" >> ${FN_CUSTOMIZATION_ENV}
sed -i '/^VITE_GIT_APP/d' ${FN_CUSTOMIZATION_ENV}
echo "VITE_GIT_APP_INFO_SHA=${GIT_SHORT_SHA}" >> ${FN_CUSTOMIZATION_ENV}
echo "VITE_GIT_APP_INFO_REF=${GIT_SHORT_REF}" >> ${FN_CUSTOMIZATION_ENV}
echo "VITE_GIT_APP_INFO_TAG=${GIT_TAG}" >> ${FN_CUSTOMIZATION_ENV}
echo "VITE_GIT_APP_INFO_DATE=${GIT_DATE}" >> ${FN_CUSTOMIZATION_ENV}

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
