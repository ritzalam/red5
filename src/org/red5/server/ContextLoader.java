package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.red5.server.jmx.mxbeans.ContextLoaderMXBean;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Red5 applications loader
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ContextLoader implements ApplicationContextAware, ContextLoaderMXBean {

	protected static Logger log = LoggerFactory.getLogger(ContextLoader.class);

	/**
	 * Spring Application context
	 */
	protected ApplicationContext applicationContext;

	/**
	 * Spring parent app context
	 */
	protected ApplicationContext parentContext;

	/**
	 * Context location files
	 */
	protected String contextsConfig;
	
	/**
	 * MBean object name used for de/registration purposes.
	 */
	private ObjectName oName;	

	/**
	 * Context map
	 */
	protected ConcurrentMap<String, ApplicationContext> contextMap = new ConcurrentHashMap<String, ApplicationContext>();

	/**
	 * Whether or not a JVM shutdown hook should be added to 
	 * the Spring application context.
	 */
	private boolean useShutdownHook;
	
	/**
	 * @param applicationContext Spring application context
	 * @throws BeansException Top level exception for app context (that is, in fact, beans
	 *             factory)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;	
	}

	/**
	 * Setter for parent application context
	 * 
	 * @param parentContext Parent Spring application context
	 */
	public void setParentContext(ApplicationContext parentContext) {
		this.parentContext = parentContext;
	}

	/**
	 * Setter for context config name
	 * 
	 * @param contextsConfig Context config name
	 */
	public void setContextsConfig(String contextsConfig) {
		this.contextsConfig = contextsConfig;
	}

	/**
	 * Whether or not the shutdown hook is enabled.
	 * 
	 * @return true if enabled, false otherwise
	 */
	public boolean isUseShutdownHook() {
		return useShutdownHook;
	}

	/**
	 * Enables or disables the shutdown hook.
	 * 
	 * @param useShutdownHook true to enable, false to disable
	 */
	public void setUseShutdownHook(boolean useShutdownHook) {
		this.useShutdownHook = useShutdownHook;
	}
	
	/**
	 * Loads context settings from ResourceBundle (.properties file)
	 * 
	 * @throws Exception I/O exception, casting exception and others
	 */
	public void init() throws Exception {
		log.debug("ContextLoader init");
		// register in jmx
		//create a new mbean for this instance
		oName = JMXFactory.createObjectName("type", "ContextLoader");
		JMXAgent.registerMBean(this, this.getClass().getName(),	ContextLoaderMXBean.class, oName);		
		
		//check to see if we should add a shutdown hook
		if (useShutdownHook) {
    		//register a jvm shutdown hook
    		((AbstractApplicationContext) applicationContext).registerShutdownHook();	
		}
		
		// Load properties bundle
		Properties props = new Properties();
		Resource res = applicationContext.getResource(contextsConfig);
		if (!res.exists()) {
			log.error("Contexts config must be set.");
			return;
		}

		// Load properties file
		props.load(res.getInputStream());
		
		// Pattern for arbitrary property substitution
		Pattern patt = Pattern.compile("\\$\\{([^\\}]+)\\}");		
		Matcher matcher = null;
		
		// Iterate thru properties keys and replace config attributes with
		// system attributes
		for (Object key : props.keySet()) {
			String name = (String) key;
			String config = props.getProperty(name);
			String configReplaced = config + "";
			//
			matcher = patt.matcher(config);
			//execute the regex
			while (matcher.find()) {
				String sysProp = matcher.group(1);				
				String systemPropValue = System.getProperty(sysProp);				
				if (systemPropValue == null) {
					systemPropValue = "null";
				}
				configReplaced = configReplaced.replace(String.format("${%s}", sysProp), systemPropValue);
			}
			log.info("Loading: {} = {}", name, config + " => " + configReplaced);

			matcher.reset();
			
			// Load context
			loadContext(name, configReplaced);
		}
		
		patt = null;
		matcher = null;
	}
	
	/**
	 * Un-loads or un-initializes the contexts; this is a shutdown method for this loader.
	 */
	public void uninit() {
		log.debug("ContextLoader un-init");		
		JMXAgent.unregisterMBean(oName);
		//shutdown the plug-in launcher here
		try {
			PluginRegistry.shutdown();
		} catch (Exception e) {
			log.warn("Exception shutting down plugin registry", e);
		}
		//unload all the contexts in the map
		for (Map.Entry<String, ApplicationContext> entry : contextMap.entrySet()) {
			unloadContext(entry.getKey());
		}
	}

	/**
	 * Loads a context (Red5 application) and stores it in a context map, then adds
	 * it's beans to parent (that is, Red5)
	 * 
	 * @param name Context name
	 * @param config Filename
	 */
	public void loadContext(String name, String config) {
		log.debug("Load context - name: {} config: {}", name, config);
		//check the existence of the config file
		try {
			File configFile = new File(config);
			if (!configFile.exists()) {
				log.warn("Config file was not found at: {}", configFile.getCanonicalPath());
				configFile = new File("file://" + config);
				if (!configFile.exists()) {
					log.warn("Config file was not found at either: {}", configFile.getCanonicalPath());
				} else {
					config = "file://" + config;
				}
			}
		} catch (IOException e) {
			log.error("Error looking for config file", e);
		}
		// add the context to the parent, this will be red5.xml
		ConfigurableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		if (factory.containsSingleton(name)) {
			log.warn("Singleton {} already exists, try unload first", name);		
			return;
		}
		// if parent context was not set then lookup red5.common
		if (parentContext == null) {
			log.debug("Lookup common - bean:{} local:{} singleton:{}", new Object[]{
					factory.containsBean("red5.common"),
					factory.containsLocalBean("red5.common"),
					factory.containsSingleton("red5.common"),
			});
			parentContext = (ApplicationContext) factory.getBean("red5.common");
		}
		if (config.startsWith("/")) {
			// Spring always interprets files as relative, so
			// will strip a leading slash unless we tell
			// it otherwise.
			// It also appears to not need this for Windows
			// absolute paths (e.g. C:\Foo\Bar) so we
			// don't catch that either
			String newConfig = "file://" + config;
			log.debug("Resetting {} to {}", config, newConfig);
			config = newConfig;
		}
		ApplicationContext context = new FileSystemXmlApplicationContext(
				new String[] { config }, parentContext);
		log.debug("Adding to context map - name: {} context: {}", name, context);
		contextMap.put(name, context);
		// Register context in parent bean factory
		log.debug("Registering - name: {}", name);
		factory.registerSingleton(name, context);
	}

	/**
	 * Unloads a context (Red5 application) and removes it from the context map, then removes
	 * it's beans from the parent (that is, Red5)
	 * 
	 * @param name Context name
	 */	
	public void unloadContext(String name) {
		log.debug("Un-load context - name: {}", name);
		ApplicationContext context = contextMap.remove(name);
		log.debug("Context from map: {}", context);
		ConfigurableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		if (factory.containsSingleton(name)) {
			log.debug("Context found in parent, destroying: {}", name);
			FileSystemXmlApplicationContext ctx = (FileSystemXmlApplicationContext) factory.getSingleton(name);
			if (ctx.isRunning()) {
				log.debug("Context was running, attempting to stop");
				ctx.stop();
			}
			if (ctx.isActive()) {
				log.debug("Context is active, attempting to close");
				ctx.close();
			}
			try {
				factory.destroyBean(name, ctx);
			} catch (Exception e) {
				log.warn("Context destroy failed for: {}", name, e);
			} finally {
				if (factory.containsSingleton(name)) {
					log.debug("Singleton still exists, trying another destroy method");
					((DefaultListableBeanFactory) factory).destroySingleton(name);
				}
			}
		}
		context = null;
	}
	
	/**
	 * Return context by name
	 * 
	 * @param name Context name
	 * @return Application context for given name
	 */
	public ApplicationContext getContext(String name) {
		return contextMap.get(name);
	}

	/**
	 * Return parent context
	 * 
	 * @return parent application context
	 */	
	public ApplicationContext getParentContext() {
		return parentContext;
	}

	public String getContextsConfig() {
		return contextsConfig;
	}
}
