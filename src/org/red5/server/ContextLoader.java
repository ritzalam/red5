package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

public class ContextLoader implements ApplicationContextAware {

	protected static Log log =
        LogFactory.getLog(ContextLoader.class.getName());
	
	protected ApplicationContext applicationContext;
	protected ApplicationContext parentContext;
	protected String contextsConfig;
	protected HashMap<String, ApplicationContext> contextMap = new HashMap<String,ApplicationContext>();
	
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;		
	}
	
	public void setParentContext(ApplicationContext parentContext) {
		this.parentContext = parentContext;
	}

	public void setContextsConfig(String contextsConfig) {
		this.contextsConfig = contextsConfig;
	}

	public void init() throws Exception {
		Properties props = new Properties();
		Resource res = applicationContext.getResource(contextsConfig);
		if(!res.exists()){
			log.error("Contexts config must be set.");
			return;
		}
    	
		props.load(res.getInputStream());
    	
    	for(Object key : props.keySet()){
    		String name = (String) key;
    		String config = props.getProperty(name);
    		// TODO: we should support arbitrary property substitution
			config = config.replace("${red5.root}", System
					.getProperty("red5.root"));
			config = config.replace("${red5.config_root}", System
					.getProperty("red5.config_root"));
    		log.info("Loading: "+name+" = "+config);
    		loadContext(name, config);
    	}
    	
	}
	
	protected void loadContext(String name, String config){
		ApplicationContext context = new FileSystemXmlApplicationContext(
				new String[] { config }, parentContext);
		contextMap.put(name, context);
		// add the context to the parent, this will be red5.xml
		ConfigurableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext)
				.getBeanFactory();
		factory.registerSingleton(name,context);
	}
	
	public ApplicationContext getContext(String name) {
		return contextMap.get(name);
	}
}