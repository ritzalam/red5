@echo off

SETLOCAL

if NOT DEFINED RED5_HOME for /F %%o IN ('cd') do set RED5_HOME=%%o

set JAVA_OPTS=-Djavax.net.ssl.keyStore=%RED5_HOME%conf\keystore.jmx -Djavax.net.ssl.keyStorePassword=password
set RED5_CLASSPATH=%RED5_HOME%red5.jar;%RED5_HOME%conf
set RED5_MAINCLASS=org.red5.server.Shutdown
set RED5_OPTS=9999 red5user changeme

"%JAVA_HOME%\bin\java" %JAVA_OPTS% -cp %RED5_CLASSPATH% %RED5_MAINCLASS% %RED5_OPTS%

ENDLOCAL