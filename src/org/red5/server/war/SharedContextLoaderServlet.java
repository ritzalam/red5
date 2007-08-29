package org.red5.server.war;

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
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.GlobalScope;
import org.red5.server.ScopeResolver;
import org.red5.server.Server;
import org.red5.server.WebScope;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.service.ServiceInvoker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * Entry point from which the server config file is loaded while running within
 * a J2EE application container.
 * 
 * This listener should be registered after Log4jConfigListener in web.xml, if
 * the latter is used.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SharedContextLoaderServlet extends ContextLoaderListener {

	private final static long serialVersionUID = 41919712007L;

	// Initialize Logging
	public static Logger logger = Logger
			.getLogger(SharedContextLoaderServlet.class.getName());

	private static ServletContext servletContext;

	{
		initRegistry();
	}

	/**
	 * Main entry point for the Red5 Server as a war
	 */
	// Notification that the web application is ready to process requests
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.setProperty("red5.deployment.type", "war");

		if (null != servletContext) {
			return;
		}

		servletContext = sce.getServletContext();
		String prefix = servletContext.getRealPath("/");

		long time = System.currentTimeMillis();

		logger.info("!!! DEBUG - VERSION - NOT MEANT FOR NON-DEV USE !!!");
		logger.info("RED5 Server (http://www.osflash.org/red5)");
		logger.info("Path: " + prefix);

		try {
			String[] strArray = servletContext.getInitParameter(
					ContextLoader.CONFIG_LOCATION_PARAM).split("[,\\s]");
			logger.info("Config location files: " + strArray.length);
			ConfigurableBeanFactory factory = null;
			IRemotableList remote = null;
			WebSettings settings = null;
			if (prefix.toUpperCase().indexOf("ROOT") == -1) {
				logger.info("******************************\nWe are a child");
				settings = new WebSettings();
				settings.setPath(prefix);
				logger.debug("Prefix: " + prefix);
				settings.setConfigs(strArray);
				settings.setWebAppKey(servletContext
						.getInitParameter("webAppRootKey"));
				// register child webscope since its not root
				// factory = applicationContext.getBeanFactory();
				//
				boolean firstReg = false;
				try {
					remote = (IRemotableList) Naming
							.lookup("rmi://localhost:1099/scopeList");
				} catch (Exception e) {
					logger.warn("Lookup failed: " + e.getMessage());
				}
				if (remote == null) {
					remote = new RemotableList();
					firstReg = true;
				}
				logger.debug("Adding child web settings");
				remote.addChild(settings);
				logger.debug("Remote list size: " + remote.numChildren());
				if (firstReg) {
					Naming.bind("rmi://localhost:1099/scopeList", remote);
				} else {
					Naming.rebind("rmi://localhost:1099/scopeList", remote);
				}
			} else {
				logger.info("******************************\nWe are ROOT");
				Class contextClass = org.springframework.web.context.support.XmlWebApplicationContext.class;
				ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext) BeanUtils
						.instantiateClass(contextClass);
				applicationContext.setConfigLocations(strArray);
				applicationContext.setServletContext(servletContext);
				applicationContext.refresh();
				// set web application context as an attribute of the servlet
				// context so that it may be located via Springs
				// WebApplicationContextUtils
				servletContext
						.setAttribute(
								WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
								applicationContext);

				factory = applicationContext.getBeanFactory();
				//
				FileSystemXmlApplicationContext common = new FileSystemXmlApplicationContext(
						prefix + "/WEB-INF/classes/red5-common.xml");
				common.setDisplayName("red5.common");

				FileSystemXmlApplicationContext core = new FileSystemXmlApplicationContext(
						new String[] { prefix
								+ "/WEB-INF/classes/red5-core.xml" }, common);
				core.setDisplayName("red5.core");

				factory.registerSingleton("red5.common", common);
				factory.registerSingleton("red5.core", core);

				// register default
				factory
						.registerSingleton("default.context",
								applicationContext);

				// for (String beanName : applicationContext
				// .getBeanDefinitionNames()) {
				// logger.info("Bean: " + beanName);
				// }

				Server server = (Server) common.getBean("red5.server");
				logger.debug("Server: " + server);

				ClientRegistry clientRegistry = (ClientRegistry) factory
						.getBean("global.clientRegistry");

				ServiceInvoker globalInvoker = (ServiceInvoker) factory
						.getBean("global.serviceInvoker");

				Context globalContext = (Context) factory
						.getBean("global.context");
				globalContext.setCoreBeanFactory(core.getBeanFactory());

				GlobalScope global = (GlobalScope) factory
						.getBean("global.scope");
				logger.debug("GlobalScope: " + global);

				ScopeResolver globalResolver = new ScopeResolver();
				globalResolver.setGlobalScope(global);

				global.setServer(server);
				global.register();
				global.start();

				Context webContext = (Context) factory.getBean("web.context");
				webContext.setCoreBeanFactory(core.getBeanFactory());
				webContext.setClientRegistry(clientRegistry);
				webContext.setServiceInvoker(globalInvoker);
				webContext.setScopeResolver(globalResolver);

				WebScope scope = (WebScope) factory.getBean("web.scope");
				scope.setServer(server);
				scope.setParent(global);
				scope.register();
				scope.start();

				// remote = (IRemotableList) factory.getBean("scopeList");
				remote = (IRemotableList) Naming
						.lookup("rmi://localhost:1099/scopeList");
				logger.debug("Children: " + remote.numChildren());
				if (remote.hasChildren()) {
					logger.debug("Children were detected");
					for (int i = 0; i < remote.numChildren(); i++) {
						logger.debug("Enumerating children");
						settings = remote.getAt(i);
						ConfigurableWebApplicationContext appCtx = (ConfigurableWebApplicationContext) BeanUtils
								.instantiateClass(contextClass);
						appCtx.setParent(applicationContext);
						String[] inConfigs = settings.getConfigs();
						String[] subConfigs = new String[inConfigs.length];
						for (int s = 0; s < inConfigs.length; s++) {
							String cfg = "file:/" + settings.getPath()
									+ inConfigs[s];
							logger.debug("Sub config location: " + cfg);
							subConfigs[s] = cfg;
						}
						appCtx.setConfigLocations(subConfigs);
						ServletContext ctx = servletContext.getContext(settings
								.getWebAppKey());
						logger
								.debug("Servlet context: "
										+ ctx.getContextPath());
						appCtx.setServletContext(ctx);
						appCtx.refresh();

						for (String beanName : appCtx.getBeanDefinitionNames()) {
							logger.info("Sub-Bean: " + beanName);
						}

						ConfigurableBeanFactory appFactory = appCtx
								.getBeanFactory();

						webContext = (Context) appCtx.getBean("web.context");
						webContext.setCoreBeanFactory(core.getBeanFactory());
						webContext.setClientRegistry(clientRegistry);
						webContext.setServiceInvoker(globalInvoker);
						webContext.setScopeResolver(globalResolver);

						scope = (WebScope) appFactory.getBean("web.scope");
						scope.setServer(server);
						scope.setParent(global);

						scope.register();
						scope.start();

					}
					logger.debug("End of children...");
				}
			}

		} catch (Throwable e) {
			logger.error(e);
		}

		long startupIn = System.currentTimeMillis() - time;
		logger.info("Startup done in: " + startupIn + " ms");

	}

	/**
	 * Clearing the in-memory configuration parameters, we will receive
	 * notification that the servlet context is about to be shut down
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("Webapp shutdown");
		// XXX Paul: grabbed this from
		// http://opensource.atlassian.com/confluence/spring/display/DISC/Memory+leak+-+classloader+won%27t+let+go
		// in hopes that we can clear all the issues with J2EE containers during
		// shutdown
		try {
			// prepare spring for shutdown
			Introspector.flushCaches();
			// dereg any drivers
			for (Enumeration e = DriverManager.getDrivers(); e
					.hasMoreElements();) {
				Driver driver = (Driver) e.nextElement();
				if (driver.getClass().getClassLoader() == getClass()
						.getClassLoader()) {
					DriverManager.deregisterDriver(driver);
				}
			}
			// shutdown jmx
			JMXAgent.shutdown();
			// shutdown spring
			// get web application context from the servlet context
			ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			ConfigurableBeanFactory factory = applicationContext
					.getBeanFactory();
			if (factory.containsSingleton("default.context")) {
				for (String scope : factory.getRegisteredScopeNames()) {
					logger.debug("Registered scope: " + scope);
				}
				for (String singleton : factory.getSingletonNames()) {
					logger.debug("Registered singleton: " + singleton);
					// factory.destroyScopedBean(singleton);
				}
				factory.destroySingletons();
			}
			servletContext
					.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			applicationContext.close();
			// http://jakarta.apache.org/commons/logging/guide.html#Classloader_and_Memory_Management
			// http://wiki.apache.org/jakarta-commons/Logging/UndeployMemoryLeak?action=print
			LogFactory.release(Thread.currentThread().getContextClassLoader());
		} catch (Throwable e) {
			// may get a java.lang.StackOverflowError when shutting appcontext
			// down in jboss
			e.printStackTrace();
		}
	}

	private void initRegistry() {
		Registry r = null;
		try {
			// lookup the registry
			r = LocateRegistry.getRegistry(1099);
			// ensure we are not already registered with the registry
			for (String regName : r.list()) {
				logger.debug("Registry entry: " + regName);
			}
		} catch (RemoteException re) {
			logger.info("RMI Registry server was not found on port 1099");
			// if we didnt find the registry and the user wants it created
			try {
				logger.info("Starting an internal RMI registry");
				// create registry for rmi port 9999
				r = LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				logger.info("RMI Registry server was not started on port 1099");
			}

		}
	}

}
