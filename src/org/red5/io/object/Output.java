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
	void writeNumber(Number num);

	void writeBoolean(Boolean bol);

	void writeString(String string);

	void writeDate(Date date);

	void writeNull();

	// Complex Data Types
	void writeStartArray(int length);

	void markElementSeparator();

	void markEndArray();

	void writeStartMap(int size);

	void writeItemKey(String key);

	void markItemSeparator();

	void markEndMap();

	void writeStartObject(String classname);

	void writeStartObject(String classname, int numMembers);

	void writePropertyName(String name);

	void markPropertySeparator();

	void markEndObject();

	void writeXML(String xml);

	// Reference to Complex Data Type
	void writeReference(Object obj);

	// Custom datatypes can be handled by
	boolean isCustom(Object custom);

	void writeCustom(Object custom);

	void storeReference(Object obj);

	boolean hasReference(Object obj);

	void clearReferences();
}
