package org.red5.logging;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * A class that allows the LoggerFactory to access an web context based LoggerContext.
 * 
 * Add this java option -Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class LoggingContextSelector implements ContextSelector {

	private static final ConcurrentMap<String, LoggerContext> contextMap = new ConcurrentHashMap<String, LoggerContext>();

	private final ThreadLocal<LoggerContext> threadLocal = new ThreadLocal<LoggerContext>();

	private final LoggerContext defaultContext;
	
	private String contextName;

	private String contextConfigFile;

	public LoggingContextSelector(LoggerContext context) {
		System.out.println("Setting default logging context: " + context.getName());
		defaultContext = context;
	}

	public LoggerContext getLoggerContext() {
		System.out.println("getLoggerContext request");		
		// First check if ThreadLocal has been set already
		LoggerContext lc = threadLocal.get();
		if (lc != null) {
			System.out.println("Thread local found: " + lc.getName());
			return lc;
		}

		if (contextName == null) {
			System.out.println("Context name was null, returning default");
			// We return the default context
			return defaultContext;
		} else {
			// Let's see if we already know such a context
			LoggerContext loggerContext = contextMap.get(contextName);
			System.out.println("Logger context for " + contextName + " is " + loggerContext);

			if (loggerContext == null) {
				// We have to create a new LoggerContext
				loggerContext = new LoggerContext();
				loggerContext.setName(contextName);

				if (contextConfigFile == null) {
					contextConfigFile = "logback-" + contextName + ".xml";
				}

				URL url = Loader.getResourceByTCL(contextConfigFile);
				if (url != null) {
					try {
						JoranConfigurator configurator = new JoranConfigurator();
						loggerContext.shutdownAndReset();
						configurator.setContext(loggerContext);
						configurator.doConfigure(url);
					} catch (JoranException e) {
						StatusPrinter.print(loggerContext);
					}
				} else {
					try {
						ContextInitializer.autoConfig(loggerContext);
					} catch (JoranException je) {
						StatusPrinter.print(loggerContext);
					}
				}

				System.out.println("Adding logger context: " + loggerContext.getName() + " to map for context: " + contextName);
				contextMap.put(contextName, loggerContext);
			}
			return loggerContext;
		}
	}

	public LoggerContext getLoggerContext(String name) {
		System.out.println("getLoggerContext request for " + name);
		System.out.println("Context is in map: " + contextMap.containsKey(name));
		return contextMap.get(name);
	}	
	
	public LoggerContext getDefaultLoggerContext() {
		return defaultContext;
	}

	public void attachLoggerContext(String contextName,
			LoggerContext loggerContext) {
		contextMap.put(contextName, loggerContext);
	}

	public LoggerContext detachLoggerContext(String loggerContextName) {
		return contextMap.remove(loggerContextName);
	}

	public List<String> getContextNames() {
		List<String> list = new ArrayList<String>();
		list.addAll(contextMap.keySet());
		return list;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	public void setContextConfigFile(String contextConfigFile) {
		this.contextConfigFile = contextConfigFile;
	}

	/**
	 * Returns the number of managed contexts Used for testing purposes
	 * 
	 * @return the number of managed contexts
	 */
	public int getCount() {
		return contextMap.size();
	}

	/**
	 * These methods are used by the LoggerContextFilter.
	 * 
	 * They provide a way to tell the selector which context to use, thus saving
	 * the cost of a JNDI call at each new request.
	 * 
	 * @param context
	 */
	public void setLocalContext(LoggerContext context) {
		threadLocal.set(context);
	}

	public void removeLocalContext() {
		threadLocal.remove();
	}

}
