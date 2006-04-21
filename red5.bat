@echo off

if not %JAVA_HOME% == "" goto launchRed5

:launchRed5
%JAVA_HOME%/bin/java -cp red5.jar;conf;%CLASSPATH% org.red5.server.Standalone
goto finaly

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.
pause

:finaly
pause
