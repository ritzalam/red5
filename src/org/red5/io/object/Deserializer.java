package org.red5.io.object;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * The Deserializer class reads data input and handles the data
 * according to the core data types
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Deserializer {

	// Initialize Logging
	private static final Logger log = LoggerFactory.getLogger(Deserializer.class);

	/**
	 * Deserializes the input parameter and returns an Object
	 * which must then be cast to a core data type
	 * @param <T> type
	 * @param in input
	 * @param target target
     * @return Object object
	 */
	@SuppressWarnings("unchecked")
	public <T> T deserialize(Input in, Type target) {

		byte type = in.readDataType();
		log.debug("Type: {} target: {}", type, (target != null ? target.toString() : ""));

		while (type == DataTypes.CORE_SKIP) {
			type = in.readDataType();
			log.debug("Type (skip): {}", type);
		}

		log.debug("Datatype: {}", DataTypes.toStringValue(type));

		Object result;

		switch (type) {
			case DataTypes.CORE_NULL:
				result = in.readNull(target);
				break;
			case DataTypes.CORE_BOOLEAN:
				result = in.readBoolean(target);
				break;
			case DataTypes.CORE_NUMBER:
				result = in.readNumber(target);
				break;
			case DataTypes.CORE_STRING:
				result = in.readString(target);
				break;
			case DataTypes.CORE_DATE:
				result = in.readDate(target);
				break;
			case DataTypes.CORE_ARRAY:
				result = in.readArray(this, target);
				break;
			case DataTypes.CORE_MAP:
				result = in.readMap(this, target);
				break;
			case DataTypes.CORE_XML:
				result = in.readXML(target);
				break;
			case DataTypes.CORE_OBJECT:
				result = in.readObject(this, target);
				break;
			case DataTypes.CORE_BYTEARRAY:
				result = in.readByteArray(target);
				break;
			case DataTypes.OPT_REFERENCE:
				result = in.readReference(target);
				break;
			default:
				result = in.readCustom(target);
				break;
		}

        return (T) postProcessExtension(result, target);
	}

	/**
	 * Post processes the result
	 * TODO Extension Point
     * @param result result
     * @param target target
     * @return object
     */
	protected Object postProcessExtension(Object result, Type target) {
		// does nothing at the moment, but will later!
		return result;
	}

}
