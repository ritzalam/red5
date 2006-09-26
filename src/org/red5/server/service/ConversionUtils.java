package org.red5.server.service;

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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.Deserializer;
import org.red5.server.api.IConnection;

/**
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class ConversionUtils {

	protected static Log log = LogFactory.getLog(Deserializer.class.getName());

	private static final Class[] PRIMITIVES = { boolean.class, byte.class,
			char.class, short.class, int.class, long.class, float.class,
			double.class };

	private static final Class[] WRAPPERS = { Boolean.class, Byte.class,
			Character.class, Short.class, Integer.class, Long.class,
			Float.class, Double.class };

	private static final Class[][] PARAMETER_CHAINS = {
			{ boolean.class, null }, { byte.class, Short.class },
			{ char.class, Integer.class }, { short.class, Integer.class },
			{ int.class, Long.class }, { long.class, Float.class },
			{ float.class, Double.class }, { double.class, null } };

	/** Mapping of primitives to wrappers */
	private static Map<Class, Class> primitiveMap = new HashMap<Class, Class>();

	/** Mapping of wrappers to primitives */
	private static Map<Class, Class> wrapperMap = new HashMap<Class, Class>();

	/** 
	 * Mapping from wrapper class to appropriate parameter types (in order) 
	 * Each entry is an array of Classes, the last of which is either null
	 * (for no chaining) or the next class to try
	 */
	private static Map<Class, Class[]> parameterMap = new HashMap<Class, Class[]>();

	/** Default number format */
	private static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

	static {
		for (int i = 0; i < PRIMITIVES.length; i++) {
			primitiveMap.put(PRIMITIVES[i], WRAPPERS[i]);
			wrapperMap.put(WRAPPERS[i], PRIMITIVES[i]);
			parameterMap.put(WRAPPERS[i], PARAMETER_CHAINS[i]);
		}
	}

	public static Object convert(Object source, Class target)
			throws ConversionException {
		if (target == null) {
			throw new ConversionException("Unable to perform conversion");
		}
		if (source == null) {
			if (target.isPrimitive()) {
				throw new ConversionException(
						"Unable to convert null to primitive value");
			}
			return source;
		}
		if (IConnection.class.isAssignableFrom(source.getClass())
				&& !target.equals(IConnection.class)) {
			throw new ConversionException("IConnection must match exact.");
		}
		if (target.isInstance(source)) {
			return source;
		}
		if (target.isAssignableFrom(source.getClass())) {
			return source;
		}
		if (target.isArray()) {
			return convertToArray(source, target);
		}
		if (target.equals(String.class)) {
			return source.toString();
		}
		if (target.isPrimitive()) {
			return convertToWrappedPrimitive(source, primitiveMap.get(target));
		}
		if (wrapperMap.containsKey(target)) {
			return convertToWrappedPrimitive(source, target);
		}
		if (target.equals(Map.class)) {
			return convertBeanToMap(source);
		}
		if ((target.equals(List.class) || target.equals(Collection.class))
				&& source.getClass().isArray()) {
			return convertArrayToList((Object[]) source);
		}
		if (target.equals(Set.class) && source.getClass().isArray()) {
			return convertArrayToSet((Object[]) source);
		}
		throw new ConversionException("Unable to preform conversion");
	}

	public static Object convertToArray(Object source, Class target)
			throws ConversionException {
		try {
			Object[] targetInstance = (Object[]) Array.newInstance(target
					.getComponentType(), 0);
			if (source.getClass().isArray()) {
				Object[] sourceArray = (Object[]) source;
				Class targetType = target.getComponentType();
				ArrayList list = new ArrayList(sourceArray.length);
				for (Object element : sourceArray) {
					list.add(convert(element, targetType));
				}
				source = list;
			}
			if (source instanceof Collection) {
				return ((Collection) source).toArray(targetInstance);
			} else {
				throw new ConversionException("Unable to convert to array");
			}
		} catch (Exception ex) {
			throw new ConversionException("Error converting to array", ex);
		}
	}

	public static Object convertToWrappedPrimitive(Object source, Class wrapper) {
		if (source == null || wrapper == null) {
			return null;
		}
		if (wrapper.isInstance(source)) {
			return source;
		}
		if (wrapper.isAssignableFrom(source.getClass())) {
			return source;
		}
		if (source instanceof Number) {
			return convertNumberToWrapper((Number) source, wrapper);
		} else {
			return convertStringToWrapper(source.toString(), wrapper);
		}
	}

	public static Object convertStringToWrapper(String str, Class wrapper) {
		if (wrapper.equals(String.class)) {
			return str;
		} else if (wrapper.equals(Boolean.class)) {
			return new Boolean(str);
		} else if (wrapper.equals(Double.class)) {
			return new Double(str);
		} else if (wrapper.equals(Long.class)) {
			return new Long(str);
		} else if (wrapper.equals(Float.class)) {
			return new Float(str);
		} else if (wrapper.equals(Integer.class)) {
			return new Integer(str);
		} else if (wrapper.equals(Short.class)) {
			return new Short(str);
		} else if (wrapper.equals(Byte.class)) {
			return new Byte(str);
		}
		throw new ConversionException("Unable to convert string to: " + wrapper);
	}

	public static Object convertNumberToWrapper(Number num, Class wrapper) {
		if (wrapper.equals(String.class)) {
			return num.toString();
		} else if (wrapper.equals(Boolean.class)) {
			return new Boolean(num.intValue() == 1);
		} else if (wrapper.equals(Double.class)) {
			return new Double(num.doubleValue());
		} else if (wrapper.equals(Long.class)) {
			return new Long(num.longValue());
		} else if (wrapper.equals(Float.class)) {
			return new Float(num.floatValue());
		} else if (wrapper.equals(Integer.class)) {
			return new Integer(num.intValue());
		} else if (wrapper.equals(Short.class)) {
			return new Short(num.shortValue());
		} else if (wrapper.equals(Byte.class)) {
			return new Byte(num.byteValue());
		}
		throw new ConversionException("Unable to convert number to: " + wrapper);
	}

	public static List findMethodsByNameAndNumParams(Object object,
			String method, int numParam) {
		LinkedList list = new LinkedList();
		Method[] methods = object.getClass().getMethods();
		for (Method m : methods) {
			log.debug("Method name: " + m.getName());
			if (!m.getName().equals(method)) {
				log.debug("Method name not the same");
				continue;
			}
			if (m.getParameterTypes().length != numParam) {
				log.debug("Param length not the same");
				continue;
			}
			list.add(m);
		}
		return list;
	}

	public static Object[] convertParams(Object[] source, Class[] target)
			throws ConversionException {
		Object[] converted = new Object[target.length];
		for (int i = 0; i < target.length; i++) {
			converted[i] = convert(source[i], target[i]);
		}
		return converted;
	}

	public static List convertArrayToList(Object[] source)
			throws ConversionException {
		ArrayList list = new ArrayList(source.length);
		for (Object element : source) {
			list.add(element);
		}
		return list;
	}

	public static Object convertMapToBean(Map source, Class target)
			throws ConversionException {
		Object bean = newInstance(target.getClass().getName());
		if (bean == null) {
			throw new ConversionException(
					"Unable to create bean using empty constructor");
		}
		try {
			BeanUtils.populate(bean, source);
		} catch (Exception e) {
			throw new ConversionException("Error populating bean", e);
		}
		return bean;
	}

	public static Map convertBeanToMap(Object source) {
		return new BeanMap(source);
	}

	public static Set convertArrayToSet(Object[] source) {
		HashSet set = new HashSet();
		for (Object element : source) {
			set.add(element);
		}
		return set;
	}

	protected static Object newInstance(String className) {
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

}
