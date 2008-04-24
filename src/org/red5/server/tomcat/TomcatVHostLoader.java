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

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 loader for Tomcat virtual hosts.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class TomcatVHostLoader extends TomcatLoader implements TomcatVHostLoaderMBean {

	// Initialize Logging
	protected static Logger log = LoggerFactory.getLogger(TomcatVHostLoader.class);

	/**
	 * Base web applications directory
	 */
	protected String webappRoot;
	
	protected String name;
	protected String domain;	
	
	protected boolean autoDeploy = true;
	protected boolean liveDeploy;
	protected boolean startChildren;
	protected boolean unpackWARs = true;	
	
	/**
	 * MBean object name used for de/registration purposes.
	 */
	private ObjectName oName;	
	
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
		
		//ensure we have a host
		if (host == null) {
			host = createHost();
		}
		
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
				if ("/root".equals(dirName) || "/root".equalsIgnoreCase(dirName)) {
					log.debug("Adding ROOT context");
					this.addContext("/", webappRoot + '/' + dirName);
				} else {
					log.debug("Adding context from directory scan: {}", dirName);
					this.addContext(dirName, webappRoot + '/' + dirName);
				}
			}
		}

		// Dump context list
		if (log.isDebugEnabled()) {
			for (Container cont : host.findChildren()) {
				log.debug("Context child name: " + cont.getName());
			}
		}

		engine.addChild(host);

		//may not have to do this step for every host
		//setApplicationLoader(new TomcatApplicationLoader(embedded, host, applicationContext.get()));
				
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
		JMXAgent.unregisterMBean(oName);
	}	
	
	/**
	 * Create a standard host.
	 * 
	 * @return
	 */
	public Host createHost() {
		StandardHost stdHost = new StandardHost();
		stdHost.setAppBase(webappRoot);
		stdHost.setAutoDeploy(autoDeploy);
		if (domain != null) {
			stdHost.setDomain(domain);
		}
		stdHost.setLiveDeploy(liveDeploy);
		stdHost.setName(name);
		//stdHost.setParent(container);
		stdHost.setStartChildren(startChildren);
		stdHost.setUnpackWARs(unpackWARs);
		//stdHost.setWorkDir(workDir);
		stdHost.setXmlNamespaceAware(false);
		stdHost.setXmlValidation(false);
		
		return stdHost;
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
	
	public void registerJMX() {
		oName = JMXFactory.createObjectName("type", "TomcatVHostLoader", "name", name, "domain", domain);
		JMXAgent.registerMBean(this, this.getClass().getName(),	TomcatVHostLoaderMBean.class, oName);
	}
	
	public void unregisterJMX() {	
		JMXAgent.unregisterMBean(oName);
	}	
	
}