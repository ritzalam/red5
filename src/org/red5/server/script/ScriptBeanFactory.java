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
package org.red5.server.script;

import javax.script.ScriptEngineFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;

/**
 * @author Luke Hubbard <luke@codegent.com>
 * @author Paul Gregoire <mondain@gmail.com>
 */
public class ScriptBeanFactory extends DefaultListableBeanFactory implements
		ApplicationContextAware {

	private static final Log log = LogFactory.getLog(ScriptBeanFactory.class);

	protected String path = "WEB-INF/applications/";

	protected boolean lazyLoading = false;

	protected boolean productionMode = false;

	protected ApplicationContext appCtx;

	protected ScriptEngineFactory factory;

	public ScriptBeanFactory() {
		super();
	}

	// Public setters
	public void setApplicationContext(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isLazyLoading() {
		return lazyLoading;
	}

	public void setLazyLoading(boolean lazyLoading) {
		this.lazyLoading = lazyLoading;
	}

	public boolean isProductionMode() {
		return productionMode;
	}

	public void setProductionMode(boolean productionMode) {
		this.productionMode = productionMode;
	}

	// Init method

	public void startup() throws Exception {
		log.debug("Starting up...");
		if (!isLazyLoading()) {
			initScripts();
		}
	}

	// Setup methods
	protected void initScripts() throws Exception {
		log.debug("Initializing scripts...");
		String[] exts = factory.getExtensions();
		for (String ext : exts) {
			Resource[] res = appCtx.getResources(path + ext);
			if (res == null || res.length == 0) {
				log.info("No scripts found in location: " + path + ext);
			} else {
				for (Resource resource : res) {
					String name = resource.getFilename();
					Object bean = null;
					log.info("Loading script for first time: " + name);
					try {
						bean = getBean(name);
					} catch (Exception ex) {
						log.error("Error creating script: " + name, ex);
					}
					if (bean == null) {
						log.error("Script bean is null: " + name);
					}
				}
			}
		}
	}

	protected ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	// Bean registration methods

	protected void registerScriptBeanDefinition(String beanName) {
		try {
			// Check the file is there, it will throw an exception if its not
			Resource resource = appCtx.getResource(path + beanName);
			resource.getFile().lastModified();
			// Continue to load bean definition
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, path + beanName);
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils
					.createBeanDefinition(null, null, cargs, null, this
							.getClassLoader());
			bd.setFactoryBeanName(factory.getLanguageName() + "Factory");
			bd.setFactoryMethodName("create");
			bd.setSingleton(true);
			registerBeanDefinition(beanName, bd);
		} catch (Exception ex) {
			registerErrorBeanDefinition(beanName, ex);
		}
	}

	protected void registerErrorBeanDefinition(String beanName, Exception ex) {
		try {
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, ex);
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils
					.createBeanDefinition(ScriptErrorBean.class.getName(),
							null, cargs, null, this.getClassLoader());
			bd.setSingleton(true);
			registerBeanDefinition(beanName, bd);
		} catch (ClassNotFoundException ex2) {
			log.error("Class not found exception while creating error bean",
					ex2);
		}
	}

	// Public beanFactory methods

	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(name, null, null);
	}

	@Override
	public Object getBean(String name, Class requiredType)
			throws BeansException {
		return getBean(name, requiredType, null);
	}

	/**
	 * Return the bean with the given name, checking the parent bean factory if
	 * not found.
	 * 
	 * @param name
	 *            the name of the bean to retrieve
	 * @param args
	 *            arguments to use if creating a prototype using explicit
	 *            arguments to a static factory method. It is invalid to use a
	 *            non-null args value in any other case.
	 */
	@Override
	public Object getBean(String name, Object[] args) throws BeansException {
		return getBean(name, null, args);
	}

	@Override
	public Object getBean(String name, Class requiredType, Object[] args)
			throws BeansException {
		log.debug("getBean - name: " + name + " type: "
				+ requiredType.getName());
		// setupScriptScope();
		Object bean = null;
		if (containsBean(name)) {
			bean = super.getBean(name, requiredType, args);
			// if production mode, dont check for reload
			if (productionMode) {
				return bean;
			}
			// otherwise check the script object
			ScriptSource script = null;
			if (factory != null) {
				// script = factory.lookupScript(bean);
				// script = factory.getScriptEngine().get(bean);
			}
			if (script != null && script.isModified()) {
				registerScriptBeanDefinition(name);
			} else {
				return bean;
			}
		} else {
			registerScriptBeanDefinition(name);
		}
		return super.getBean(name, requiredType, args);
	}

	public ScriptEngineFactory getFactory() {
		return factory;
	}

	public void setFactory(ScriptEngineFactory factory) {
		this.factory = factory;
	}

}
