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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.utils.ObjectMap;

/**
 * Input for red5 data (AMF3) types
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Input extends org.red5.io.amf.Input implements org.red5.io.object.Input {

	/**
	 * Holds informations about already deserialized classes.
	 */
	protected class ClassReference {
		
		/** Name of the deserialized class. */
		protected String className;
		/** Names of the attributes of the class. */
		protected List<String> attributeNames;
		
		/** Create new informations about a class. */
		public ClassReference(String className, List<String> attributeNames) {
			this.className = className;
			this.attributeNames = attributeNames;
		}
	}
	
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(Input.class.getName());
	/**
	 * Set to a value above <tt>0</tt> to enforce AMF3 decoding mode.
	 */
	private int amf3_mode;
	/**
	 * List of string values found in the input stream.
	 */
	private List<String> stringReferences;
	/**
	 * Informations about already deserialized classes.
	 */
	private List<ClassReference> classReferences;

	/**
	 * Creates Input object for AMF3 from byte buffer
	 * 
	 * @param buf        Byte buffer
	 */
	public Input(ByteBuffer buf) {
		super(buf);
		amf3_mode = 0;
		stringReferences = new ArrayList<String>();
		classReferences = new ArrayList<ClassReference>();
	}

	/**
	 * Provide access to raw data.
	 * 
	 * @return ByteBuffer
	 */
	protected ByteBuffer getBuffer() {
		return buf;
	}
	
	/**
	 * Reads the data type
	 * 
	 * @return byte      Data type
	 */
	@Override
	public byte readDataType() {

		if (buf == null) {
			log.error("Why is buf null?");
		}

		currentDataType = buf.get();
		byte coreType;

		if (currentDataType == AMF.TYPE_AMF3_OBJECT) {
			currentDataType = buf.get();
		} else if (amf3_mode == 0) {
			// AMF0 object
			return readDataType(currentDataType);
		}

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
	 * Reads a null (value)
	 * 
	 * @return Object    null
	 */
	@Override
	public Object readNull() {
		return null;
	}

	/**
	 * Reads a boolean
	 * 
	 * @return boolean     Boolean value
	 */
	@Override
	public Boolean readBoolean() {
		return (currentDataType == AMF3.TYPE_BOOLEAN_TRUE) ? Boolean.TRUE
				: Boolean.FALSE;
	}

	/**
	 * Reads a Number
	 * 
	 * @return Number      Number
	 */
	@Override
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
	 * @return String       String
	 */
	@Override
	public String readString() {
		int len = readAMF3Integer();
		if (len == 1)
			// Empty string
			return "";
		
		if ((len & 1) == 0) {
			// Reference
			return stringReferences.get(len >> 1);
		}
		len >>= 1;
		int limit = buf.limit();
		final java.nio.ByteBuffer strBuf = buf.buf();
		strBuf.limit(strBuf.position() + len);
		final String string = AMF3.CHARSET.decode(strBuf).toString();
		buf.limit(limit); // Reset the limit
		stringReferences.add(string);
		return string;
	}

	/**
	 * Returns a date
	 * 
	 * @return Date        Date object
	 */
	@Override
	public Date readDate() {
		int ref = readAMF3Integer();
		if ((ref & 1) == 0) {
			// Reference to previously found date
			return (Date) getReference(ref >> 1);
		}
		
		long ms = (long) buf.getDouble();
		Date date = new Date(ms);
		storeReference(date);
		return date;
	}

	// Array

	/**
	 * Returns an array
	 * 
	 * @return int        Length of array
	 */
    public Object readArray(Deserializer deserializer) {
		int count = readAMF3Integer();
		if ((count & 1) == 0) {
			// Reference
			return getReference(count >> 1);
		}
		
		count = (count >> 1);
		String key = readString();
		amf3_mode += 1;
		Object result;
		if (key.equals("")) {
			// normal array
			List<Object> resultList = new ArrayList<Object>(count);
			storeReference(resultList);
			for (int i=0; i<count; i++) {
				final Object value = deserializer.deserialize(this);
				resultList.add(value);
			}
			result = resultList;
		} else {
			// associative array
			Map<Object, Object> resultMap = new HashMap<Object, Object>();
			storeReference(resultMap);
			while (!key.equals("")) {
				final Object value = deserializer.deserialize(this);
				resultMap.put(key, value);
				key = readString();
			}
			for (int i=0; i<count; i++) {
				final Object value = deserializer.deserialize(this);
				resultMap.put(i, value);
			}
			result = resultMap;
		}
		amf3_mode -= 1;
		return result;			
	}

    public Object readMap(Deserializer deserializer) {
    	throw new RuntimeException("AMF3 doesn't support maps.");
    }
    
	// Object

    public Object readObject(Deserializer deserializer) {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			// Reference
			return getReference(type >> 1);
		}
		
		type >>= 1;
		List<String> attributes = null;
		String className;
		Object result = null;
		boolean inlineClass = (type & 1) == 1;
		if (!inlineClass) {
			ClassReference info = classReferences.get(type >> 1);
			className = info.className;
			attributes = info.attributeNames;
			type = attributes.size() << 2 | AMF3.TYPE_OBJECT_PROPERTY;
		} else {
			type >>= 1;
			className = readString();
		}
		amf3_mode += 1;
		Map<String, Object> properties = null;
		switch (type & 0x03) {
		case AMF3.TYPE_OBJECT_PROPERTY:
			// Load object properties into map
			int count = type >> 2;
			properties = new ObjectMap<String, Object>();
			if (attributes == null) {
				attributes = new ArrayList<String>(count);
				for (int i=0; i<count; i++) {
					attributes.add(readString());					
				}
				classReferences.add(new ClassReference(className, attributes));
			}
			for (int i=0; i<count; i++) {
				properties.put(attributes.get(i), deserializer.deserialize(this));
			}
			break;
		case AMF3.TYPE_OBJECT_EXTERNALIZABLE:
			// Use custom class to deserialize the object
			if ("".equals(className))
				throw new RuntimeException("need a classname to load an externalizable object");
			
			result = newInstance(className);
			if (result == null)
				throw new RuntimeException("could not instantiate class");
			
			if (!(result instanceof IExternalizable))
				throw new RuntimeException("the class must implement the IExternalizable interface");
			
			((IExternalizable) result).readExternal(new DataInput(this, deserializer));
			break;
		case AMF3.TYPE_OBJECT_VALUE:
			// Load object properties into map
			properties = new ObjectMap<String, Object>();
			attributes = new LinkedList<String>();
			String key = readString();
			while (!"".equals(key)) {
				attributes.add(key);
				Object value = deserializer.deserialize(this);
				properties.put(key, value);
				key = readString();
			}
			classReferences.add(new ClassReference(className, attributes));
			break;
		default:
		case AMF3.TYPE_OBJECT_UNKNOWN:
			throw new RuntimeException("Unknown object type: " + (type & 0x03));
		}
		amf3_mode -= 1;
		
		if (result == null) {
			// Create result object based on classname
			if ("".equals(className)) {
				// "anonymous" object, load as Map
				storeReference(properties);
				result = properties;
			} else if ("RecordSet".equals(className)) {
				// TODO: how are RecordSet objects encoded?
				throw new RuntimeException("Objects of type " + className + " not supported yet.");
			} else if ("RecordSetPage".equals(className)) {
				// TODO: how are RecordSetPage objects encoded?
				throw new RuntimeException("Objects of type " + className + " not supported yet.");
			} else {
				// Apply properties to object
				result = newInstance(className);
				if (result != null) {
					storeReference(properties);
					Class resultClass = result.getClass();
					for (Map.Entry<String, Object> entry: properties.entrySet()) {
						try {
							try {
								resultClass.getField(entry.getKey()).set(result, entry.getValue());
							} catch (Exception e) {
								BeanUtils.setProperty(result, entry.getKey(), entry.getValue());
							}
						} catch (Exception e) {
							log.error("Error mapping property: " + entry.getKey() + " (" + entry.getValue() + ")");
						}
					}
				} // else fall through
			}
		}
		return result;
    }
    
	// Others

	/**
	 * Reads Custom
	 * 
	 * @return Object     Custom type object
	 */
	@Override
	public Object readCustom() {
		// Return null for now
		return null;
	}

	/** {@inheritDoc} */
	public Object readReference() {
		throw new RuntimeException("AMF3 doesn't support direct references.");
	}

	/**
	 * Resets map
	 */
	@Override
	public void reset() {
		super.reset();
		stringReferences.clear();
	}

	/**
	 * Parser of AMF3 "compressed" integer data type
	 * 
	 * @return a converted integer value
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
