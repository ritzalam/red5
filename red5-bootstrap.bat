@echo off

set JAVA_OPTS = -Xmx768m -Xms256 -Xmn512m -Xss128k -XX:+AggressiveOpts -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000

if not "%JAVA_HOME%" == "" goto launchRed5

:launchRed5
"%JAVA_HOME%/bin/java" %JAVA_OPTS% -Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector -Dcatalina.useNaming=true -Djava.security.manager -Djava.security.debug=failure -Djava.security.policy=conf/red5.policy -cp red5.jar org.red5.server.Bootstrap 1>log\boot.log 2>log\error.log
goto finally

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.
pause

:finally
REM pause