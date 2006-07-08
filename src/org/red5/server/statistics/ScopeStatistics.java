package org.red5.server.statistics;

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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.exception.ScopeNotFoundException;

/**
 * Public methods for XML-RPC scope statistics service.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ScopeStatistics {

	private IScope globalScope = null;
	
	public ScopeStatistics() {
		
	}
	
	public ScopeStatistics(IScope globalScope) {
		this.globalScope = globalScope;
	}
	
	public void setGlobalScope(IScope scope) {
		globalScope = scope;
	}
	
	/**
	 * Resolve path to scope.
	 * 
	 * @param path
	 * 			path to return scope for
	 * @return the scope for the given path
	 * @throws ScopeNotFoundException
	 */
	private IScope getScope(String path) throws ScopeNotFoundException {
		IScope scope;
		if (path != null && !path.equals("")) 
			scope = ScopeUtils.resolveScope(globalScope, path);
		else
			scope = globalScope;
		
		if (scope == null)
			throw new ScopeNotFoundException(globalScope, path);
		
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
	 * @param path
	 * 			path of scope to return subscopes of
	 * @return list of subscope names
	 */
	public String[] getScopes(String path) {
		IScope scope = getScope(path);
		List<String> result = new ArrayList<String>();
		Iterator<String> iter = scope.getScopeNames();
		while (iter.hasNext()) {
			String name = iter.next();
			result.add(name.substring(name.indexOf(IScope.SEPARATOR)+1));
		}
		
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	/**
	 * Return attributes of the global scope.
	 * 
	 * @return the scope's attributes
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
			return null;
		}
		
		Class type = value.getClass();
		if (type.equals(Integer.class)
			|| type.equals(Double.class)
			|| type.equals(Boolean.class)
			|| type.equals(String.class)
			|| type.equals(Date.class)) {
			return value;
		} else if (type.isArray() && type.getComponentType().equals(byte.class)) {
			return value;
		} else if (type.isArray()) {
			int length = Array.getLength(value);
			Vector<Object> res = new Vector<Object>();
			for (int i = 0; i < length; i++)
				res.add(getXMLRPCValue(Array.get(value, i)));
			return res;
		}
		
		throw new RuntimeException("Don't know how to convert " + value);
	}
	
	/**
	 * Return attributes of a given scope.
	 * 
	 * @param path
	 * 			path of scope to return attributes of
	 * @return the scope's attributes
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
}
