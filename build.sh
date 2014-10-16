#!/bin/bash

if [ ! -e bower_components ]
then
	npm install bower react-tools
	node_modules/bower/bin/bower install jquery bootstrap react react-addons react-bootstrap
fi

JSDIR=src/main/webapp/js
for f in $JSDIR/*.jsx; do cp $f $JSDIR/`basename $f .jsx`.js; done
node_modules/react-tools/bin/jsx --no-cache-dir $JSDIR $JSDIR

mvn clean
mvn package