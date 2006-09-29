package org.red5.server.persistence;

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
import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Persistence implementation that stores the objects in memory.
 * This serves as default persistence if nothing has been configured.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RamPersistence implements IPersistenceStore {

	/** This is used in the id for objects that have a name of <code>null</code> **/
	protected static final String PERSISTENCE_NO_NAME = "__null__";

	protected Map<String, IPersistable> objects = new HashMap<String, IPersistable>();

	protected ResourcePatternResolver resources;

	public RamPersistence(ResourcePatternResolver resources) {
		this.resources = resources;
	}

	public RamPersistence(IScope scope) {
		this((ResourcePatternResolver) ScopeUtils.findApplication(scope));
	}

	protected String getObjectName(String id) {
		// The format of the object id is <type>/<path>/<objectName>
		String result = id.substring(id.lastIndexOf('/') + 1);
		if (result.equals(PERSISTENCE_NO_NAME)) {
			result = null;
		}
		return result;
	}

	protected String getObjectPath(String id, String name) {
		// The format of the object id is <type>/<path>/<objectName>
		id = id.substring(id.indexOf('/') + 1);
		if (id.startsWith("/")) {
			id = id.substring(1);
		}
		if (id.lastIndexOf(name) == -1)
			return id;
		return id.substring(0, id.lastIndexOf(name)-1);
	}

	protected String getObjectId(IPersistable object) {
		// The format of the object id is <type>/<path>/<objectName>
		String result = object.getType();
		if (!object.getPath().startsWith("/")) {
			result += "/";
		}
		result += object.getPath();
		if (!result.endsWith("/")) {
			result += "/";
		}
		String name = object.getName();
		if (name == null) {
			name = PERSISTENCE_NO_NAME;
		}
		if (name.startsWith("/")) {
			// "result" already ends with a slash
			name = name.substring(1);
		}
		return result + name;
	}

	public synchronized boolean save(IPersistable object) {
		objects.put(getObjectId(object), object);
		object.setPersistent(true);
		return true;
	}

	public synchronized IPersistable load(String name) {
		return objects.get(name);
	}

	public boolean load(IPersistable obj) {
		return obj.isPersistent();
	}

	public synchronized boolean remove(IPersistable object) {
		return remove(getObjectId(object));
	}

	public synchronized boolean remove(String name) {
		if (!objects.containsKey(name)) {
			return false;
		}

		IPersistable object = objects.remove(name);
		object.setPersistent(false);
		return true;
	}

	public Iterator<String> getObjectNames() {
		return objects.keySet().iterator();
	}

	public Iterator<IPersistable> getObjects() {
		return objects.values().iterator();
	}
}
