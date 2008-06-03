#!/bin/bash

for JAVA in "$JAVA_HOME/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x $JAVA ]
  then
    break
  fi
done

if [ ! -x $JAVA ]
then
  echo "Unable to locate Java. Please set JAVA_HOME environment variable."
  exit
fi

# start Red5
echo "Starting Red5..."
$JAVA -Xrs -Xms512M -Xmx1024M -Xss128K -XX:NewSize=256m -XX:SurvivorRatio=16 -XX:MinHeapFreeRatio=20 -XX:+AggressiveHeap -XX:+ExplicitGCInvokesConcurrent -XX:+UseConcMarkSweepGC -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000 -Djava.net.preferIPv4Stack=true -Xverify:none -Djava.security.policy=conf/red5.policy -cp red5.jar:conf:$CLASSPATH org.red5.server.Standalone  >> ${RED5_HOME}/log/jvm.stdout 2>&1 &
