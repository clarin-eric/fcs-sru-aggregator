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

cd aggregator-app

DEBUGGER_OPTS=
#DEBUGGER_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005"

JAR=`find target -iname 'aggregator-app-*.jar'`
java $DEBUGGER_OPTS -cp src/main/resources:$JAR -Xmx4096m eu.clarin.sru.fcs.aggregator.app.AggregatorApp server aggregator_devel.yml
