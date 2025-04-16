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

#git submodule update --remote aggregator-webui/

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

# manual update
# ls -1 aggregator-webui/dist/index.html

# --------------------------------------------------------------------------

# build core + app
# mvn clean package
