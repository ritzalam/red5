#!/bin/bash

if [ -z "$RED5_HOME" ]; then 
  export RED5_HOME=`pwd`; 
fi

P=":" # The default classpath separator
OS=`uname`
case "$OS" in
  CYGWIN*|MINGW*) # Windows Cygwin or Windows MinGW
  P=";" # Since these are actually Windows, let Java know
  ;;
  Darwin*)

  ;;
  SunOS*)
      if [ -z "$JAVA_HOME" ]; then 
          export JAVA_HOME=/opt/local/java/sun6; 
      fi
  ;;
  *)
  # Do nothing
  ;;
esac

echo "Running on " $OS

YOURKIT="-agentpath:/home/firstuser/yourkit/yjp-12.0.3/bin/linux-x86-32/libyjpagent.so=disablestacktelemetry,disableexceptiontelemetry,builtinprobes=none,delay=10000"

# JAVA options
# You can set JAVA_OPTS to add additional options if you want
JVM_OPTS="-Xverify:none -XX:+TieredCompilation -XX:+UseBiasedLocking -XX:+UseStringCache -XX:+OptimizeStringConcat -XX:+UseParallelOldGC -XX:+UseParallelGC -XX:+UseParNewGC -XX:InitialCodeCacheSize=8m -XX:ReservedCodeCacheSize=32m -Dorg.terracotta.quartz.skipUpdateCheck=true"
# Set up logging options
LOGGING_OPTS="-Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true"
# Set up security options
SECURITY_OPTS="-Djava.security.debug=failure"
export JAVA_OPTS="$LOGGING_OPTS $SECURITY_OPTS $JAVA_OPTS"

if [ -z "$RED5_MAINCLASS" ]; then
  export RED5_MAINCLASS=org.red5.server.Bootstrap
fi

# Jython options
JYTHON="-Dpython.home=lib"

for JAVA in "${JAVA_HOME}/bin/java" "${JAVA_HOME}/Home/bin/java" "/usr/bin/java" "/usr/local/bin/java"
do
  if [ -x "$JAVA" ]
  then
    break
  fi
done

if [ ! -x "$JAVA" ]
then
  echo "Unable to locate Java. Please set JAVA_HOME environment variable."
  exit
fi

export RED5_CLASSPATH="${RED5_HOME}/red5-server-bootstrap.jar${P}${RED5_HOME}/conf${P}${CLASSPATH}"

# start Red5
echo "Starting Red5"
exec "$JAVA" "$JYTHON" $YOURKIT -Dred5.root="${RED5_HOME}" $JAVA_OPTS -cp "${RED5_CLASSPATH}" "$RED5_MAINCLASS" $RED5_OPTS

