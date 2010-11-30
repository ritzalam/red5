package org.red5.logging;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;

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
		//System.out.printf("About to detach context named %s\n", contextName);
		ContextSelector selector = Red5LoggerFactory.getContextSelector();
		LoggerContext context = selector.detachLoggerContext(contextName);
		if (context != null) {
			Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
			logger.debug("Shutting down context {}", contextName);
			context.reset();
		} else {
			System.err.printf("No context named %s was found", contextName);
		}
	}

	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Context init...");
		String contextName = pathToName(event);
		System.out.printf("Logger name for context: %s\n", contextName);
		LoggingContextSelector selector = null;
		try {
			selector = (LoggingContextSelector) Red5LoggerFactory.getContextSelector();
			//set this contexts name
			selector.setContextName(contextName);
			LoggerContext context = selector.getLoggerContext();
			if (context != null) {
				Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
				logger.debug("Starting up context {}", contextName);
			} else {
				System.err.printf("No context named %s was found", contextName);
			}
		} catch (Exception e) {
			System.err.printf("LoggingContextSelector is not the correct type", e);
		} finally {
			//reset the name
			if (selector != null) {
				selector.setContextName(null);
			}
		}
	}

	private String pathToName(ServletContextEvent event) {
		String contextName = event.getServletContext().getContextPath().replaceAll("/", "");
		if ("".equals(contextName)) {
			contextName = "root";
		}
		return contextName;
	}

}
