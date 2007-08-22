package org.red5.server.jboss;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.beans.Introspector;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.red5.server.jmx.JMXAgent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Red5 loader for Jboss
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JbossLoader implements ApplicationContextAware, JbossLoaderMBean {

	/**
	 * We store the application context in a ThreadLocal so we can access it
	 * later
	 */
	protected static ThreadLocal<ApplicationContext> applicationContext = new ThreadLocal<ApplicationContext>();

	// Initialize Logging
	protected static Logger logger = Logger.getLogger(JbossLoader.class.getName());
	
	/**
	 * Initialization
	 */
	public void start() {
		logger.info("Loading jboss service");

		logger.info("RED5 Server (http://www.osflash.org/red5)");
		//logger.info("Loading red5 global context from: " + red5Config);

		long time = System.currentTimeMillis();		
		
		try {
	        // get Red5 root as environment variable, this is set in the META-INF/jboss-service.xml
			String root = System.getProperty("red5.root");
			logger.info("Red5 root: " + root);
			String configRoot = System.getProperty("red5.config_root");
			logger.info("Red5 config root: " + configRoot);					
			
			ConfigurableApplicationContext appCtx = new FileSystemXmlApplicationContext(configRoot + "applicationContext.xml");
			
			/*
			String[] defNames = appCtx.getBeanDefinitionNames();
			for (String nm : defNames) {
				logger.debug("Bean def: " + nm);
			}
			*/

			ConfigurableBeanFactory factory = appCtx.getBeanFactory();
			//register default add the context to the parent
			factory.registerSingleton("default.context", applicationContext);				
					
			this.setApplicationContext(appCtx);

		} catch (Exception e) {
			logger.error("Error during startup", e);
		}				

		long startupIn = System.currentTimeMillis() - time;
		logger.info("Startup done in: " + startupIn + " ms");		
		
	}

    public boolean isStarted() {
        return true;
    }

	/**
	 * Shut server down
	 */
	public void stop() {
		logger.info("Shutting down jboss context");
		try {
			//prepare spring for shutdown
			Introspector.flushCaches();
            //shutdown our jmx agent
    		JMXAgent.shutdown();
			//shutdown spring
    		FileSystemXmlApplicationContext appContext = (FileSystemXmlApplicationContext) getApplicationContext();
			ConfigurableBeanFactory factory = appContext.getBeanFactory();
			if (factory.containsSingleton("default.context")) {
				for (String scope : factory.getRegisteredScopeNames()) {
					logger.debug("Registered scope: " + scope);
				}
				for (String singleton : factory.getSingletonNames()) {
					logger.debug("Registered singleton: " + singleton);
					//factory.destroyScopedBean(singleton);
				}
				factory.destroySingletons();
			}
			appContext.close();
			LogFactory.release(Thread.currentThread().getContextClassLoader());			
		} catch (Exception e) {
			logger.warn("Jboss could not be stopped", e);
		}
	}

	public void setApplicationContext(ApplicationContext applicationCtx) throws BeansException {
		logger.debug("Attempt to set app context");
		applicationContext.set(applicationCtx);	
	}

	public ApplicationContext getApplicationContext() {
		logger.debug("Attempt to get app context");
		return applicationContext.get();
	}	
	
}
