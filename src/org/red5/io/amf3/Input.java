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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.object.BaseInput;
import org.red5.io.object.DataTypes;

/**
 * Input for red5 data (AMF3) types
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Input extends BaseInput implements org.red5.io.object.Input {

	protected static Log log = LogFactory.getLog(Input.class.getName());

	protected ByteBuffer buf;

	protected byte currentDataType;

	/**
	 * Input Constructor
	 * 
	 * @param buf
	 */
	public Input(ByteBuffer buf) {
		super();
		this.buf = buf;
	}

	/**
	 * Reads the data type
	 * 
	 * @return byte
	 */
	public byte readDataType() {

		if (buf == null) {
			log.error("Why is buf null?");
		}

		currentDataType = buf.get();
		byte coreType;

		switch (currentDataType) {

			case AMF3.TYPE_NULL:
				coreType = DataTypes.CORE_NULL;
				break;

			case AMF3.TYPE_INTEGER:
			case AMF3.TYPE_NUMBER:
				coreType = DataTypes.CORE_NUMBER;
				break;

			case AMF3.TYPE_BOOLEAN_TRUE:
			case AMF3.TYPE_BOOLEAN_FALSE:
				coreType = DataTypes.CORE_BOOLEAN;
				break;

			case AMF3.TYPE_STRING:
				coreType = DataTypes.CORE_STRING;
				break;
			// TODO check XML_SPECIAL
			case AMF3.TYPE_XML:
			case AMF3.TYPE_XML_SPECIAL:
				coreType = DataTypes.CORE_XML;
				break;
			case AMF3.TYPE_OBJECT:
				coreType = DataTypes.CORE_OBJECT;
				break;

			case AMF3.TYPE_ARRAY:
				// should we map this to list or array?
				coreType = DataTypes.CORE_ARRAY;
				break;

			case AMF3.TYPE_DATE:
				coreType = DataTypes.CORE_DATE;
				break;

			default:
				log.info("Unknown datatype: " + currentDataType);
				// End of object, and anything else lets just skip
				coreType = DataTypes.CORE_SKIP;
				break;
		}

		return coreType;
	}

	// Basic

	/**
	 * Reads a null
	 * 
	 * @return Object
	 */
	public Object readNull() {
		return null;
	}

	/**
	 * Reads a boolean
	 * 
	 * @return boolean
	 */
	public Boolean readBoolean() {
		return (currentDataType == AMF3.TYPE_BOOLEAN_TRUE) ? Boolean.TRUE
				: Boolean.FALSE;
	}

	/**
	 * Reads a Number
	 * 
	 * @return Number
	 */
	public Number readNumber() {
		if (currentDataType == AMF3.TYPE_NUMBER) {
			return buf.getDouble();
		} else {
			// we are decoding an int
			return readAMF3Integer();
		}
	}

	/**
	 * Reads a string
	 * 
	 * @return String
	 */
	public String readString() {
		int len = buf.getInt();
		// shift by one bit ?
		// is it a reference ? if not continue
		int limit = buf.limit();
		final java.nio.ByteBuffer strBuf = buf.buf();
		strBuf.limit(strBuf.position() + len);
		final String string = AMF3.CHARSET.decode(strBuf).toString();
		buf.limit(limit); // Reset the limit
		// save a reference
		return string;
	}

	/**
	 * Returns a date
	 * 
	 * @return Date
	 */
	public Date readDate() {
		/*
		 * Date: 0x0B T7 T6 .. T0 Z1 Z2 T7 to T0 form a 64 bit Big Endian number
		 * that specifies the number of nanoseconds that have passed since
		 * 1/1/1970 0:00 to the specified time. This format is “UTC 1970”. Z1 an
		 * Z0 for a 16 bit Big Endian number indicating the indicated time’s
		 * timezone in minutes.
		 */
		long ms = (long) buf.getDouble();
		short clientTimeZoneMins = buf.getShort();
		ms += clientTimeZoneMins * 60 * 1000;
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date(ms - TimeZone.getDefault().getRawOffset()));
		Date date = cal.getTime();
		if (cal.getTimeZone().inDaylightTime(date)) {
			date.setTime(date.getTime() - cal.getTimeZone().getDSTSavings());
		}
		return date;
	}

	// Array

	/**
	 * Returns an array
	 * 
	 * @return int
	 */
	public int readStartArray() {
		return buf.getInt();
	}

	/**
	 * Skips elements TODO
	 */
	public void skipElementSeparator() {
		// SKIP
	}

	/**
	 * Skips end array TODO
	 */
	public void skipEndArray() {
		// SKIP
	}

	// Object

	/**
	 * Reads start list
	 * 
	 * @return int
	 */
	public int readStartMap() {
		return buf.getInt();
	}

	/**
	 * Returns a boolean stating whether this has more items
	 * 
	 * @return boolean
	 */
	public boolean hasMoreItems() {
		return hasMoreProperties();
	}

	/**
	 * Reads the item index
	 * 
	 * @return int
	 */
	public String readItemKey() {
		return "";
	}

	/**
	 * Skips item seperator
	 */
	public void skipItemSeparator() {
		// SKIP
	}

	/**
	 * Skips end list
	 */
	public void skipEndMap() {
		skipEndObject();
	}

	// Object

	/**
	 * Reads start object
	 * 
	 * @return String
	 */
	public String readStartObject() {
		return null;
	}

	/**
	 * Returns a boolean stating whether there are more properties
	 * 
	 * @return boolean
	 */
	public boolean hasMoreProperties() {
		return false;
	}

	/**
	 * Reads property name
	 * 
	 * @return String
	 */
	public String readPropertyName() {
		return null;
	}

	/**
	 * Skips property seperator
	 */
	public void skipPropertySeparator() {
		// SKIP
	}

	/**
	 * Skips end object
	 */
	public void skipEndObject() {
		// skip two marker bytes
		// then end of object byte
		buf.skip(3);
		// byte nextType = buf.get();
	}

	// Others

	/**
	 * Reads xml
	 * 
	 * @return String
	 */
	public String readXML() {
		return readString();
	}

	/**
	 * Reads Custom
	 * 
	 * @return Object
	 */
	public Object readCustom() {
		// Return null for now
		return null;
	}

	/**
	 * Reads Reference
	 * 
	 * @return Object
	 */
	public Object readReference() {
		return getReference(buf.getShort());
	}

	/**
	 * Resets map
	 */
	public void reset() {
		this.clearReferences();
	}

	/**
	 * Parser of AMF3 "compressed" integer data type
	 * 
	 * @return a converted integer value
	 * @throws IOException
	 * @see <a href="http://osflash.org/amf3/parsing_integers">parsing AMF3
	 *      integers (external)</a>
	 */
	private int readAMF3Integer() {
		int n = 0;
		int b = buf.get();
		int result = 0;

		while ((b & 0x80) != 0 && n < 3) {
			result <<= 7;
			result |= (b & 0x7f);
			b = buf.get();
			n++;
		}
		if (n < 3) {
			result <<= 7;
			result |= b;
		} else {
			/* Use all 8 bits from the 4th byte */
			result <<= 8;
			result |= b;

			/* Check if the integer should be negative */
			if ((result & 0x10000000) != 0) {
				/* and extend the sign bit */
				result |= 0xe0000000;
			}
		}

		return result;
	}
}
