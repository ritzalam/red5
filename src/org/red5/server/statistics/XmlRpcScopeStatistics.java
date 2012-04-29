/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.statistics;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.exception.ScopeNotFoundException;
import org.red5.server.util.ScopeUtils;

/**
 * Public methods for XML-RPC scope statistics service.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class XmlRpcScopeStatistics {
	/**
	 * Global scope
	 */
	private IScope globalScope;

	/** Constructs a new XmlScopeStatistics. */
	public XmlRpcScopeStatistics() {

	}

	/**
	 * Create new scope statistic.
	 *
	 * @param globalScope        Global scope ref
	 */
	public XmlRpcScopeStatistics(IScope globalScope) {
		this.globalScope = globalScope;
	}

	/**
	 * Setter for global scope.
	 *
	 * @param scope Value to set for property 'globalScope'.
	 */
	public void setGlobalScope(IScope scope) {
		globalScope = scope;
	}

	/**
	 * Resolve path to scope.
	 * 
	 * @param path	Path to return scope for
	 * @return		The scope for the given path
	 *
	 * @throws ScopeNotFoundException	Thrown when scope with given path can't be resolved
	 */
	private IScope getScope(String path) throws ScopeNotFoundException {
		IScope scope;
		if (path != null && !path.equals("")) {
			scope = ScopeUtils.resolveScope(globalScope, path);
		} else {
			scope = globalScope;
		}

		if (scope == null) {
			throw new ScopeNotFoundException(globalScope, path);
		}

		return scope;
	}

	/**
	 * Return available applications. 
	 * 
	 * @return list of application names
	 */
	public String[] getScopes() {
		return getScopes(null);
	}

	/**
	 * Return subscopes of another scope.
	 * 
	 * @param path	Path of scope to return subscopes of
	 * @return		List of subscope names
	 */
	public String[] getScopes(String path) {
		IScope scope = getScope(path);
		Set<String> result = scope.getScopeNames();
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Return attributes of the global scope.
	 * 
	 * @return The scope's attributes
	 */
	public Map<String, Object> getScopeAttributes() {
		return getScopeAttributes(null);
	}

	/**
	 * Return an object that can be serialized through XML-RPC.
	 * Inspired by "Reflective XML-RPC" by "Stephan Maier".
	 * 
	 * @param value
	 * @return
	 */
	private Object getXMLRPCValue(Object value) {
		if (value == null) {
			return "<null>";
		}

		Class<?> type = value.getClass();
		if (type.equals(Integer.class) || type.equals(Double.class) || type.equals(Boolean.class) || type.equals(String.class) || type.equals(Date.class)) {
			return value;
		} else if (type.equals(Long.class)) {
			// XXX: long values are not supported by XML-RPC, convert to string
			// instead
			return ((Long) value).toString();
		} else if (type.isArray() && type.getComponentType().equals(byte.class)) {
			return value;
		} else if (type.isArray()) {
			int length = Array.getLength(value);
			Vector<Object> res = new Vector<Object>();
			for (int i = 0; i < length; i++) {
				res.add(getXMLRPCValue(Array.get(value, i)));
			}
			return res;
		} else if (value instanceof Map<?, ?>) {
			Hashtable<Object, Object> res = new Hashtable<Object, Object>();
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				res.put(entry.getKey(), getXMLRPCValue(entry.getValue()));
			}
			return res;
		} else if (value instanceof Collection<?>) {
			Collection<?> coll = (Collection<?>) value;
			Vector<Object> result = new Vector<Object>(coll.size());
			for (Object item : coll) {
				result.add(getXMLRPCValue(item));
			}
			return result;
		}

		throw new RuntimeException("Don't know how to convert " + value);
	}

	/**
	 * Return attributes of a given scope.
	 * 
	 * @param path	Path of scope to return attributes of
	 * @return		The scope's attributes
	 */
	public Map<String, Object> getScopeAttributes(String path) {
		IScope scope = getScope(path);
		Map<String, Object> result = new Hashtable<String, Object>();
		for (String name : scope.getAttributeNames()) {
			Object value = scope.getAttribute(name);
			try {
				result.put(name, getXMLRPCValue(value));
			} catch (RuntimeException err) {
				// Could not convert attribute for XML-RPC serialization.
			}
		}
		return result;
	}

	/**
	 * Return informations about shared objects of a given scope.
	 * 
	 * @param path	Path of scope to return shared objects for
	 * @return		A mapping containing the shared object name -> (persistent, data)
	 */
	public Map<String, Object> getSharedObjects(String path) {
		IScope scope = getScope(path);
		ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, false);
		if (service == null) {
			return new Hashtable<String, Object>();
		}

		Map<String, Object> result = new Hashtable<String, Object>();
		for (String name : service.getSharedObjectNames(scope)) {
			ISharedObject so = service.getSharedObject(scope, name);
			try {
				result.put(name, new Object[] { so.isPersistent(), getXMLRPCValue(so.getData()) });
			} catch (RuntimeException err) {
				// Could not convert attribute for XML-RPC serialization.
				result.put(name, "--- Error while serializing \"" + so.getData().toString() + "\" ---");
			}
		}
		return result;
	}
}
