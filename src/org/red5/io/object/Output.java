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
 * Output interface which defines contract methods to be implemented
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface Output {

	boolean supportsDataType(byte type);

	void putString(String string);

	// Basic Data Types
    /**
     * Write number
     * @param num       Number
     */
    void writeNumber(Number num);

    /**
     * Write boolean
     * @param bol       Boolean
     */
    void writeBoolean(Boolean bol);

    /**
     * Write string
     * @param string    String
     */
    void writeString(String string);

    /**
     * Write date
     * @param date      Date
     */
    void writeDate(Date date);

	void writeNull();

    /**
     * Write start of array marker
     * @param length              Array length
     */
    void writeStartArray(int length);

    /**
     * Write array element separator mark
     */
    void markElementSeparator();

    /**
     * Write end of array marker
     *
     */
    void markEndArray();

    /**
     * Write map object
     * @param size     Map size
     */
    void writeStartMap(int size);

    /**
     * Write map item key
     * @param key   Key name
     */
    void writeItemKey(String key);

    /**
     * Write map items separator mark
     */
    void markItemSeparator();

    /**
     * Write end of map object marker
     */
    void markEndMap();

    void writeStartObject(String classname, int numMembers);

    /**
     * Write start of object marker
     * @param classname    Object class name
     */
    void writeStartObject(String classname);

    /**
     * Write property name
     * @param name        Property name
     */
    void writePropertyName(String name);

    /**
     * Write object property separator marker
     */
    void markPropertySeparator();

    /**
     * Writes end of object marker
     */
    void markEndObject();

    /**
     * Write XML object
     * @param xml      XML as string
     */
    void writeXML(String xml);

    /**
     * Write reference to complex data type
     * @param obj   Referenced object
     */
	void writeReference(Object obj);

    /**
     * Whether object is custom
     *
     * @param custom           Object
     * @return                 true if object is of user type, false otherwise
     */
	boolean isCustom(Object custom);

    /**
     * Write custom (user) object
     * @param custom
     */
    void writeCustom(Object custom);

    /**
     * Store reference to object
     * @param obj              Referenced object
     */
    void storeReference(Object obj);

    /**
     * Check whether object is referenced
     * @param obj              Object
     * @return                 true if object is referenced, false otherwise
     */
    boolean hasReference(Object obj);

    /**
     * Clear references
     */
    void clearReferences();
}
