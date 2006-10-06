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
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.cache.ICacheStore;
import org.red5.server.api.cache.ICacheable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Provides an implementation of an object cache which actually
 * does not provide a cache.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class NoCacheImpl implements ICacheStore, ApplicationContextAware {

	protected static Log log = LogFactory.getLog(NoCacheImpl.class.getName());

	/*
	 * This constructor helps to ensure that we are singleton.
	 */
	private NoCacheImpl() {
	}

	// We store the application context in a ThreadLocal so we can access it
	// later.
	private static ApplicationContext applicationContext = null;

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		NoCacheImpl.applicationContext = context;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public Iterator<String> getObjectNames() {
		return null;
	}

	public Iterator<SoftReference<? extends ICacheable>> getObjects() {
		return null;
	}

	public boolean offer(String key, ByteBuffer obj) {
		return false;
	}

	public boolean offer(String name, ICacheable obj) {
		return false;
	}

	public void put(String name, Object obj) {
	}

	public void put(String name, ICacheable obj) {
	}

	public ICacheable get(String name) {
		return null;
	}

	public boolean remove(ICacheable obj) {
		return false;
	}

	public boolean remove(String name) {
	    return false;
	}

	public static long getCacheHit() {
		return 0;
	}

	public static long getCacheMiss() {
		return 0;
	}

	public void setMaxEntries(int max) {
	}

	public void destroy() {
	}
}
