@echo off

if not "%JAVA_HOME%" == "" goto launchRed5

:launchRed5
"%JAVA_HOME%/bin/java" -Djava.security.manager -Djava.security.policy=conf/red5.policy -Xrs -Xms512M -Xmx768M -Xss128K -XX:PermSize=256M -XX:MaxPermSize=512M -XX:NewRatio=2 -XX:MinHeapFreeRatio=20 -XX:+AggressiveHeap -XX:+DisableExplicitGC -XX:ParallelGCThreads=2 -XX:+UseParallelOldGC -XX:+MaxFDLimit -Dsun.rmi.dgc.client.gcInterval=990000 -Dsun.rmi.dgc.server.gcInterval=990000 -Djava.net.preferIPv4Stack=true -Xverify:none -cp red5.jar;conf;bin org.red5.server.Standalone
goto finaly

:err
echo JAVA_HOME environment variable not set! Take a look at the readme.
pause

:finaly
pause
