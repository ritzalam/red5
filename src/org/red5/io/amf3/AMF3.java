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

import java.nio.charset.Charset;

/**
 * AMF3 data type definitions.<br />
 * For detailed specification please see <strong>specification AMF3</strong>
 * link below.
 *
 * @see org.red5.io.amf.AMF
 * @see <a href="http://osflash.org/amf3/index">specifictation AMF3 (external)</a>
 * @see <a href="http://osflash.org/amf/astypes">specifictation AMF (external)</a>
 */
public class AMF3 {

	public static final Charset CHARSET = Charset.forName("UTF-8");

	public static final int LONG_STRING_LENGTH = 65535;

	public final static byte TYPE_NULL = 0x01;

	public final static byte TYPE_BOOLEAN_TRUE = 0x02;

	public final static byte TYPE_BOOLEAN_FALSE = 0x03;

	public final static byte TYPE_INTEGER = 0x04;

	public final static byte TYPE_NUMBER = 0x05;

	public final static byte TYPE_STRING = 0x06;

	// TODO m.j.m hm..not defined on site, says it's only XML type, so i'll
	// assume it is for the time being..
	public final static byte TYPE_XML_SPECIAL = 0x07;

	public final static byte TYPE_DATE = 0x08;

	public final static byte TYPE_ARRAY = 0x09;

	public final static byte TYPE_OBJECT = 0x0A;

	public final static byte TYPE_XML = 0x0B;

	//public final static byte TYPE_YYY = 0x0C;
	//public final static byte TYPE_ZZZ = 0x0D;

	/**
	 * 00 = property list encoding.<br />
	 * The remaining integer-data represents the number of class members that exist.<br />
	 * The property names are read as string-data.<br />
	 * The values are then read as amf3-data.
	 */
	public final static byte TYPE_OBJECT_PROPERTY = 0x00;

	/**
	 * 01 = single anonymous property. <br />
	 * A single amf3-data contains a single value. <br />
	 * The property name should be declared in the class-def reference but this doesn’t seem <b />
	 * to always be the case - more to come here.
	 */
	public final static byte TYPE_OBJECT_ANONYMOUS_PROPERTY = 0x01;

	/**
	 * 10 = name value encoding.<br />
	 * The property names and values are encoded as string-data followed by amf3-data <br />
	 * until there is an empty string property name.<br />
	 * If there is a class-def reference there are no property names and the number of values <br />
	 * is equal to the number of properties in the class-def.
	 */
	public final static byte TYPE_OBJECT_VALUE = 0x0A;

	/**
	 * 11 = unseen / unknown
	 */
	public final static byte TYPE_OBJECT_UNKNOWN = 0x0B;

}
