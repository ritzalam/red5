package org.red5.classloading;

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

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Implementation of a class loader specific to Red5. 
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Red5ClassLoader extends URLClassLoader {

	public Red5ClassLoader(URL[] urls) {
		super(urls);
	}

	public Red5ClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public void addURL(URL url) {
		super.addURL(url);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		//System.out.printf("Find class: %s\n", name);
		return super.findClass(name);
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, false);
	}

	/**
	 * We override the parent-first behavior established by
	 * java.lang.Classloader.
	 * <p>
	 * The implementation is surprisingly straightforward.
	 * 
	 * @param name
	 *            the name of the class to load, should not be <code>null</code>
	 *            .
	 * 
	 * @param resolve
	 *            flag that indicates whether the class should be resolved.
	 * 
	 * @return the loaded class, never <code>null</code>.
	 * 
	 * @throws ClassNotFoundException
	 *             if the class could not be loaded.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		//System.out.printf("Load class: %s resolve: %s\n", name, resolve);
		
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);

		// if not loaded, search the local (child) resources
		if (c == null) {
			try {
				c = findClass(name);
			} catch (ClassNotFoundException cnfe) {
				// ignore
			}
		}

		// If we could not find it, delegate to parent
		// Note that we do not attempt to catch any ClassNotFoundException
		if (c == null) {
			if (getParent() != null) {
				c = getParent().loadClass(name);
			} else {
				c = getSystemClassLoader().loadClass(name);
			}
		}

		// Resolve the class, if required
		if (resolve) {
			resolveClass(c);
		}

		return c;
	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		//System.out.printf("Get resource as stream: %s\n", name);
		return super.getResourceAsStream(name);
	}
	
	/**
	 * Override the parent-first resource loading model established by
	 * java.lang.Classloader with child-first behavior.
	 * 
	 * @param name
	 *            the name of the resource to load, should not be
	 *            <code>null</code>.
	 * 
	 * @return a {@link URL} for the resource, or <code>null</code> if it could
	 *         not be found.
	 */
	@Override
	public URL getResource(String name) {
		//System.out.printf("Get resource: %s\n", name);
		URL url = findResource(name);

		// If local search failed, delegate to parent
		if (url == null) {
			//System.out.printf("Parent classloader: %s\n", getParent());
			url = getParent().getResource(name);
		}

		//System.out.printf("Resource url: %s\n", url);
		return url;
	}
	
	
}