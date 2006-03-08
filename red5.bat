@echo off

if not %JAVA_HOME% == "" goto launchRed5

:launchRed5
%JAVA_HOME%/bin/java -jar red5.jar
goto finaly

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.
pause

:finaly
pause
