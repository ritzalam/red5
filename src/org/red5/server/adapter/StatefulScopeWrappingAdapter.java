package org.red5.server.adapter;

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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeAware;
import org.springframework.core.io.Resource;

/**
 * StatefulScopeWrappingAdapter class wraps stateful IScope functionality. That is, it has
 * attributes that you can work with, subscopes, associated resources and connections.
 *
 */
public class StatefulScopeWrappingAdapter extends AbstractScopeAdapter 
	implements IScopeAware, IAttributeStore {
		
	protected IScope scope; 
	
	public void setScope(IScope scope) {
		this.scope = scope;	
	}

	public IScope getScope(){
		return scope;
	}
	
	public Object getAttribute(String name) {
		return scope.getAttribute(name);
	}

	public Object getAttribute(String name, Object defaultValue) {
		return scope.getAttribute(name, defaultValue);
	}
	
	public Set<String> getAttributeNames() {
		return scope.getAttributeNames();
	}

	public boolean hasAttribute(String name) {
		return scope.hasAttribute(name);
	}

	public boolean removeAttribute(String name) {
		return scope.removeAttribute(name);
	}

	public void removeAttributes() {
		scope.removeAttributes();
	}

	public boolean setAttribute(String name, Object value) {
		return scope.setAttribute(name,value);
	}

	public void setAttributes(IAttributeStore values) {
		scope.setAttributes(values);
	}

	public void setAttributes(Map<String, Object> values) {
		scope.setAttributes(values);
	}
	
	public boolean createChildScope(String name) {
		return scope.createChildScope(name);
	}

	public IScope getChildScope(String name) {
		return scope.getScope(name);
	}

	public Iterator<String> getChildScopeNames() {
		return scope.getScopeNames();
	}

	public Set<IClient> getClients() {
		return scope.getClients();
	}

	public Iterator<IConnection> getConnections() {
		return scope.getConnections();
	}

	public IContext getContext() {
		return scope.getContext();
	}

	public int getDepth() {
		return scope.getDepth();
	}

	public String getName() {
		return scope.getName();
	}

	public IScope getParent() {
		return scope.getParent();
	}

	public String getPath() {
		return scope.getPath();
	}

	public boolean hasChildScope(String name) {
		return scope.hasChildScope(name);
	}

	public boolean hasParent() {
		return scope.hasParent();
	}

	public Set<IConnection> lookupConnections(IClient client) {
		return scope.lookupConnections(client);
	}

	public Resource[] getResources(String pattern) throws IOException {
		return scope.getResources(pattern);
	}

	public Resource getResource(String path) {
		return scope.getResource(path);
	} 

}
