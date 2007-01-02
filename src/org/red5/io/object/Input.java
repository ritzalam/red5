package org.red5.io.object;

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

import java.util.Date;

/**
 * Interface for Input which defines the contract methods which are
 * to be implemented. Input object provides
     * ways to read primitives, complex object and object references from byte buffer.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface Input {
    /**
     * Read type of data
     * @return         Type of data as byte
     */
	byte readDataType();

	/**
	 * Read a string without the string type header.
	 * 
	 * @return         String
	 */
	String getString();

    /**
     * Read Null data type
     * @return         Null datatype (AS)
     */
	Object readNull();

    /**
     * Read Boolean value
     * @return         Boolean
     */
    Boolean readBoolean();

    /**
     * Read Number object
     * @return         Number
     */
    Number readNumber();

    /**
     * Read String object
     * @return         String
     */
    String readString();

    /**
     * Read date object
     * @return         Date
     */
    Date readDate();

	// Stuctures

    /**
     * Read start of array marker.
     * @return               Size of array
     */
    int readStartArray();

    /**
     * Skip array element separator marker
     */
    void skipElementSeparator();

    /**
     * Skip end of array separator marker
     */
    void skipEndArray();

    /**
     * Read start of map marker
     * @return           Map size
     */
    int readStartMap();

    /**
     * Read item key from map
     * @return           Key name as String
     */
    String readItemKey();

    /**
     * Skip item separator
     */
    void skipItemSeparator();

    /**
     * Check whether there's more items in map to read and deserialize
     * @return        <code>true</code> if there are items to read, <code>false</code> otherwise
     */
    boolean hasMoreItems();

    /**
     * Skip map end marker
     */
    void skipEndMap();

    /**
     *
     * @return
     */
    String readStartObject();

    /**
     * 
     * @return
     */
    String readPropertyName();

    /**
     * Skip object property separator
     */
    void skipPropertySeparator();

    /**
     * Check whether more object properties are available
     * @return     <code>true</code> if there are properties to read, <code>false</code> otherwise
     */
    boolean hasMoreProperties();

    /**
     * Slip object end marker
     */
    void skipEndObject();

    /**
     * Read XML as String
     * @return       String representation of XML object
     */
	String readXML();

    /**
     * Read custom object
     * @return          Custom object
     */
    Object readCustom();

	//void readEndXML();

    /**
     * Read reference to Complex Data Type. Objects that are collaborators (properties) of other
     * objects must be stored as references in map of id-reference pairs.
     */
	Object readReference();

    /**
     * Store object reference. Objects that are collaborators (properties) of other
     * objects must be stored as references in map of id-reference pairs.
     * @param obj            Object
     */
    void storeReference(Object obj);

    /**
     * Clears all references
     */
    void clearReferences();

}
