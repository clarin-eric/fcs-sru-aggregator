#!/bin/bash

ASSETDIR=src/main/resources/assets
LIBDIR=$ASSETDIR/lib
FONTDIR=$ASSETDIR/fonts
JSDIR=$ASSETDIR/js

RUN_BOWER=
BUILD_JSX=1
BUILD_JAR=
BUILD_RPM=
RUN_JAR=
RUN_JAR_PRODUCTION=

while [[ $# > 0 ]]
do
key="$1"
# echo $# " :" $key
case $key in
    --bower)
    RUN_BOWER=1
    ;;
    --jsx)
    BUILD_JSX=1
    ;;
    --jar)
    BUILD_JAR=1
    ;;
    --rpm)
    BUILD_RPM=1
    BUILD_JAR=
    ;;
    --run)
    RUN_JAR=1
    ;;
    --run-production)
    RUN_JAR_PRODUCTION=1
    ;;
    *)
    echo "Unknown option:" $1
    exit 1
    ;;
esac
shift
done

if [ $RUN_BOWER ]
then
	mkdir -p $LIBDIR
	mkdir -p $FONTDIR
	mkdir -p $JSDIR

	npm install bower react-tools
	node_modules/bower/bin/bower install jquery bootstrap react react-addons font-awesome

	cp bower_components/bootstrap/dist/css/bootstrap.min.css $LIBDIR/
	cp bower_components/bootstrap/dist/js/bootstrap.min.js $LIBDIR/
	cp bower_components/jquery/dist/jquery.min.js $LIBDIR/
	cp bower_components/jquery/dist/jquery.min.map $LIBDIR/
	cp bower_components/react/react-with-addons.js $LIBDIR/
	cp bower_components/react/react-with-addons.min.js $LIBDIR/
	cp bower_components/font-awesome/css/font-awesome.min.css $LIBDIR/

	cp bower_components/bootstrap/fonts/*  $FONTDIR/
	cp bower_components/font-awesome/fonts/* $FONTDIR/
fi

if [ $BUILD_JSX ]
then
	echo; echo "---- jsx"
	for f in $JSDIR/*.jsx; do
		cp -v $f $JSDIR/`basename $f .jsx`.js;
	done
	node_modules/react-tools/bin/jsx --no-cache-dir $JSDIR $JSDIR
fi

if [ $BUILD_JAR ]
then
	echo; echo "---- mvn clean package"
	mvn -q clean package
fi

if [ $BUILD_RPM ]
then
	echo; echo "---- mvn clean package jdeb:jdeb rpm:rpm"
	mvn clean package jdeb:jdeb rpm:rpm
fi

if [ $RUN_JAR ]
then
	echo; echo "---- run devel"
	JAR=`find target -iname 'aggregator-*.jar'`
	echo java -cp src/main/resources:$JAR eu.clarin.sru.fcs.aggregator.app.Aggregator server aggregator_devel.yml
	java -cp src/main/resources:$JAR eu.clarin.sru.fcs.aggregator.app.Aggregator server aggregator_devel.yml
fi

if [ $RUN_JAR_PRODUCTION ]
then
	echo; echo "---- run production"
	JAR=`find target -iname 'aggregator-*.jar'`
	echo java -jar $JAR server aggregator.yml
	java -jar $JAR server aggregator.yml
fi
