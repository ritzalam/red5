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
import org.red5.io.object.BaseOutput;
import org.red5.io.object.DataTypes;

public class Output extends BaseOutput implements org.red5.io.object.Output {

	protected static Log log = LogFactory.getLog(Output.class.getName());

	protected List<Object> list;

	public Output(List<Object> list) {
		super();
		this.list = list;
	}

	public boolean isCustom(Object custom) {
		// No custom types supported
		return false;
	}

	// DONE
	public void markElementSeparator() {
		list.add(Byte.valueOf(Mock.TYPE_ELEMENT_SEPARATOR));
	}

	// DONE
	public void markEndArray() {
		list.add(Byte.valueOf(Mock.TYPE_END_OF_ARRAY));
	}

	public void markEndObject() {
		list.add(Byte.valueOf(Mock.TYPE_END_OF_OBJECT));
	}

	public void markEndMap() {
		list.add(Byte.valueOf(Mock.TYPE_END_OF_MAP));
	}

	// DONE
	public void markPropertySeparator() {
		log.info("PROPERTY SEPARATOR");
		list.add(Byte.valueOf(Mock.TYPE_PROPERTY_SEPARATOR));
	}

	public void markItemSeparator() {
		log.info("ITEM SEPARATOR");
		list.add(Byte.valueOf(Mock.TYPE_ITEM_SEPARATOR));
	}

	public boolean supportsDataType(byte type) {
		// does not yet support references 
		return type <= DataTypes.OPT_REFERENCE;
	}

	// DONE
	public void writeBoolean(Boolean bol) {
		list.add(Byte.valueOf(DataTypes.CORE_BOOLEAN));
		list.add(bol);
	}

	public void writeCustom(Object custom) {
		// Customs not supported by this version
	}

	public void writeDate(Date date) {
		list.add(Byte.valueOf(DataTypes.CORE_DATE));
		list.add(date);
	}

	// DONE
	public void writeNull() {
		list.add(Byte.valueOf(DataTypes.CORE_NULL));
	}

	// DONE
	public void writeNumber(Number num) {
		list.add(Byte.valueOf(DataTypes.CORE_NUMBER));
		list.add(num);
	}

	public void writePropertyName(String name) {
		list.add(name);
	}

	public void writeItemKey(String key) {
		list.add(key);
	}

	public void writeReference(Object obj) {
		list.add(Byte.valueOf(DataTypes.OPT_REFERENCE));
		list.add(Short.valueOf(getReferenceId(obj)));
	}

	public void writeStartArray(int length) {
		list.add(Byte.valueOf(DataTypes.CORE_ARRAY));
		list.add(Integer.valueOf(length));
	}

	public void writeStartMap(int highestIndex) {
		list.add(Byte.valueOf(DataTypes.CORE_MAP));
		list.add(Integer.valueOf(highestIndex));
	}

	public void writeStartObject(String className) {
		list.add(Byte.valueOf(DataTypes.CORE_OBJECT));
		list.add(className == null ? null : className);
	}

	public void writeString(String string) {
		list.add(Byte.valueOf(DataTypes.CORE_STRING));
		list.add(string);
	}

	public void writeXML(String xml) {
		list.add(Byte.valueOf(DataTypes.CORE_XML));
		list.add(xml);
	}

}
