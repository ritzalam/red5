package org.red5.server.adapter;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import java.util.Collections;
import java.util.List;

/**
 * Provides configuration details for Applications.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@SuppressWarnings("unchecked")
public final class Config {

	/**
	 * Holder for classes implementing IoFilter.
	 */
	private List<String> filterNames = Collections.EMPTY_LIST;

	public List<String> getFilterNames() {
		return filterNames;
	}

	public void setFilterNames(List<String> filterNames) {
		this.filterNames = filterNames;
	}

}