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
import org.red5.io.amf.AMF;
import org.red5.io.amf3.AMF3;

/**
 * AMF3 output writer
 *
 * @see  org.red5.io.amf3.AMF3
 * @see  org.red5.io.amf3.Input
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Output extends org.red5.io.amf.Output implements org.red5.io.object.Output {

	protected static Log log = LogFactory.getLog(Output.class.getName());

	/**
	 * Constructor of AMF3 output.
	 *
	 * @param buffer
	 *            instance of ByteBuffer
	 * @see ByteBuffer
	 */
	public Output(ByteBuffer buf) {
		super(buf);
	}

	public boolean supportsDataType(byte type) {
		return true;
	}// Basic Data Types

	public void writeBoolean(Boolean bol) {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(bol.booleanValue() ? AMF3.TYPE_BOOLEAN_TRUE : AMF3.TYPE_BOOLEAN_FALSE);
	}

	public void writeNull() {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_NULL);
	}

	protected void putInteger(long value) {
		if (value < 0) {
			System.err.println("MISSING: negative integer");
			return;
		}
		
		if (value <= 0x7f) {
			buf.put((byte) value);
		} else if (value <= 0x3fff) {
			buf.put((byte) (0x80 | ((value >> 7) & 0x7f)));
			buf.put((byte) (value & 0x7f));
		} else if (value <= 0x1fffff) {
			buf.put((byte) (0x80 | ((value >> 14) & 0x7f)));
			buf.put((byte) (0x80 | ((value >> 7) & 0x7f)));
			buf.put((byte) (value & 0x7f));
		} else {
			buf.put((byte) (0x80 | ((value >> 22) & 0xff)));
			buf.put((byte) (0x80 | ((value >> 15) & 0x7f)));
			buf.put((byte) (0x80 | ((value >> 8) & 0x7f)));
			buf.put((byte) (value & 0xff));
		}
	}
	
	protected void putString(java.nio.ByteBuffer string) {
		final int len = string.limit();
		// XXX: Support references!
		putInteger(len << 1 | 1);
		buf.put(string);
	}

	public void putString(String string) {
		final java.nio.ByteBuffer strBuf = AMF3.CHARSET.encode(string);
		putString(strBuf);
	}

	public void writeNumber(Number num) {
		if (num instanceof Long || num instanceof Integer || num instanceof Short || num instanceof Byte) {
			buf.put(AMF.TYPE_AMF3_OBJECT);
			buf.put(AMF3.TYPE_INTEGER);
			putInteger(num.longValue());
		} else {
			buf.put(AMF.TYPE_AMF3_OBJECT);
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		}
	}

	public void writeString(String string) {
		final java.nio.ByteBuffer strBuf = AMF3.CHARSET.encode(string);
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_STRING);
		putString(strBuf);
	}

	public void writeDate(Date date) {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_DATE);
		// XXX: Support references!
		putInteger(1);
		buf.putDouble(date.getTime());
	}

	public void writeStartArray(int length) {
		System.err.println("MISSING: writeStartArray");
	}

	public void markElementSeparator() {
		System.err.println("MISSING: markElementSeparator");
	}

	public void markEndArray() {
		System.err.println("MISSING: markEndArray");
	}

	public void writeStartMap(int size) {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_ARRAY);
		// XXX: Support references
		putInteger(size << 1 | 1);
	}

	public void writeItemKey(String key) {
		putString(key);
	}

	public void markItemSeparator() {
		System.err.println("MISSING: markItemSeparator");
	}

	public void markEndMap() {
		System.err.println("MISSING: markEndMap");
	}

	public void writeStartObject(String classname) {
		System.err.println("MISSING: writeStartObject");
	}

	public void writeStartObject(String classname, int numMembers) {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_OBJECT);
		// XXX: support object and classname references
		int value = numMembers << 4 | AMF3.TYPE_OBJECT_VALUE | 1;
		putInteger(value);
		if (classname == null) {
			putInteger(0);
		} else {
			putString(classname);
		}
	}

	public void writePropertyName(String name) {
		putString(name);
	}

	public void markPropertySeparator() {
		
	}

	public void markEndObject() {
		putInteger(0);
	}

	public void writeXML(String xml) {
		buf.put(AMF.TYPE_AMF3_OBJECT);
		buf.put(AMF3.TYPE_XML);
		putString(xml);
	}
}
