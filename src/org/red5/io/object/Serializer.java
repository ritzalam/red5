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

import org.apache.commons.collections.BeanMap;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.utils.XMLUtils;
import org.w3c.dom.Document;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * The Serializer class writes data output and handles the data according to the
 * core data types
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Serializer implements SerializerOpts {

    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(Serializer.class.getName());

	/**
	 * Serializes output to a core data type object
	 * 
	 * @param out          Output writer
	 * @param any          Object to serialize
	 */
	public void serialize(Output out, Object any) {
		if (log.isDebugEnabled()) {
			log.debug("serialize");
		}
		if (writeBasic(out, any)) {
			if (log.isDebugEnabled()) {
				log.debug("write basic");
			}
			return;
		}

		// Extension point
		if (out.hasReference(any)) {
			if (log.isDebugEnabled()) {
				log.debug("write ref");
			}
            // Write referemce
            out.writeReference(any);
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("Store reference: " + any);
		}
		out.storeReference(any);
		if (!writeComplex(out, any)) {
			if (log.isDebugEnabled()) {
				log.debug("Unable to serialize: " + any);
			}
		}
	}

	/**
	 * Writes a primitive out as an object
	 *
	 * @param out              Output writer
	 * @param basic            Primitive
	 * @return boolean         true if object was successfully serialized, false otherwise
	 */
	protected boolean writeBasic(Output out, Object basic) {
		if (basic == null) {
			out.writeNull();
		} else if (basic instanceof Boolean) {
			out.writeBoolean((Boolean) basic);
		} else if (basic instanceof Number) {
			out.writeNumber((Number) basic);
		} else if (basic instanceof String) {
			out.writeString((String) basic);
		} else if (basic instanceof Date) {
			out.writeDate((Date) basic);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a complex type object out
	 *
	 * @param out        Output writer
	 * @param complex    Complex datatype object
	 * @return boolean   true if object was successfully serialized, false otherwise
	 */
	public boolean writeComplex(Output out, Object complex) {
		if (log.isDebugEnabled()) {
			log.debug("writeComplex");
		}
		if (writeListType(out, complex)) {
			return true;
		} else if (writeArrayType(out, complex)) {
			return true;
		} else if (writeXMLType(out, complex)) {
			return true;
		} else if (writeCustomType(out, complex)) {
			return true;
		} else if (writeObjectType(out, complex)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Writes Lists out as a data type
	 *
	 * @param out             Output write
	 * @param listType        List type
	 * @return boolean        true if object was successfully serialized, false otherwise
	 */
	protected boolean writeListType(Output out, Object listType) {
		if (log.isDebugEnabled()) {
			log.debug("writeListType");
		}
		if (listType instanceof List) {
			writeList(out, (List) listType);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a List out as an Object
	 *
	 * @param out             Output writer
	 * @param list            List to write as Object
	 */
	protected void writeList(Output out, List list) {
		// if its a small list, write it as an array
		if (list.size() < 100) {
			writeListAsArray(out, list);
			return;
		}
		// else we should check for lots of null values,
		// if there are over 80% then its probably best to do it as a map
		int size = list.size();
		int nullCount = 0;
		for (int i = 0; i < size; i++) {
			if (list.get(i) == null) {
				nullCount++;
			}
		}
		if (nullCount > (size * 0.8)) {
			writeListAsMap(out, list);
		} else {
			writeListAsArray(out, list);
		}
	}

    /**
     * Writes list as map
     *
     * @param out        Output writer
     * @param list       List to write
     */
    protected void writeListAsMap(Output out, List list) {
		int size = list.size();
        // Write start map marker
        out.writeStartMap(size);
		for (int i = 0; i < size; i++) {
			Object item = list.get(i);
			if (item != null) {
                // Write key
                out.writeItemKey(Integer.toString(i));
                // Serialize
                serialize(out, item);
                // Write item separator mark
                out.markItemSeparator();
			}
		}
		out.markEndMap();
	}

    /**
     * Write list as array. Used for small sized lists (size < 100)
     * @param out           Output object
     * @param list          List to write
     */
    protected void writeListAsArray(Output out, List list) {
		int size = list.size();
        // Write begin of array marker
        out.writeStartArray(size);
		for (int i = 0; i < size; i++) {
			if (i > 0) {
                // Write element separator
                out.markElementSeparator();
			}
			//log.info(i);
			serialize(out, list.get(i));
		}
        // Write end of array marker
        out.markEndArray();
	}

	/**
	 * Writes array (or collection) out as output Arrays, Collections, etc
	 *
	 * @param out               Output object
	 * @param arrType           Array or collection type
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	protected boolean writeArrayType(Output out, Object arrType) {
		if (log.isDebugEnabled()) {
			log.debug("writeArrayType");
		}
		if (arrType instanceof Collection) {
			writeCollection(out, (Collection<Object>) arrType);
		} else if (arrType instanceof Iterator) {
			writeIterator(out, (Iterator<Object>) arrType);
		} else if (arrType.getClass().isArray()
				&& arrType.getClass().getComponentType().isPrimitive()) {
			writePrimitiveArray(out, arrType);
		} else if (arrType instanceof Object[]) {
			writeObjectArray(out, (Object[]) arrType);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a collection to the output
	 *
	 * @param out        Output object
	 * @param col        Collection
	 */
	protected void writeCollection(Output out, Collection<Object> col) {
		if (log.isDebugEnabled()) {
			log.debug("writeCollection");
		}
        // Write begin of array marker
        out.writeStartArray(col.size());
		Iterator<Object> it = col.iterator();
		boolean isFirst = true;
        // Iterate thru elements to write element separators
        while (it.hasNext()) {
			if (!isFirst) {
                // Write element separator
                out.markElementSeparator();
			} else {
                // Do not write separator before first object
                isFirst = false;
			}
            // Serialize
            serialize(out, it.next());
		}
        // Write end of array marker
        out.markEndArray();
	}

	/**
	 * Writes a primitive array to the output
	 *
	 * @param out           Output writer
	 * @param array         Array
	 */
	protected void writePrimitiveArray(Output out, Object array) {
		//out.writeS
		if (log.isDebugEnabled()) {
			log.debug("write primitive array");
		}
        // Write start of array marker
        out.writeStartArray(Array.getLength(array));
		Iterator it = IteratorUtils.arrayIterator(array);
		while (it.hasNext()) {
            // Serialize element
            serialize(out, it.next());
			if (it.hasNext()) {
                // Write element separator
                out.markElementSeparator();
			}
		}
        // Write end of array marker
        out.markEndArray();
	}

	/**
	 * Writes an object array out to the output
	 *
	 * @param out         Output writer
	 * @param array       Array of objects
	 */
	protected void writeObjectArray(Output out, Object[] array) {
		//out.writeS
		if (log.isDebugEnabled()) {
			log.debug("write object array");
		}
        // Write out array start marker
        out.writeStartArray(array.length);

		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
                // Write element separator
                out.markElementSeparator();
			}
			//log.info(i);
            // Serialize array item
            serialize(out, array[i]);
		}

        // Write out end of array marker
        out.markEndArray();
	}

	/**
	 * Writes an iterator out to the output
	 *
	 * @param out          Output writer
	 * @param it           Iterator to write
	 */
	protected void writeIterator(Output out, Iterator<Object> it) {
		if (log.isDebugEnabled()) {
			log.debug("writeIterator");
		}
        // Create LinkedList of collection we iterate thru and write it out later
        LinkedList<Object> list = new LinkedList<Object>();
		while (it.hasNext()) {
			list.addLast(it.next());
		}
        // Write out collection
        writeCollection(out, list);
	}

	/**
	 * Writes an xml type out to the output
	 *
	 * @param out          Output writer
	 * @param xml          XML
	 * @return boolean     <code>true</code> if object was successfully written, <code>false</code> otherwise
	 */
	protected boolean writeXMLType(Output out, Object xml) {
		if (log.isDebugEnabled()) {
			log.debug("writeXMLType");
		}
        // If it's a Document write it as Document
        if (xml instanceof Document) {
            writeDocument(out, (Document) xml);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a document to the output
	 *
	 * @param out           Output writer
	 * @param doc           Document to write
	 */
	protected void writeDocument(Output out, Document doc) {
        // Write Document converted to String
        out.writeXML(XMLUtils.docToString(doc));
	}

	/**
	 * Write typed object to the output
	 *
	 * @param out           Output writer
	 * @param obj           Object type to write
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	protected boolean writeObjectType(Output out, Object obj) {
		if (obj instanceof Map) {
			writeMap(out, (Map) obj);
		} else if (obj instanceof RecordSet) {
			writeRecordSet(out, (RecordSet) obj);
		} else if (!writeBean(out, obj)) {
			writeObject(out, obj);
		}
		return true;
	}

	/**
	 * Writes a RecordSet to the output.
	 *
	 * @param out          Output writer
	 * @param set          RecordSet to write
	 */
	protected void writeRecordSet(Output out, RecordSet set) {
		if (log.isDebugEnabled()) {
			log.debug("writeRecordSet");
		}

        // Write out start of object marker
        out.writeStartObject("RecordSet");
        // Serialize
        Map<String, Object> info = set.serialize();
        // Write out serverInfo key
        out.writeItemKey("serverInfo");
        // Serialize
        serialize(out, info);
        // Write out end of object marker
        out.markEndObject();
	}

	/**
	 * Writes a map to the output
	 *
	 * @param out       Output writer
	 * @param map       Map to write
	 */
	public void writeMap(Output out, Map map) {
		if (log.isDebugEnabled()) {
			log.debug("writeMap");
		}

		final Set set = map.entrySet();
		// NOTE: we encode maps as objects so the flash client
		//       can access the entries through attributes
		Iterator it = set.iterator();
		boolean isBeanMap = (map instanceof BeanMap);
		out.writeStartObject(null, isBeanMap ? map.size() - 1 : map.size());
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			if (isBeanMap && entry.getKey().equals("class")) {
				continue;
			}
			out.writeItemKey(entry.getKey().toString());
			serialize(out, entry.getValue());
            // ...only write separator if there's
            if (it.hasNext()) {
                out.markPropertySeparator();
			}
		}
        // Write out end of map mark
        out.markEndMap();
	}

	/**
	 * Write object as bean to the output.
	 *
	 * @param out         Output writer
	 * @param bean        Bean to write
     * @return            true if bean was successfully written, false otherwise
	 */
	public boolean writeBean(Output out, Object bean) {
        // Create new map out of bean properties
        BeanMap beanMap = new BeanMap(bean);
        // Set of bean attributes
        Set set = beanMap.entrySet();
		if ((set.size() == 0)
				|| (set.size() == 1 && beanMap.containsKey("class"))) {
			// BeanMap is empty or can only access "class" attribute, skip it
			return false;
		}

        // Write out either start of object marker for class name or "empty" start of object marker
        if (isOptEnabled(bean, SerializerOption.SerializeClassName)) {
			out.writeStartObject(bean.getClass().getName());
		} else {
			out.writeStartObject(null, set.size() - 1);
		}
		Iterator it = set.iterator();
        // Iterate thru entries and write out property names with separators
		Class beanClass = bean.getClass();
        while (it.hasNext()) {
			BeanMap.Entry entry = (BeanMap.Entry) it.next();
			if (entry.getKey().toString().equals("class")) {
				continue;
			}

			String keyName = entry.getKey().toString();
			// Check if the Field corresponding to the getter/setter pair is transient
			try {
				Field field = beanClass.getDeclaredField(keyName);
				int modifiers = field.getModifiers();
				
				if (Modifier.isTransient(modifiers)) {
					if (log.isDebugEnabled()) {
						log.debug("Skipping " + field.getName() + " because its transient");
					}
					continue;
				}
			} catch (NoSuchFieldException nfe) {
				// Ignore this exception and use the default behaviour
			}
			
			out.writePropertyName(keyName);
			serialize(out, entry.getValue());
			if (it.hasNext()) {
				out.markPropertySeparator();
			}
		}
        // Write out end of object mark
		out.markEndObject();
		return true;
	}

	/**
	 * Writes an arbitrary object to the output.
	 *
	 * @param out           Output writer
	 * @param object        Object to write
	 */
	public void writeObject(Output out, Object object) {
		if (log.isDebugEnabled()) {
			log.debug("writeObject");
		}
        // If we need to serialize class information...
        if (isOptEnabled(object, SerializerOption.SerializeClassName)) {
            // Write out start object marker for class name
            out.writeStartObject(object.getClass().getName());
		} else {
            // Write out start object marker without class name
            out.writeStartObject(null);
		}

		// Get public field values
		Map<String, Object> values = new HashMap<String, Object>();
        // Iterate thru fields of an object to build "name-value" map from it
        for (Field field : object.getClass().getFields()) {
			Object value;
			try {
                // Get field value
                value = field.get(object);
			} catch (IllegalAccessException err) {
                // Swallow on private and protected properties access exception
                continue;
			}
            // Put field to the map of "name-value" pairs
            values.put(field.getName(), value);
		}

		// Output public values
		Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator();
        // Iterate thru map and write out properties with separators
        while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
            // Write out prop name
            out.writePropertyName(entry.getKey());
            // Write out
            serialize(out, entry.getValue());
			if (it.hasNext()) {
                // Write out property separator
                out.markPropertySeparator();
			}
		}
        // Write out end of object marker
		out.markEndObject();
	}

	// Extension points
	/**
	 * Pre processes an object
     * TODO // must be implemented
     *
     * @return              Prerocessed object
     * @param any           Object to preprocess
     */
	public Object preProcessExtension(Object any) {
		// Does nothing right now but will later
		return any;
	}

	/**
	 * Writes a custom data to the output
	 *
	 * @param out       Output writer
	 * @param obj       Custom data
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	protected boolean writeCustomType(Output out, Object obj) {
		if (out.isCustom(obj)) {
            // Write custom data
            out.writeCustom(obj);
			return true;
		} else {
			return false;
		}
	}

    /**
     * Check serializer options whether given object turned on
     *
     * @param obj           Option
     * @param opt           Serializer option
     * @return              true if options is enabled, false otherwise
     */
    public boolean isOptEnabled(Object obj, SerializerOption opt) {
		if (obj != null) {
			if (obj instanceof SerializerOpts) {
                // Cast to SerializerOption
                SerializerOpts opts = (SerializerOpts) obj;
                // Get flag
                Flag flag = opts.getSerializerOption(opt);
                // Check against Default first
                if (flag != Flag.Default) {
					return (flag == Flag.Enabled);
				}
			}
		}
        // Check against Enabled flag
        return getSerializerOption(opt) == Flag.Enabled;
	}

    /**
     *  Return Flag (enum that can be Enabled, Disabled or Default)
     *
     * @param opt       Serializer option to check
     * @return          Property flag enum value (Default, Enabled or Disabled)
     */
    public Flag getSerializerOption(SerializerOption opt) {
		// We can now return defaults
		switch (opt) {
			case SerializeClassName:
                return Flag.Enabled;
		}
        // Return disabled otherwise
        return Flag.Disabled;
	}

}
