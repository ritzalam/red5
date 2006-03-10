package org.red5.server.api;

import java.util.Set;

/**
 * Base Interface for all api objects with attributes
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
	 * @param	 name	the name of the attribute to change
	 * @param	 value	the new value of the attribute
	 */
	public void setAttribute(String name,Object value);
	
	/**
	 * Return the value for a given attribute.
	 * 
	 * @param	 name	the name of the attribute to get
	 * @return	the attribute value or null if the attribute doesn't exist
	 */
	public Object getAttribute(String name);
	
	/**
	 * Check the object has an attribute
	 * 
	 * @param name	the name of the attribute to check
	 * @return true if the attribute exists otherwise false
	 */
	public boolean hasAttribute(String name);
	
	/**
	 * Removes an attribute
	 * 
	 * @param name the name of the attribute to remove
	 */
	public void removeAttribute(String name);
	
}
