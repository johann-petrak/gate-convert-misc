#!/bin/bash

origdir="$1"
destdir="$2"
if [[ "x$destdir" == x ]]
then
  echo Need two parameters, origdir and destdir
  exit 1
fi

export JAVA_OPTS=${JAVA_OPTS:=-Xmx2G}

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`


iconv -f windows-1252 -t utf-8 < "${origdir}"/rt-polarity.pos | groovy -cp $GATE_HOME/bin/gate.jar:$GATE_HOME/lib/'*' "$SCRIPTDIR"/sentences2gate.groovy "$destdir" rt-polirity.pos pos
iconv -f windows-1252 -t utf-8 < "${origdir}"/rt-polarity.neg | groovy -cp $GATE_HOME/bin/gate.jar:$GATE_HOME/lib/'*' "$SCRIPTDIR"/sentences2gate.groovy "$destdir" rt-polarity.neg neg
