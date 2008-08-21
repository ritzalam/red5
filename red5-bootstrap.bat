@echo off

if NOT DEFINED RED5_HOME set RED5_HOME=.

set LOGGING_OPTS= -Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true

set SECURITY_OPTS= -Djava.security.debug=failure

rem Note: This used to have all the same options as red5-highperf.bat but
rem at least on Win32 the JVM will not start (1.6)
set JAVA_OPTS= %LOGGING_OPTS% %SECURITY_OPTS%
set RED5_MAINCLASS=org.red5.server.Bootstrap
%RED5_HOME%\red5.bat 1>%RED5_HOME%\log\stdout.log 2>%RED5_HOME%\log\stderr.log
