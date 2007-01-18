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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.utils.XMLUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.*;

/**
 * The Deserializer class reads data input and handles the data 
 * according to the core data types 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Deserializer {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(Deserializer.class.getName());

	/**
	 * Deserializes the input parameter and returns an Object
	 * which must then be cast to a core data type
	 * @param in
	 * @return Object
	 */
	public Object deserialize(Input in) {

		byte type = in.readDataType();

		while (type == DataTypes.CORE_SKIP) {
			type = in.readDataType();
		}

		if (log.isDebugEnabled()) {
			log.debug("Datatype: " + DataTypes.toStringValue(type));
		}

		Object result;

		switch (type) {
			case DataTypes.CORE_NULL:
				result = in.readNull();
				break;
			case DataTypes.CORE_BOOLEAN:
				result = in.readBoolean();
				break;
			case DataTypes.CORE_NUMBER:
				result = in.readNumber();
				break;
			case DataTypes.CORE_STRING:
				result = in.readString();
				break;
			case DataTypes.CORE_DATE:
				result = in.readDate();
				break;
			case DataTypes.CORE_ARRAY:
				result = readArray(in);
				break;
			case DataTypes.CORE_MAP:
				result = readMixedArray(in);
				break;
			case DataTypes.CORE_XML:
				result = readXML(in);
				break;
			case DataTypes.CORE_OBJECT:
				result = readObject(in);
				break;
			case DataTypes.OPT_REFERENCE:
				result = readReference(in);
				break;
			default:
				result = in.readCustom();
				break;
		}

		if (type >= DataTypes.CORE_OBJECT) {
			result = postProcessExtension(result);
		}

		return result;
	}

	/**
	 * Reads the input and returns an array of Objects
	 * 
	 * @param in
	 * @return ArrayList
	 */
	protected ArrayList readArray(Input in) {
		if (log.isDebugEnabled()) {
			log.debug("Read array");
		}
		final int arraySize = in.readStartArray();
		ArrayList list = new ArrayList(arraySize);
		in.storeReference(list);
		for (int i = 0; i < arraySize; i++) {
			list.add(deserialize(in));
			in.skipElementSeparator();
		}
		in.skipEndArray();
		return list;
	}

	/**
	 * Reads the input and returns a List or Map depending on
	 * the mixed array's keys.
	 * 
	 * @param in
	 * @return List or Map
	 */
	protected Object readMixedArray(Input in) {
		if (log.isDebugEnabled()) {
			log.debug("read mixed array");
		}

		// The maximum number used in this mixed array.
		int maxNumber = in.readStartMap();
		if (log.isDebugEnabled()) {
			log.debug("Read start mixed array: " + maxNumber);
		}

		boolean allNumbers = true;
		Object result;
		final Map<Object, Object> mixedResult = new LinkedHashMap<Object, Object>(maxNumber);
		while (in.hasMoreItems()) {
			String key = in.readItemKey();
			if (log.isDebugEnabled()) {
				log.debug("key: " + key);
			}
			Object item = deserialize(in);
			if (log.isDebugEnabled()) {
				log.debug("item: " + item);
			}
			try {
				mixedResult.put(Integer.parseInt(key), item);
			} catch (NumberFormatException err) {
				mixedResult.put(key, item);
				allNumbers = false;
			}
			if (in.hasMoreItems()) {
				in.skipItemSeparator();
			}
		}
		
		if (allNumbers) {
			// MixedArray actually is a regular array
			if (log.isDebugEnabled()) {
				log.debug("mixed array is a regular array");
			}
			final List<Object> listResult = new ArrayList<Object>(maxNumber);
			for (int i=0; i<maxNumber; i++) {
				listResult.add(i, mixedResult.get(i));
			}
			result = listResult;
		} else
			result = mixedResult;
		
		in.storeReference(result);
		in.skipEndMap();
		return result;
	}

	/**
	 * Reads the input as xml and returns an object
	 * 
	 * @param in
	 * @return the xml document
	 */
	protected Object readXML(Input in) {
		final String xmlString = in.readString();
		Document doc = null;
		try {
			doc = XMLUtils.stringToDoc(xmlString);
		} catch (IOException ioex) {
			log.error("IOException converting xml to dom", ioex);
		}
		in.storeReference(doc);
		return doc;
	}

	/**
	 * Reads the input as and object and returns an Object
	 * 
	 * @param in
	 * @return Object
	 */
	protected Object readObject(Input in) {
		if (log.isDebugEnabled()) {
			log.debug("read object");
		}
		final String className = in.readStartObject();
		if (className != null) {
			if (log.isDebugEnabled()) {
				log.debug("read class object");
			}
			Object instance;
			if (className.equals("RecordSet")) {
				return new RecordSet(in);
			} else if (className.equals("RecordSetPage")) {
				return new RecordSetPage(in);
			} else {
				instance = newInstance(className);
				if (instance != null) {
					return readBean(in, instance);
				} // else fall through
			}
		}
		return readSimpleObject(in);
	}

	/**
	 * Reads the input as a bean and returns an object
	 * 
	 * @param in
	 * @param bean
	 * @return Object
	 */
	protected Object readBean(Input in, Object bean) {
		if (log.isDebugEnabled()) {
			log.debug("read bean");
		}
		in.storeReference(bean);
		while (in.hasMoreProperties()) {
			String name = in.readPropertyName();
			if (log.isDebugEnabled()) {
				log.debug("property: " + name);
			}
			Object property = deserialize(in);
			if (log.isDebugEnabled()) {
				log.debug("val: " + property);
			}
			//log.debug("val: "+property.getClass().getName());
			try {
				if (property != null) {
					BeanUtils.setProperty(bean, name, property);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Skipping null property: " + name);
					}
				}
			} catch (Exception ex) {
				log.error("Error mapping property: " + name);
			}
			if (in.hasMoreProperties()) {
				in.skipPropertySeparator();
			}
		}
		in.skipEndObject();
		return bean;
	}

	/**
	 * Reads the input as a map and returns a Map
	 * 
	 * @param in
	 * @return Map
	 */
	protected Map readSimpleObject(Input in) {
		if (log.isDebugEnabled()) {
			log.debug("read map");
		}
		Map map = new HashMap();
		in.storeReference(map);
		while (in.hasMoreProperties()) {
			String name = in.readPropertyName();
			if (log.isDebugEnabled()) {
				log.debug("property: " + name);
			}
			Object property = deserialize(in);
			if (log.isDebugEnabled()) {
				log.debug("val: " + property);
			}
			//log.debug("val: "+property.getClass().getName());
			map.put(name, property);
			if (in.hasMoreProperties()) {
				in.skipPropertySeparator();
			}
		}
		in.skipEndObject();
		return map;
	}

	/**
	 * Creats a new instance of the className parameter and 
	 * returns as an Object
	 * @param className
	 * @return Object
	 */
	protected Object newInstance(String className) {
		Object instance = null;
		try {
			Class clazz = Thread.currentThread().getContextClassLoader()
					.loadClass(className);
			instance = clazz.newInstance();
		} catch (Exception ex) {
			log.error("Error loading class: " + className, ex);
		}
		return instance;
	}

	/**
	 * Reads the input as a reference and returns an Object
	 * 
	 * @param in
	 * @return Object
	 */
	protected Object readReference(Input in) {
		final Object ref = in.readReference();
		if (ref == null) {
			log.error("Reference returned by input is null");
		}
		return ref;
	}

	/**
	 * Post processes the result
	 * TODO Extension Point
     * @param result
     * @return
     */
	protected Object postProcessExtension(Object result) {
		// does nothing at the moment, but will later!
		return result;
	}

}
