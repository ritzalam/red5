package org.red5.io.amf3;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;

/**
 * AMF3 output writer
 *
 * @see  org.red5.io.amf3.AMF3
 * @see  org.red5.io.amf3.Input
 * @author The Red5 Project (red5@osflash.org)
 */
public class Output implements org.red5.io.object.Output {

	protected static Log log = LogFactory.getLog(Output.class.getName());

	protected ByteBuffer buffer;

	/**
	 * Constructor of AMF3 output.
	 *
	 * @param buffer
	 *            instance of ByteBuffer
	 * @see ByteBuffer
	 */
	public Output(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public boolean supportsDataType(byte type) {
		return false;
	}// Basic Data Types

	public void writeNumber(Number num) {

	}

	public void writeBoolean(Boolean bol) {

	}

	public void writeString(String string) {

	}

	public void writeDate(Date date) {

	}

	public void writeNull() {

	}// Complex Data Types

	public void writeStartArray(int length) {

	}

	public void markElementSeparator() {

	}

	public void markEndArray() {

	}

	public void writeStartMap(int size) {

	}

	public void writeItemKey(String key) {

	}

	public void markItemSeparator() {

	}

	public void markEndMap() {

	}

	public void writeStartObject(String classname) {

	}

	public void writePropertyName(String name) {

	}

	public void markPropertySeparator() {

	}

	public void markEndObject() {

	}

	public void writeXML(String xml) {

	}// Reference to Complex Data Type

	public void writeReference(Object obj) {

	}// Custom datatypes can be handled by

	public boolean isCustom(Object custom) {
		return false;
	}

	public void writeCustom(Object custom) {

	}

	public void storeReference(Object obj) {

	}

	public boolean hasReference(Object obj) {
		return false;
	}

	public void clearReferences() {

	}
}
