#!/bin/bash

for JAVA in "$JAVA_HOME/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x $JAVA ]
  then
    break
  fi
done

exec $JAVA -cp red5.jar:$CLASSPATH org.red5.server.Standalone
