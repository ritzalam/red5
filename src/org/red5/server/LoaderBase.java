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
import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IApplicationContext;
import org.red5.server.api.IApplicationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Base class for all J2EE application loaders.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class LoaderBase implements ApplicationContextAware {

	private static Logger log = LoggerFactory.getLogger(LoaderBase.class);
	
	/**
	 * We store the application context so we can access it later.
	 */
	protected static ApplicationContext applicationContext = null;

	/**
	 * Current Red5 application context, set by the different loaders.
	 */
	public static Map<String, IApplicationContext> red5AppCtx = new HashMap<String, IApplicationContext>();
	
	/**
	 * Loader for new applications.
	 */
	protected static ThreadLocal<IApplicationLoader> loader = new ThreadLocal<IApplicationLoader>();

	/**
	 * Folder containing the webapps.
	 */
	protected String webappFolder = null;

	/**
	 * Getter for the application loader.
	 * 
	 * @return Application loader
	 */ 
	public static IApplicationLoader getApplicationLoader() {
		log.debug("Get application loader");
		return loader.get();
	}
	
	/**
	 * Setter for the application loader.
	 * 
	 * @param loader Application loader
	 */
	public static void setApplicationLoader(IApplicationLoader loader) {
		log.debug("Set application loader: {}", loader);
		LoaderBase.loader.set(loader);
	}
	
	/**
	 * Returns the map containing all of the registered Red5 application contexts.
	 * 
	 * @return a map
	 */
	public static Map<String, IApplicationContext> getRed5ApplicationContexts() {
		log.debug("Get all red5 application contexts");
		return red5AppCtx;
	}
	
	/**
	 * Getter for a Red5 application context.
	 * @param path path
	 * 
	 * @return Red5 application context 
	 */
	public static IApplicationContext getRed5ApplicationContext(String path) {
		log.debug("Get red5 application context - path: {}", path);
		//log.trace("Map at get: {}", red5AppCtx);
		return red5AppCtx.get(path);
	}
	
	/**
	 * Setter for a Red5 application context.
	 * @param path path
	 * 
	 * @param context Red5 application context
	 */
	public static void setRed5ApplicationContext(String path, IApplicationContext context) {
		log.debug("Set red5 application context - path: {} context: {}", path, context);
		//log.trace("Map at set: {}", red5AppCtx);
		if (context != null) {
			red5AppCtx.put(path, context);
		} else {
			red5AppCtx.remove(path);
		}
		//log.trace("Map after set: {}", red5AppCtx);
	}
	
	/**
	 * Remover for a Red5 application context.
	 * @param path path
	 * 
	 * @return Red5 application context 
	 */
	public static IApplicationContext removeRed5ApplicationContext(String path) {
		log.debug("Remove red5 application context - path: {}", path);
		return red5AppCtx.remove(path);
	}
	
	/**
	 * Getter for application context
	 * @return         Application context
	 */
	public static ApplicationContext getApplicationContext() {
		log.debug("Get application context: {}", applicationContext);
		return applicationContext;
	}
	
	/**
	 * Setter for application context.
	 * 
	 * @param context Application context
	 * @throws BeansException Abstract superclass for all exceptions thrown in the beans
	 *             package and subpackages
	 */
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		log.debug("Set application context: {}", context);
		applicationContext = context;
	}	
	
	/**
	 * Set the folder containing webapps.
	 * 
	 * @param webappFolder web app folder
	 */
	public void setWebappFolder(String webappFolder) {
		File fp = new File(webappFolder);
		if (!fp.canRead()) {
			throw new RuntimeException(String.format("Webapp folder %s cannot be accessed.", webappFolder));
		}
		if (!fp.isDirectory()) {
			throw new RuntimeException(String.format("Webapp folder %s doesn't exist.", webappFolder));
		}
		fp = null;
		this.webappFolder = webappFolder;
	}
	
	/**
	 * Remove context from the current host.
	 * 
	 * @param path		Path
	 */	
	public void removeContext(String path) {
		throw new UnsupportedOperationException();
	}

}
