#!/bin/bash

if [ -z "$RED5_HOME" ]; then export RED5_HOME=.;  fi

LOGGING_OPTS="-Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true"
SECURITY_OPTS="-Djava.security.debug=failure"
# Add them to the JAVA_OPTS that red5 will use
export JAVA_OPTS="$LOGGING_OPTS $SECURITY_OPTS"

export RED5_MAINCLASS=org.red5.server.Bootstrap

# start Red5
echo "Starting Red5 ($RED5_MAINCLASS)..."
exec $RED5_HOME/red5.sh 1> $RED5_HOME/log/stdout.log  2>$RED5_HOME/log/stderr.log
