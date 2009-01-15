#!/bin/bash

if [ -z "$RED5_HOME" ]; then export RED5_HOME=.; fi

P=":" # The default classpath separator
OS=`uname`
case "$OS" in
  CYGWIN*|MINGW*) # Windows Cygwin or Windows MinGW
  P=";" # Since these are actually Windows, let Java know
  ;;
  *)
  # Do nothing
  ;;
esac

# JAVA options
# You can set JAVA_OPTS to add additional options if you want
# Set up logging options
LOGGING_OPTS="-Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true"
# Set up security options
SECURITY_OPTS="-Djava.security.debug=failure"
export JAVA_OPTS="-Dred5.root=$RED5_HOME -Djava.security.manager -Djava.security.policy=$RED5_HOME/conf/red5.policy $LOGGING_OPTS $SECURITY_OPTS $JAVA_OPTS"

# Jython options
JYTHON="-Dpython.home=lib"

for JAVA in "$JAVA_HOME/bin/java" "/usr/bin/java" "/usr/local/bin/java"
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

export RED5_CLASSPATH="$RED5_HOME/red5.jar$P$RED5_HOME/conf$Plib/ejb3-persistence.jar$P$CLASSPATH"
if [ -z "$RED5_MAINCLASS" ]; then
  export RED5_MAINCLASS=org.red5.server.Bootstrap
fi

# start Red5
echo "Starting Red5 ($RED5_MAINCLASS)..."
exec "$JAVA" $JYTHON $JAVA_OPTS -cp $RED5_CLASSPATH $RED5_MAINCLASS $RED5_OPTS
# 1> $RED5_HOME/log/stdout.log  2>$RED5_HOME/log/stderr.log