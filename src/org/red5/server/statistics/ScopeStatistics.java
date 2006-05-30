package org.red5.server.statistics;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.exception.ScopeNotFoundException;

/**
 * Public methods for XML-RPC scope statistics service.
 * 
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class ScopeStatistics {

	private IContext context;
	
	public ScopeStatistics(IContext context) {
		this.context = context;
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
			scope = ScopeUtils.resolveScope(context.getGlobalScope(), path);
		else
			scope = context.getGlobalScope();
		
		if (scope == null)
			throw new ScopeNotFoundException(context.getGlobalScope(), path);
		
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
			// TODO: allow other filter objects that can be sent through XML-RPC
			if (value instanceof String)
				result.put(name, value);
		}
		return result;
	}
}
