@echo off

REM Previous option set
REM -Xrs -Xms512M -Xmx768M -Xss128K -XX:PermSize=256M -XX:MaxPermSize=512M -XX:NewRatio=2 -XX:MinHeapFreeRatio=20 -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=2 -XX:+UseParallelOldGC -XX:+MaxFDLimit -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000 -Djava.net.preferIPv4Stack=true -Xverify:none

REM Latest 06/2008
REM -Xmx768m -Xms256 -Xmn512m -Xss128k -XX:+AggressiveOpts -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000

set JAVA_OPTS = -Xmx768m -Xms256 -Xmn512m -Xss128k -XX:+AggressiveOpts -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -Djava.net.preferIPv4Stack=true -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000

if not "%JAVA_HOME%" == "" goto launchRed5

:launchRed5
"%JAVA_HOME%/bin/java" %JAVA_OPTS% -Djava.security.manager -Djava.security.policy=conf/red5.policy -cp red5.jar;conf;bin org.red5.server.Standalone
goto finally

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.

:finally
pause
