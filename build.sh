#!/bin/bash

ASSETDIR=src/main/resources/assets
LIBDIR=$ASSETDIR/lib

if [ ! -e bower_components ]
then
	npm install bower react-tools
	node_modules/bower/bin/bower install jquery bootstrap react react-addons font-awesome

	mkdir -p src/main/webapp/lib
	cp bower_components/bootstrap/dist/css/bootstrap.min.css $LIBDIR
	cp bower_components/bootstrap/dist/js/bootstrap.min.js $LIBDIR
	cp bower_components/jquery/dist/jquery.min.js $LIBDIR
	cp bower_components/react/react-with-addons.js $LIBDIR
	cp bower_components/react/react-with-addons.min.js $LIBDIR
	cp bower_components/font-awesome/css/font-awesome.min.css $LIBDIR

	mkdir -p src/main/webapp/fonts
	cp bower_components/bootstrap/fonts/*  $ASSETDIR/fonts/
	cp bower_components/font-awesome/fonts/* $ASSETDIR/fonts/
fi

JSDIR=$ASSETDIR/js
for f in $JSDIR/*.jsx; do 
	cp -v $f $JSDIR/`basename $f .jsx`.js; 
done
node_modules/react-tools/bin/jsx --no-cache-dir $JSDIR $JSDIR

#mvn -q clean package

# Run in production:
#java -jar target/Aggregator2-2.0.0-alpha-6.jar server aggregator.yml

# Run for development:
#java -cp src/main/resources:target/Aggregator2-2.0.0-alpha-6.jar eu.clarin.sru.fcs.aggregator.app.Aggregator server aggregator_development.yml
