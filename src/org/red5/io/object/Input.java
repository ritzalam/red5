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

	byte readDataType();

	/**
	 * Read a string without the string type header.
	 * 
	 * @return
	 */
	String getString();
	
	// Data Types
	Object readNull();

	Boolean readBoolean();

	Number readNumber();

	String readString();

	Date readDate();

	// Stuctures
	int readStartArray();

	void skipElementSeparator();

	void skipEndArray();

	int readStartMap();

	String readItemKey();

	void skipItemSeparator();

	boolean hasMoreItems();

	void skipEndMap();

	String readStartObject();

	String readPropertyName();

	void skipPropertySeparator();

	boolean hasMoreProperties();

	void skipEndObject();

	//int readStartXML();
	String readXML();

	Object readCustom();

	//void readEndXML();

	// Reference to Complex Data Type
	Object readReference();

	void storeReference(Object obj);

	void clearReferences();

}
