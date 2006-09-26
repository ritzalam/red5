package org.red5.server.cache;

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

import java.lang.ref.SoftReference;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.cache.ICacheStore;
import org.red5.server.api.cache.ICacheable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheConfiguration;
import com.whirlycott.cache.CacheManager;

/**
 * Provides an implementation of an object cache using whirlycache.
 * 
 * @see https://whirlycache.dev.java.net/
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class WhirlyCacheImpl implements ICacheStore, ApplicationContextAware {

	protected static Log log = LogFactory.getLog(WhirlyCacheImpl.class
			.getName());

	private static Cache cache;

	private CacheConfiguration cacheConfig;

	// We store the application context in a ThreadLocal so we can access it
	// later.
	private static ApplicationContext applicationContext = null;

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		WhirlyCacheImpl.applicationContext = context;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void init() {
		log.info("Loading whirlycache");
		// log.debug("Appcontext: " + applicationContext.toString());
		try {
			// instance the manager
			CacheManager cm = CacheManager.getInstance();
			// get the default cache that is created when a config file is not
			// found - we want to wire ours via Spring
			for (String nm : cm.getCacheNames()) {
				log.debug("Cache name: " + nm);
				if (nm.equals("default")) {
					// destroy the default cache
					cm.destroy("default");
				}
			}
			// Use the cache manager to create our cache
			cache = cm.createCache(cacheConfig);
		} catch (Exception e) {
			log.warn("Error on cache init", e);
		}
	}

	public ICacheable get(String name) {
		return (ICacheable) cache.retrieve(name);
	}

	public void put(String name, Object obj) {
		// Put an object into the cache
		cache.store(name, new CacheableImpl(obj));
	}

	public void put(String name, ICacheable obj) {
		// Put an object into the cache
		cache.store(name, obj);
	}

	public Iterator<String> getObjectNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator<SoftReference<? extends ICacheable>> getObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean offer(String name, ICacheable obj) {
		// Put an object into the cache
		cache.store(name, obj);
		// almost always returns true because store does not return a status
		return true;
	}

	public boolean remove(ICacheable obj) {
		return (null != cache.remove(obj.getName()));
	}

	public boolean remove(String name) {
		return (null != cache.remove(name));
	}

	public void setCacheConfig(CacheConfiguration cacheConfig) {
		this.cacheConfig = cacheConfig;
	}

	public void setMaxEntries(int capacity) {
		log.debug("Setting max entries for this cache to " + capacity);
		// cacheConfig.setMaxSize(capacity);
	}

	public void destroy() {
		// Shut down the cache manager
		try {
			CacheManager.getInstance().shutdown();
		} catch (Exception e) {
			log.warn("Error on cache shutdown", e);
		}
	}
}
