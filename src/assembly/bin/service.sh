#!/bin/bash
#
# This is a simple startup script that should work for the
# dropwizard based web services.
#

APPNAME=$(basename $0)
BASEDIR=${BASEDIR:-.}
CONFFILE=${CONFFILE:-${BASEDIR}/${APPNAME}.yaml}
HEAPSIZE=${HEAPSIZE:-4096m}
JARFILE=${JARFILE:-${BASEDIR}/${APPNAME}.jar}

while getopts ":c:h:" opt; do
    case $opt in
        c) CONFFILE=${OPTARG}
        ;;
        h) HEAPSIZE=${OPTARG}
        ;;
    esac
done

cd ${BASEDIR}
exec java -Xmx${HEAPSIZE} -jar ${JARFILE} server ${CONFFILE}
