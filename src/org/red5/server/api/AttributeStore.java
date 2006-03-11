package org.red5.server.api;

import java.util.Set;

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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

/**
 * Base Interface for all API objects with attributes
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface AttributeStore {

	/**
	 * Get the attribute names
	 * 
	 * @return set containing all attribute names
	 */
	public Set getAttributeNames();

	/**
	 * Set an attribute on this object
	 * 
	 * @param name
	 *            the name of the attribute to change
	 * @param value
	 *            the new value of the attribute
	 */
	public void setAttribute(String name, Object value);

	/**
	 * Return the value for a given attribute.
	 * 
	 * @param name
	 *            the name of the attribute to get
	 * @return the attribute value or null if the attribute doesn't exist
	 */
	public Object getAttribute(String name);

	/**
	 * Check the object has an attribute
	 * 
	 * @param name
	 *            the name of the attribute to check
	 * @return true if the attribute exists otherwise false
	 */
	public boolean hasAttribute(String name);

	/**
	 * Removes an attribute
	 * 
	 * @param name
	 *            the name of the attribute to remove
	 */
	public void removeAttribute(String name);

}
