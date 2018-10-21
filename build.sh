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
    --jar-debug)
    BUILD_JAR_DEBUG=1
    ;;
    --rpm)
    BUILD_RPM=1
    BUILD_JAR=
    BUILD_JAR_DEBUG=
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

	npm install bower browserify babelify babel-cli babel-preset-es2015 babel-preset-react babel-preset-env prop-types create-react-class react-addons-linked-state-mixin react-transition-group react-i18next codemirror 
	node_modules/bower/bin/bower install jquery bootstrap react font-awesome

	cp bower_components/bootstrap/dist/css/bootstrap.min.css $LIBDIR/
	cp bower_components/bootstrap/dist/css/bootstrap.min.css.map $LIBDIR/
	cp bower_components/bootstrap/dist/js/bootstrap.min.js $LIBDIR/
	cp bower_components/jquery/dist/jquery.min.js $LIBDIR/
	cp bower_components/jquery/dist/jquery.min.map $LIBDIR/
	cp bower_components/react/react.development.js $LIBDIR/
	cp bower_components/react/react.production.min.js $LIBDIR/
	cp bower_components/react/react-dom.development.js $LIBDIR/
	cp bower_components/react/react-dom.production.min.js $LIBDIR/
	cp bower_components/font-awesome/css/font-awesome.min.css $LIBDIR/
	cp bower_components/bootstrap/fonts/*  $FONTDIR/
	cp bower_components/font-awesome/fonts/* $FONTDIR/
fi

if [ $BUILD_JSX ]
then
	echo; echo "---- jsx"
	for f in $JSDIR/*.jsx; do
	    jsxtime=`stat -c %Y ${f}`
	    jstime=""
	    if [ -e ${f%.jsx}.js ]; then
		jstime=`stat -c %Y ${f%.jsx}.js`
	    fi
	    for subres in $(find $JSDIR/{pages,components}/ -name '*.jsx'); do
		jsxsubtime=`stat -c %Y ${subres}`
		if [ ${jsxsubtime} -gt ${jsxtime} ]; then
		    jsxtime=${jsxsubtime};
		fi
	    done

	    if [ "${jstime}" == "" ] || [ ${jsxtime} -gt ${jstime} ]; then
		echo "${f}";
		node_modules/.bin/browserify -t [ babelify --presets [ es2015 react ] ] ${f} -o ${f%.jsx}.js;
		if [ $? -gt 0 ]; then
		    rm -f ${f%.jsx}.js;
		    if [ -e ${f%.jsx}.js ]; then 
			echo "Removed output ${f%.jsx}.js since the return value is greater than 0 ..."
		    fi
		fi
	    else
		echo "${f} is already up-to-date."
	    fi
	done

fi

if [ $BUILD_JAR ]
then
	echo; echo "---- mvn clean package"
	mvn -q clean package
fi

if [ $BUILD_JAR_DEBUG ]
then
	echo; echo "---- mvn clean package debug"
	mvn -X -q clean package
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
	echo java -cp src/main/resources:$JAR -Xmx4096m eu.clarin.sru.fcs.aggregator.app.Aggregator server aggregator_devel.yml
	java -cp src/main/resources:$JAR -Xmx4096m eu.clarin.sru.fcs.aggregator.app.Aggregator server aggregator_devel.yml
fi

if [ $RUN_JAR_PRODUCTION ]
then
	echo; echo "---- run production"
	JAR=`find target -iname 'aggregator-*.jar'`
	echo java -Xmx4096m -jar $JAR server aggregator.yml
	java -Xmx4096m -jar $JAR server aggregator.yml
fi
