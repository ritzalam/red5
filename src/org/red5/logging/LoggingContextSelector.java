package org.red5.logging;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

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
		System.out.printf("Setting default logging context: %s\n", context.getName());
		defaultContext = context;
	}

	public LoggerContext getLoggerContext() {
		System.out.println("getLoggerContext request");		
		// First check if ThreadLocal has been set already
		LoggerContext lc = threadLocal.get();
		if (lc != null) {
			System.out.printf("Thread local found: %s\n", lc.getName());
			return lc;
		}

		if (contextName == null) {
			System.out.println("Context name was null, returning default");
			// We return the default context
			return defaultContext;
		} else {
			// Let's see if we already know such a context
			LoggerContext loggerContext = contextMap.get(contextName);
			System.out.printf("Logger context for %s is %s\n", contextName, loggerContext);

			if (loggerContext == null) {
				// We have to create a new LoggerContext
				loggerContext = new LoggerContext();
				loggerContext.setName(contextName);

				//if (contextConfigFile == null) {
					contextConfigFile = String.format("logback-%s.xml", contextName);
					System.out.printf("Context logger config file: %s\n", contextConfigFile);
				//}
				
				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
				System.out.printf("Thread context cl: %s\n", classloader);
				ClassLoader classloader2 = Loader.class.getClassLoader();
				System.out.printf("Loader tcl: %s\n", classloader2);
				
				//URL url = Loader.getResourceBySelfClassLoader(contextConfigFile);
				URL url = Loader.getResource(contextConfigFile, classloader);
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
						ContextInitializer ctxInit = new ContextInitializer(loggerContext);
						ctxInit.autoConfig();
					} catch (JoranException je) {
						StatusPrinter.print(loggerContext);
					}
				}

				System.out.printf("Adding logger context: %s to map for context: %s\n", loggerContext.getName(), contextName);
				contextMap.put(contextName, loggerContext);
			}
			return loggerContext;
		}
	}

	public LoggerContext getLoggerContext(String name) {
		System.out.printf("getLoggerContext request for %s\n", name);
		System.out.printf("Context is in map: %s\n", contextMap.containsKey(name));
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
