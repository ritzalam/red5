package org.red5.io.amf;

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


import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.beanutils.BeanMap;
import org.apache.mina.common.ByteBuffer;
import org.red5.annotations.Anonymous;
import org.red5.io.amf3.ByteArray;
import org.red5.io.object.BaseOutput;
import org.red5.io.object.ICustomSerializable;
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Harald Radi (harald.radi@nme.at)
 */
public class Output extends BaseOutput implements org.red5.io.object.Output {

	protected static Logger log = LoggerFactory.getLogger(Output.class);

	/**
	 * Cache encoded strings.
	 */
	protected static final ConcurrentMap<String, byte[]> stringCache = new ConcurrentHashMap<String, byte[]>();
	protected static final Map<Class<?>, Map<String, Boolean>> serializeCache = new HashMap<Class<?>, Map<String, Boolean>>();
	protected static final Map<Class<?>, Map<String, Field>> fieldCache = new HashMap<Class<?>, Map<String, Field>>();
	protected static final Map<Class<?>, Map<String, Method>> getterCache = new HashMap<Class<?>, Map<String, Method>>();

    /**
     * Output buffer
     */
    protected ByteBuffer buf;

    /**
     * Creates output with given byte buffer
     * @param buf         Bute buffer
     */
    public Output(ByteBuffer buf) {
		super();
		this.buf = buf;
	}

	/** {@inheritDoc} */
    public boolean isCustom(Object custom) {
		return false;
	}

    protected boolean checkWriteReference(Object obj) {
    	if (hasReference(obj)) {
    		writeReference(obj);
    		return true;
    	} else
    		return false;
    }

	/** {@inheritDoc} */
	public void writeArray(Collection<?> array, Serializer serializer) {
		if (checkWriteReference(array)) {
			return;
		}
		storeReference(array);
		buf.put(AMF.TYPE_ARRAY);
		buf.putInt(array.size());
		for (Object item : array) {
			serializer.serialize(this, item);
		}
	}

	/** {@inheritDoc} */
	public void writeArray(Object[] array, Serializer serializer) {
		log.debug("writeArray - array: {} serializer: {}", array, serializer);
    	if (array != null) {		
    		if (checkWriteReference(array)) {
    			return;
    		}
    		storeReference(array);
    		buf.put(AMF.TYPE_ARRAY);
    		buf.putInt(array.length);
    		for (Object item : array) {
    			serializer.serialize(this, item);
    		}
    	} else {
    		writeNull();
    	}		
	}

	/** {@inheritDoc} */
    public void writeArray(Object array, Serializer serializer) {
    	if (array != null) {
    		if (checkWriteReference(array)) {
    			return;
    		}
    		storeReference(array);
    		buf.put(AMF.TYPE_ARRAY);
    		buf.putInt(Array.getLength(array));
    		for (int i=0; i<Array.getLength(array); i++) {
    			serializer.serialize(this, Array.get(array, i));
    		}
    	} else {
    		writeNull();
    	}
    }

