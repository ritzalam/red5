@echo off

if not defined JAVA_HOME goto err

%JAVA_HOME%/bin/java -jar red5.jar

:err

echo JAVA_HOME environment variable not set! Take a look at the readme.

pause