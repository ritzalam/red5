package org.red5.io.mock;

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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.BaseInput;

public class Input extends BaseInput implements org.red5.io.object.Input {

	protected static Log log = LogFactory.getLog(Input.class.getName());

	protected List list;

	protected int idx;

	public Input(List list) {
		super();
		this.list = list;
		this.idx = 0;
	}

	protected Object getNext() {
		return list.get(idx++);
	}

	public byte readDataType() {
		Byte b = (Byte) getNext();
		return b.byteValue();
	}

	// Basic

	public Object readNull() {
		return null;
	}

	public Boolean readBoolean() {
		return (Boolean) getNext();
	}

	public Number readNumber() {
		return (Number) getNext();
	}

	public String readString() {
		return (String) getNext();
	}

	public Date readDate() {
		return (Date) getNext();
	}

	// Array

	public int readStartArray() {
		Integer i = (Integer) getNext();
		return i.intValue();
	}

	public void skipElementSeparator() {
		getNext();
	}

	public void skipEndArray() {
		// SKIP
	}

	public boolean hasMoreItems() {
		Object next = list.get(idx);
		if (!(next instanceof Byte)) {
			return true;
		}
		Byte b = (Byte) next;
		return (b.byteValue() != Mock.TYPE_END_OF_MAP);
	}

	public String readItemKey() {
		return (String) getNext();
	}

	public int readStartMap() {
		return ((Integer) getNext()).intValue();
	}

	public void skipEndMap() {
		getNext();
	}

	public void skipItemSeparator() {
		getNext();
	}

	// Object

	public String readStartObject() {
		return readString();
	}

	public boolean hasMoreProperties() {
		Object next = list.get(idx);
		if (!(next instanceof Byte)) {
			return true;
		}
		Byte b = (Byte) next;
		return (b.byteValue() != Mock.TYPE_END_OF_OBJECT);
	}

	public String readPropertyName() {
		return (String) getNext();
	}

	public void skipPropertySeparator() {
		getNext();
	}

	public void skipEndObject() {
		getNext();
	}

	// Others

	public String readXML() {
		return readString();
	}

	public Object readCustom() {
		// Not supported
		return null;
	}

	public Object readReference() {
		final Short num = (Short) getNext();
		return getReference(num.shortValue());
	}

}