	/** {@inheritDoc} */
	public void writeMap(Map<Object, Object> map, Serializer serializer) {
		if (checkWriteReference(map)) {
			return;
		}
		storeReference(map);
		buf.put(AMF.TYPE_MIXED_ARRAY);
		int maxInt=-1;
		for (int i=0; i<map.size(); i++) {
			try {
				if (!map.containsKey(i))
					break;
			} catch (ClassCastException err) {
				// Map has non-number keys.
				break;
			}

			maxInt = i;
		}
		buf.putInt(maxInt+1);
		// TODO: Need to support an incoming key named length
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			final String key = entry.getKey().toString();
			if ("length".equals(key)) {
				continue;
			}
			putString(key);
			serializer.serialize(this, entry.getValue());
		}
		if (maxInt >= 0) {
			putString("length");
			serializer.serialize(this, maxInt+1);
		}
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}

	/** {@inheritDoc} */
	public void writeMap(Collection<?> array, Serializer serializer) {
		if (checkWriteReference(array)) {
			return;
		}
		storeReference(array);
		buf.put(AMF.TYPE_MIXED_ARRAY);
		buf.putInt(array.size()+1);
		int idx=0;
		for (Object item: array) {
			if (item != null) {
				putString(String.valueOf(idx++));
				serializer.serialize(this, item);
			} else {
				idx++;
			}
		}
		putString("length");
		serializer.serialize(this, array.size()+1);

		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}

	/** {@inheritDoc} */
    public void writeRecordSet(RecordSet recordset, Serializer serializer) {
		if (checkWriteReference(recordset)) {
			return;
		}
		storeReference(recordset);
        // Write out start of object marker
		buf.put(AMF.TYPE_CLASS_OBJECT);
		putString("RecordSet");
        // Serialize
        Map<String, Object> info = recordset.serialize();
        // Write out serverInfo key
        putString("serverInfo");
        // Serialize
        serializer.serialize(this, info);
        // Write out end of object marker
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
    }

	/** {@inheritDoc} */
    public boolean supportsDataType(byte type) {
		return false;
	}

	/** {@inheritDoc} */
	public void writeBoolean(Boolean bol) {
		buf.put(AMF.TYPE_BOOLEAN);
		buf.put(bol ? AMF.VALUE_TRUE : AMF.VALUE_FALSE);
	}

	/** {@inheritDoc} */
    public void writeCustom(Object custom) {

	}

	/** {@inheritDoc} */
    public void writeDate(Date date) {
		buf.put(AMF.TYPE_DATE);
		buf.putDouble(date.getTime());
		buf.putShort((short) (TimeZone.getDefault().getRawOffset() / 60 / 1000));
	}

	/** {@inheritDoc} */
	public void writeNull() {
		// System.err.println("Write null");
		buf.put(AMF.TYPE_NULL);
	}

	/** {@inheritDoc} */
	public void writeNumber(Number num) {
		buf.put(AMF.TYPE_NUMBER);
		buf.putDouble(num.doubleValue());
	}

	/** {@inheritDoc} */
    public void writeReference(Object obj) {
		log.debug("Write reference");
		buf.put(AMF.TYPE_REFERENCE);
		buf.putShort(getReferenceId(obj));
	}

	/** {@inheritDoc} */
    @SuppressWarnings("unchecked")
	public void writeObject(Object object, Serializer serializer) {
		if (checkWriteReference(object)) {
			return;
		}
		storeReference(object);
        // Create new map out of bean properties
        BeanMap beanMap = new BeanMap(object);
        // Set of bean attributes
        Set set = beanMap.keySet();
		if ((set.size() == 0) || (set.size() == 1 && beanMap.containsKey("class"))) {
			// BeanMap is empty or can only access "class" attribute, skip it
			writeArbitraryObject(object, serializer);
			return;
		}

        // Write out either start of object marker for class name or "empty" start of object marker
		Class<?> objectClass = object.getClass();
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
			buf.put(AMF.TYPE_CLASS_OBJECT);
			putString(buf, objectClass.getName());
		} else {
			buf.put(AMF.TYPE_OBJECT);
		}

        if (object instanceof ICustomSerializable) {
        	((ICustomSerializable) object).serialize(this, serializer);
    		buf.put((byte) 0x00);
    		buf.put((byte) 0x00);
    		buf.put(AMF.TYPE_END_OF_OBJECT);
    		return;
        }

        // Iterate thru entries and write out property names with separators
		for (Object key : set) {
            String fieldName = key.toString();
            log.debug("Field name: {} class: {}", fieldName, objectClass);
            
            Field field = getField(objectClass, fieldName);
            Method getter = getGetter(objectClass, fieldName);

            // Check if the Field corresponding to the getter/setter pair is transient
            if (!serializeField(serializer, objectClass, fieldName, field, getter)) {
            	continue;
            }

            putString(buf, fieldName);
			serializer.serialize(this, field, getter, beanMap.get(key));
		}
        // Write out end of object mark
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}

	protected boolean serializeField(Serializer serializer, Class<?> objectClass, String keyName, Field field, Method getter) {
		Map<String, Boolean> serializeMap = serializeCache.get(objectClass);
		if (serializeMap == null) {
			serializeMap = new HashMap<String, Boolean>();
			serializeCache.put(objectClass, serializeMap);
		}

		Boolean serialize;
		if (serializeCache.containsKey(keyName)) {
			serialize = serializeMap.get(keyName);
		} else {
			serialize = serializer.serializeField(keyName, field, getter);
			serializeMap.put(keyName, serialize);
		}

		return serialize;
	}

	protected Field getField(Class<?> objectClass, String keyName) {
	    Map<String, Field> fieldMap = fieldCache.get(objectClass);
	    if (fieldMap == null) {
		    fieldMap = new HashMap<String, Field>();
		    fieldCache.put(objectClass, fieldMap);
	    }

	    Field field = null;

	    if (fieldMap.containsKey(keyName)) {
		    field = fieldMap.get(keyName);
	    } else {
		    for (Class<?> clazz = objectClass; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
			    try {
				    field = clazz.getDeclaredField(keyName);
			    } catch (NoSuchFieldException nfe) {
				    // Ignore this exception and use the default behavior
				    log.debug("writeObject caught NoSuchFieldException", nfe);
			    }
		    }

		    fieldMap.put(keyName, field);
	    }

	    return field;
    }

	protected Method getGetter(Class<?> objectClass, String keyName) {

		Map<String, Method> getterMap = getterCache.get(objectClass);
		if (getterMap == null) {
			getterMap = new HashMap<String, Method>();
			getterCache.put(objectClass, getterMap);
		}

		Method getter = null;

		if (getterMap.containsKey(keyName)) {
			getter = getterMap.get(keyName);
		} else {
			String upper = keyName.substring(0, 1).toUpperCase() + keyName.substring(1);

			for (Class<?> clazz = objectClass; !clazz.equals(Object.class); clazz = clazz.getSuperclass()) {
				try {
					getter = clazz.getMethod("get" + upper);
				} catch (NoSuchMethodException e1) {
					try {
						getter = clazz.getMethod("is" + upper);
					} catch (NoSuchMethodException e2) {
						// Ignore this exception and use the default behavior
						log.debug("writeObject caught NoSuchMethodException", e2);
					}
				}
			}

			getterMap.put(keyName, getter);
		}

		return getter;
	}


    /** {@inheritDoc} */
    public void writeObject(Map<Object, Object> map, Serializer serializer) {
		if (checkWriteReference(map)) {
			return;
		}
		storeReference(map);
		buf.put(AMF.TYPE_OBJECT);
		boolean isBeanMap = (map instanceof BeanMap);
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			if (isBeanMap && "class".equals(entry.getKey())) {
				continue;
			}
			putString(entry.getKey().toString());
			serializer.serialize(this, entry.getValue());
		}
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
    }

	/**
	 * Writes an arbitrary object to the output.
	 *
	 * @param serializer    Output writer
	 * @param object        Object to write
	 */
	protected void writeArbitraryObject(Object object, Serializer serializer) {
			log.debug("writeObject");
        // If we need to serialize class information...
		Class<?> objectClass = object.getClass();
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
            // Write out start object marker for class name
			buf.put(AMF.TYPE_CLASS_OBJECT);
			putString(buf, objectClass.getName());
		} else {
            // Write out start object marker without class name
			buf.put(AMF.TYPE_OBJECT);
		}

        // Iterate thru fields of an object to build "name-value" map from it
        for (Field field : objectClass.getFields()) {
	        String fieldName = field.getName();

        	log.debug("Field: {} class: {}", field, objectClass);
            // Check if the Field corresponding to the getter/setter pair is transient
	        if (!serializeField(serializer, objectClass, fieldName, field, null)) {
            	continue;
            }

			Object value;
			try {
                // Get field value
                value = field.get(object);
			} catch (IllegalAccessException err) {
                // Swallow on private and protected properties access exception
                continue;
			}
            // Write out prop name
	        putString(buf, fieldName);
            // Write out
            serializer.serialize(this, field, null, value);
		}
        // Write out end of object marker
		buf.put((byte) 0x00);
		buf.put((byte) 0x00);
		buf.put(AMF.TYPE_END_OF_OBJECT);
	}


	/** {@inheritDoc} */
    public void writeString(String string) {
    	final byte[] encoded = encodeString(string);
		final int len = encoded.length;
		if (len < AMF.LONG_STRING_LENGTH) {
			buf.put(AMF.TYPE_STRING);
			buf.putShort((short) len);
		} else {
			buf.put(AMF.TYPE_LONG_STRING);
			buf.putInt(len);
		}
		buf.put(encoded);
	}

    /** {@inheritDoc} */
    public void writeByteArray(ByteArray array) {
		throw new RuntimeException("ByteArray objects not supported with AMF0");
    }

    /**
     * Encode string.
     *
     * @param string
     * @return encoded string
     */
    protected static byte[] encodeString(String string) {
    	byte[] encoded = stringCache.get(string);
		if (encoded == null) {
    		java.nio.ByteBuffer buf = AMF.CHARSET.encode(string);
    		encoded = new byte[buf.limit()];
    		buf.get(encoded);
    		stringCache.put(string, encoded);
    	}		
    	return encoded;
    }

    /**
     * Write out string
     * @param buf         Byte buffer to write to
     * @param string      String to write
     */
    public static void putString(ByteBuffer buf, String string) {
    	final byte[] encoded = encodeString(string);
		buf.putShort((short) encoded.length);
		buf.put(encoded);
	}

    /** {@inheritDoc} */
	public void putString(String string) {
		putString(buf, string);
	}

    /** {@inheritDoc} */
	public void writeXML(Document xml) {
		buf.put(AMF.TYPE_XML);
		putString(XMLUtils.docToString(xml));
	}

	/**
	 * Convenience method to allow XML text to be used, instead
	 * of requiring an XML Document.
	 * 
	 * @param xml
	 */
	public void writeXML(String xml) {
		buf.put(AMF.TYPE_XML);
		putString(xml);
	}	
	
    /**
     * Return buffer of this Output object
     * @return        Byte buffer of this Output object
     */
    public ByteBuffer buf() {
		return this.buf;
	}

    public void reset() {
    	clearReferences();
    }

}