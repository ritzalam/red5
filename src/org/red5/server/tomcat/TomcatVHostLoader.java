package org.red5.server.tomcat;

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

import java.io.File;
import java.util.Map;

import javax.management.ObjectName;
import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.loader.WebappLoader;
import org.red5.server.LoaderBase;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Red5 loader for Tomcat virtual hosts.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class TomcatVHostLoader extends TomcatLoader implements TomcatVHostLoaderMBean {

	// Initialize Logging
	private static Logger log = LoggerFactory.getLogger(TomcatVHostLoader.class);

	/**
	 * Base web applications directory
	 */
	protected String webappRoot;
	
	//the virtual hosts name
	protected String name;
	//the domain
	protected String domain;	
	
	protected boolean autoDeploy;
	protected boolean liveDeploy;
	protected boolean startChildren = true;
	protected boolean unpackWARs;	
	
	/**
	 * MBean object name used for de/registration purposes.
	 */
	private ObjectName oName;	
	
	private String defaultApplicationContextId = "default.context";
	
	/**
	 * Initialization.
	 */
	public void init() {
		log.info("Loading tomcat virtual host");

		if (webappFolder != null) {
			//check for match with base webapp root
			if (webappFolder.equals(webappRoot)) {
				log.error("Web application root cannot be the same as base");
				return;
			}
		}
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		
		//ensure we have a host
		if (host == null) {
			host = createHost();
		}
		
		host.setParentClassLoader(classloader);
		
		String propertyPrefix = name;
		if (domain != null) {
			propertyPrefix += '_' + domain.replace('.', '_');
		}
		log.debug("Generating name (for props) {}", propertyPrefix);
		System.setProperty(propertyPrefix + ".webapp.root", webappRoot);
		log.info("Virtual host root: " + webappRoot);

		// Root applications directory
		File appDirBase = new File(webappRoot);
		// Subdirs of root apps dir
		File[] dirs = appDirBase.listFiles(new TomcatLoader.DirectoryFilter());
		// Search for additional context files
		for (File dir : dirs) {
			String dirName = '/' + dir.getName();
			// check to see if the directory is already mapped
			if (null == host.findChild(dirName)) {
			    String webappContextDir = formatPath(appDirBase.getAbsolutePath(), dirName);
				Context ctx = null;
				if ("/root".equals(dirName) || "/root".equalsIgnoreCase(dirName)) {
					log.debug("Adding ROOT context");
					ctx = addContext("/", webappContextDir);
				} else {
					log.debug("Adding context from directory scan: {}", dirName);
					ctx = addContext(dirName, webappContextDir);
				}
				if (ctx != null) {
    				Object ldr = ctx.getLoader();
    				if (ldr != null) {
    					if (ldr instanceof WebappLoader) {
    						log.debug("Replacing context loader");				
    						((WebappLoader) ldr).setLoaderClass("org.red5.server.tomcat.WebappClassLoader");
    					} else {
    						log.debug("Context loader was instance of {}", ldr.getClass().getName());
    					}
    				} else {
    					log.debug("Context loader was null");
    					WebappLoader wldr = new WebappLoader(classloader);
    					wldr.setLoaderClass("org.red5.server.tomcat.WebappClassLoader");
    					ctx.setLoader(wldr);
    				}  				    				
				}	
				webappContextDir = null;			
			}
		}
        appDirBase = null;
        dirs = null;

		// Dump context list
		if (log.isDebugEnabled()) {
			for (Container cont : host.findChildren()) {
				log.debug("Context child name: " + cont.getName());
			}
		}

		engine.addChild(host);

		// Start server
		try {
			log.info("Starting Tomcat virtual host");	

			//may not have to do this step for every host
			LoaderBase.setApplicationLoader(new TomcatApplicationLoader(embedded, host, applicationContext));
			
			for (Container cont : host.findChildren()) {
				if (cont instanceof StandardContext) {
					StandardContext ctx = (StandardContext) cont;			
						
            		ServletContext servletContext = ctx.getServletContext();
            		log.debug("Context initialized: {}", servletContext.getContextPath());
            		
            		String prefix = servletContext.getRealPath("/");
            		log.debug("Path: {}", prefix);
            
            		try {
            			Loader cldr = ctx.getLoader();
            			log.debug("Loader type: {}", cldr.getClass().getName());
            			ClassLoader webClassLoader = cldr.getClassLoader();
            			log.debug("Webapp classloader: {}", webClassLoader);
            			//create a spring web application context
            			XmlWebApplicationContext appctx = new XmlWebApplicationContext();
            			appctx.setClassLoader(webClassLoader);
            			appctx.setConfigLocations(new String[]{"/WEB-INF/red5-*.xml"});
            			appctx.setParent((ApplicationContext) applicationContext.getBean(defaultApplicationContextId));					
            			appctx.setServletContext(servletContext);
            			//set the root webapp ctx attr on the each servlet context so spring can find it later					
            			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appctx);
            			appctx.refresh();
            		} catch (Throwable t) {
            			log.error("Error setting up context: {}", servletContext.getContextPath(), t);
            			if (log.isDebugEnabled()) {
            				t.printStackTrace();
            			}
            		}					
				}
			}	
		} catch (Exception e) {
			log.error("Error loading Tomcat virtual host", e);
		}			
		
	}

	/**
	 * Un-initialization.
	 */	
	public void uninit() {
		log.debug("TomcatVHostLoader un-init");		
		Container[] children = host.findChildren();
		for (Container c : children) {
			if (c instanceof StandardContext) {
				try {
					((StandardContext) c).stop();
					host.removeChild(c);			
				} catch (Exception e) {
					log.error("Could not stop context: {}", c.getName(), e);
				}				
			}
		}
		//remove system prop
		String propertyPrefix = name;
		if (domain != null) {
			propertyPrefix += '_' + domain.replace('.', '_');
		}		
		System.clearProperty(propertyPrefix + ".webapp.root");
		//stop the host
		try {
			((StandardHost) host).stop();
		} catch (LifecycleException e) {
			log.error("Could not stop host: {}", host.getName(), e);
		}
		//remove host
		engine.removeChild(host);
		//unregister jmx
		unregisterJMX();
	}	
	
	/**
	 * Create a standard host.
	 * 
	 * @return
	 */
	public Host createHost() {
		log.debug("Creating host");
		StandardHost stdHost = new StandardHost();
		stdHost.setAppBase(webappRoot);
		stdHost.setAutoDeploy(autoDeploy);
		if (domain == null) {
			stdHost.setName(name);
		} else {
			stdHost.setDomain(domain);
			//seems to require that the domain be appended to the name
			stdHost.setName(name + '.' + domain);
		}
		stdHost.setLiveDeploy(liveDeploy);
		//stdHost.setParent(container);
		stdHost.setStartChildren(startChildren);
		stdHost.setUnpackWARs(unpackWARs);
		//stdHost.setWorkDir(workDir);
		stdHost.setXmlNamespaceAware(false);
		stdHost.setXmlValidation(false);
		
		return stdHost;
	}
	
	/**
	 * Returns the current host.
	 *
	 * @return
	 */
	public Host getHost() {
		return host;
	}
	
	/**
	 * Adds an alias to the current host.
	 * 
	 * @param alias
	 */
	public void addAlias(String alias) {
		log.debug("Adding alias: {}", alias);
		host.addAlias(alias);
	}	
	
	/**
	 * Removes an alias from the current host.
	 * 
	 * @param alias
	 */
	public void removeAlias(String alias) {
		log.debug("Removing alias: {}", alias);
		String[] aliases = host.findAliases();
		for (String s : aliases) {
			if (alias.equals(s)) {
				host.removeAlias(alias);
				break;
			}
		}
	}
	
	/**
	 * Adds a valve to the current host.
	 * 
	 * @param valve
	 */
	public void addValve(Valve valve) {
		log.debug("Adding valve: {}", valve);
		log.debug("Valve info: {}", valve.getInfo());
		((StandardHost) host).addValve(valve);
	}
	
	/**
	 * Removes a valve from the current host.
	 * 
	 * @param valveInfo
	 */
	public void removeValve(String valveInfo) {
		log.debug("Removing valve: {}", valveInfo);
		try {
			String[] valveNames = ((StandardHost) host).getValveNames();
			for (String s : valveNames) {
				log.debug("Valve name: {}", s);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Valve[] valves = ((StandardHost) host).getValves();
		for (Valve v : valves) {
			log.debug("Valve: {}", v);
			log.debug("Valve info: {}", v.getInfo());
		}		
		
		//TODO: fix removing valves
		//((StandardHost) host).removeValve(valve);	
	}
	
	/**
	 * Set additional contexts.
	 * 
	 * @param contexts
	 *            Map of contexts
	 */
	@Override
	public void setContexts(Map<String, String> contexts) {
		log.debug("setContexts: {}", contexts.size());
		for (String key : contexts.keySet()) {
			host.addChild(embedded.createContext(key, webappRoot + contexts.get(key)));
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getWebappRoot() {
		return webappRoot;
	}

	public void setWebappRoot(String webappRoot) {
		this.webappRoot = webappRoot;
	}

	public boolean getAutoDeploy() {
		return autoDeploy;
	}

	public void setAutoDeploy(boolean autoDeploy) {
		this.autoDeploy = autoDeploy;
	}

	public boolean getLiveDeploy() {
		return liveDeploy;
	}

	public void setLiveDeploy(boolean liveDeploy) {
		this.liveDeploy = liveDeploy;
	}

	public boolean getStartChildren() {
		return startChildren;
	}

	public void setStartChildren(boolean startChildren) {
		this.startChildren = startChildren;
	}

	public boolean getUnpackWARs() {
		return unpackWARs;
	}

	public void setUnpackWARs(boolean unpackWARs) {
		this.unpackWARs = unpackWARs;
	}
	
	public String getDefaultApplicationContextId() {
		return defaultApplicationContextId;
	}

	public void setDefaultApplicationContextId(String defaultApplicationContextId) {
		this.defaultApplicationContextId = defaultApplicationContextId;
	}

	public void registerJMX() {
		oName = JMXFactory.createObjectName("type", "TomcatVHostLoader", "name", name, "domain", domain);
		JMXAgent.registerMBean(this, this.getClass().getName(),	TomcatVHostLoaderMBean.class, oName);
	}
	
	public void unregisterJMX() {	
		JMXAgent.unregisterMBean(oName);
	}	
	
}