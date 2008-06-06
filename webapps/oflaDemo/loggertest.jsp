<%@page import="org.slf4j.Logger,org.slf4j.LoggerFactory,org.slf4j.impl.StaticLoggerBinder,ch.qos.logback.classic.LoggerContext,org.red5.logging.LoggingContextSelector"%>
<html>
<body>
<%
Logger log = LoggerFactory.getLogger("TestJsp");
log.info("This is a test log entry from a web context");

//
LoggingContextSelector selector = (LoggingContextSelector) StaticLoggerBinder.SINGLETON.getContextSelector();		
LoggerContext ctx = selector.getLoggerContext("oflaDemo");
Logger log2 = ctx.getLogger("TestJsp");
log2.info("This is a test log entry from a web context attempt 2");


for (int i = 0; i < 10; i++) {
    out.print(i);
    out.print("<br />");
}
%>
</body>
</html>

