#!/bin/bash

# JAVA options
JAVA_OPTS="-Dred5.root=/opt/red5"
LOGBACK_OPTS="-Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true"
SECURITY_OPTS="-Djava.security.manager -Djava.security.debug=failure -Djava.security.policy=conf/red5.policy"
# Jython options
JYTHON="-Dpython.home=lib"

for JAVA in "/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java" "$JAVA_HOME/bin/java" "/usr/bin/java" "/usr/local/bin/java"
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

#JAVA=/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java

# start Red5
echo "Starting Red5..."
exec $JAVA $JYTHON $JAVA_OPTS $LOGBACK_OPTS $SECURITY_OPTS -cp red5.jar org.red5.server.Bootstrap 1>log\stdout.log 2>log\stderr.log
