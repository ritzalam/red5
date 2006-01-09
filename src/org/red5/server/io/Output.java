package org.red5.server.io;

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
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.util.Date;

/**
 * Output interface which defines contract methods to be implemented
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public interface Output {

	boolean supportsDataType(byte type);
	
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
	
	void writeStartList(int highestIndex);
	void writeItemIndex(int index);
	void markItemSeparator();
	void markEndList();
	
	void writeStartObject(String classname);
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
