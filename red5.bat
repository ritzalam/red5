@echo off

if NOT DEFINED RED5_HOME set RED5_HOME=.

if NOT DEFINED JAVA_HOME goto err

if NOT DEFINED JAVA_OPTS set JAVA_OPTS=
set JAVA_OPTS=-Dred5.root=%RED5_HOME% -Djava.security.manager -Djava.security.policy=%RED5_HOME%\conf\red5.policy %JAVA_OPTS%

set JYTHON_OPTS=-Dpython.home=lib

set RED5_CLASSPATH=%RED5_HOME%\red5.jar;%RED5_HOME%\conf;%CLASSPATH%
if NOT DEFINED RED5_MAINCLASS set RED5_MAINCLASS=org.red5.server.Standalone
if NOT DEFINED RED5_OPTS set RED5_OPTS= 

goto launchRed5

:launchRed5
echo Starting Red5 (%RED5_MAINCLASS%)
"%JAVA_HOME%/bin/java" %JYTHON_OPTS% %JAVA_OPTS% -cp %RED5_CLASSPATH% %RED5_MAINCLASS% %RED5_OPTS%
goto finally

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.
pause

:finally
