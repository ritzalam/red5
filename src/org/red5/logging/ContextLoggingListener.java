package org.red5.logging;

import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.impl.StaticLoggerBinder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;

/**
 * A servlet context listener that puts this contexts LoggerContext 
 * into a static map of logger contexts within an overall singleton
 * log context selector.
 * 
 * To use it, add the following line to a web.xml file
 *<pre>
	&lt;listener&gt;
		&lt;listener-class&gt;org.red5.logging.ContextLoggingListener&lt;/listener-class&gt;
	&lt;/listener&gt;
 *</pre>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ContextLoggingListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent event) {
		System.out.println("Context destroying...");

		String contextName = pathToName(event);
		System.out.println("About to detach context named " + contextName);

		ContextSelector selector = StaticLoggerBinder.SINGLETON.getContextSelector();
		LoggerContext context = selector.detachLoggerContext(contextName);
		if (context != null) {
			Logger logger = context.getLogger(LoggerContext.ROOT_NAME);
			logger.info("Shutting down context {}", contextName);
			context.shutdownAndReset();
		} else {
			System.err.println("No context named " + contextName
					+ " was found.");
		}
	}

	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Context init...");

		String contextName = pathToName(event);
		System.out.println("Logger name for context: " + contextName);

		try {
			LoggingContextSelector selector = (LoggingContextSelector) StaticLoggerBinder.SINGLETON
					.getContextSelector();

			selector.setContextName(contextName);
			LoggerContext context = selector.getLoggerContext();

			if (context != null) {
				Logger logger = context.getLogger(LoggerContext.ROOT_NAME);
				logger.info("Starting up context {}", contextName);
			} else {
				System.err.println("No context named " + contextName
						+ " was found.");
			}
			
			//List<String> ctxNameList = selector.getContextNames();
			//for (String s : ctxNameList) {
			//	System.out.println("Selector context name: " + s);
			//}			
			
		} catch (Exception e) {
			System.err.println("LoggingContextSelector is not the correct type");
		}

	}

	private String pathToName(ServletContextEvent event) {
		String contextName = event.getServletContext().getContextPath()
				.replaceAll("/", "");
		if (contextName.equals("")) {
			contextName = "root";
		}
		return contextName;
	}

}
