#!/bin/bash

if [ ! -e bower_components ]
then
	npm install bower react-tools
	node_modules/bower/bin/bower install jquery bootstrap react react-addons react-bootstrap

	cp bower_components/bootstrap/dist/css/bootstrap.min.css src/main/webapp/lib/
	cp bower_components/bootstrap/dist/js/bootstrap.min.js src/main/webapp/lib/
	cp bower_components/jquery/dist/jquery.min.js src/main/webapp/lib/
	cp bower_components/react/react-with-addons.js src/main/webapp/lib/
	cp bower_components/react/react-with-addons.min.js src/main/webapp/lib/
	cp bower_components/react-bootstrap/react-bootstrap.min.js src/main/webapp/lib/
fi

JSDIR=src/main/webapp/js
for f in $JSDIR/*.jsx; do 
	cp -v $f $JSDIR/`basename $f .jsx`.js; 
done
node_modules/react-tools/bin/jsx --no-cache-dir $JSDIR $JSDIR

#mvn clean package
