package org.red5.server.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BeanMap;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.utils.XMLUtils;
import org.springframework.beans.BeanUtils;
import org.w3c.dom.Document;

/**
 * The Serializer class writes data output and handles the data 
 * according to the core data types 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class Serializer {

	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Serializer.class.getName());
	
	/**
	 * serializes output to a core data type object
	 * @param out
	 * @param any
	 */
	public void serialize(Output out, Object any) {
		// TODO Auto-generated method stub
		if(log.isDebugEnabled()) {
			log.debug("serialize");
		}
		if(writeBasic(out,any)){
			if(log.isDebugEnabled()) {
				log.debug("write basic");
			}
			return;
		}
		
		// Extension point
		if(out.hasReference(any)){
			if(log.isDebugEnabled()) {
				log.debug("write ref");
			}
			out.writeReference(any);
			return;
		}

		if(log.isDebugEnabled()) {
			log.debug("Store reference: "+any);
		}
		out.storeReference(any);
		if(!writeComplex(out,any))
			if(log.isDebugEnabled()) {
				log.debug("Unable to serialize: "+any);
			}
	}
		
	/**
	 * Writes a primitive out as an object
	 * @param out
	 * @param basic
	 * @return boolean
	 */
	protected boolean writeBasic(Output out, Object basic){
		if(basic==null) 
			out.writeNull();
		else if(basic instanceof Boolean) 
			out.writeBoolean((Boolean) basic);
		else if(basic instanceof Number)
			out.writeNumber((Number) basic);
		else if(basic instanceof String)
			out.writeString((String) basic);
		else if(basic instanceof Date)
			out.writeDate((Date) basic);
		else return false;
		return true;
	}
	
	/**
	 * Writes a complex type out as an object
	 * @param out
	 * @param complex
	 * @return boolean
	 */
	public boolean writeComplex(Output out, Object complex){
		if(log.isDebugEnabled()) {
			log.debug("writeComplex");
		}
		if(writeListType(out,complex)) return true;
		else if(writeArrayType(out,complex)) return true;
		else if(writeXMLType(out,complex)) return true;
		else if(writeCustomType(out, complex)) return true;
		else if(writeObjectType(out,complex)) return true;
		else return false;
	}
	
	/**
	 * Writes Lists out as a data type
	 * @param out
	 * @param listType
	 * @return boolean
	 */
	protected boolean writeListType(Output out, Object listType){
		if(log.isDebugEnabled()) {
			log.debug("writeListType");
		}
		if(listType instanceof List) {
			writeList(out, (List) listType);
		} else return false;
		return true;
	}
	
	/**
	 * Writes a List out as an Object
	 * @param out
	 * @param list
	 */
	protected void writeList(Output out, List list){
		int size = list.size();
		out.writeStartList(size);
		for(int i=0; i<size; i++){
			Object item = list.get(i);
			if(item!=null){
				out.writeItemIndex(i);
				serialize(out, item);
				out.markItemSeparator();
			}
		}
		out.markEndList();
	}
	
	/**
	 * Writes an Array type out as output
	 * Arrays, Collections, etc
	 * @param out
	 * @param arrType
	 * @return
	 */
	protected boolean writeArrayType(Output out, Object arrType){
		if(log.isDebugEnabled()) {
			log.debug("writeArrayType");
		}
		if(arrType instanceof Collection) {
			writeCollection(out, (Collection) arrType);
		} else if(arrType instanceof Iterator) {
			writeIterator(out, (Iterator) arrType);
		// Need a replacement here. Perhaps copy from spring BeanUtils to util class
		} else if(BeanUtils.isPrimitiveArray(arrType.getClass())) {
			writePrimitiveArray(out, arrType);
		} else if(arrType instanceof Object[]) {
			writeObjectArray(out, (Object[]) arrType);
		} else return false;
		return true;
	}
	
	/**
	 * Writes a collection to the output
	 * @param out
	 * @param col
	 */
	protected void writeCollection(Output out, Collection col){
		if(log.isDebugEnabled()) {
			log.debug("writeCollection");
		}
		out.writeStartArray(col.size());
		Iterator it = col.iterator();
		boolean isFirst = true;
		while(it.hasNext()){
			if(!isFirst) out.markElementSeparator();
			else isFirst = false;
			serialize(out, it.next());
		}
		out.markEndArray();
	}
	
	/**
	 * Writes a primitive array to the output
	 * @param out
	 * @param array
	 */
	protected void writePrimitiveArray(Output out, Object array){
		//out.writeS
		if(log.isDebugEnabled()) {
			log.debug("write primitive array");
		}
		out.writeStartArray(Array.getLength(array));
		Iterator it = IteratorUtils.arrayIterator(array);
		while(it.hasNext()){
			serialize(out, it.next());
			if(it.hasNext()) out.markElementSeparator();
		}
		out.markEndArray();
	}
	
	/**
	 * Writes an object array out to the output
	 * @param out
	 * @param array
	 */
	protected void writeObjectArray(Output out, Object[] array){
		//out.writeS
		if(log.isDebugEnabled()) {
			log.debug("write object array");
		}
		out.writeStartArray(array.length);

		for (int i = 0; i < array.length; i++) {
			if(i>0) out.markElementSeparator();
			//log.info(i);
			serialize(out, array[i]);
		}

		out.markEndArray();
	}
	
	/**
	 * Writes an iterator out to the output
	 * @param out
	 * @param it
	 */
	protected void writeIterator(Output out,Iterator it){
		if(log.isDebugEnabled()) {
			log.debug("writeIterator");
		}
		LinkedList list = new LinkedList();
		while(it.hasNext()) list.addLast(it.next());
		writeCollection(out, list);
	}
	
	/**
	 * Writes an xml type out to the output
	 * @param out
	 * @param xml
	 * @return boolean
	 */
	protected boolean writeXMLType(Output out, Object xml){
		if(log.isDebugEnabled()) {
			log.debug("writeXMLType");
		}
		if(xml instanceof Document) 
			writeDocument(out, (Document) xml);
		else return false;
		return true;
	}
	
	/**
	 * Writes a document to the output
	 * @param out
	 * @param doc
	 */
	protected void writeDocument(Output out, Document doc){
		out.writeXML(XMLUtils.docToString(doc));
	}
	
	/**
	 * Writes an object to the output
	 * @param out
	 * @param obj
	 * @return
	 */
	protected boolean writeObjectType(Output out, Object obj){		
		if(obj instanceof Map) 
			writeMap(out, (Map) obj);
		else writeBean(out, obj);
		return true;
	}
	
	/**
	 * Writes a map to the output
	 * @param out
	 * @param map
	 */
	public void writeMap(Output out, Map map){
		if(log.isDebugEnabled()) {
			log.debug("writeMap");
		}
		out.writeStartObject(null);
		Set set = map.entrySet();
		Iterator it = set.iterator();
		boolean isBeanMap = (map instanceof BeanMap);
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry) it.next();
			if(isBeanMap && ((String)entry.getKey()).equals("class")) continue;
			out.writePropertyName(entry.getKey().toString());
			serialize(out,entry.getValue());
			if(it.hasNext()) out.markPropertySeparator();
		}
		out.markEndObject();
	}
	
	/**
	 * Writes a bean to the output
	 * @param out
	 * @param bean
	 */
	public void writeBean(Output out, Object bean){
		if(log.isDebugEnabled()) {
			log.debug("writeBean");
		}
		out.writeStartObject(bean.getClass().getName());
		BeanMap beanMap = new BeanMap(bean);
		Set set = beanMap.entrySet();
		Iterator it = set.iterator();
		while(it.hasNext()){
			BeanMap.Entry entry = (BeanMap.Entry) it.next();
			if(entry.getKey().toString().equals("class")) continue;
			out.writePropertyName(entry.getKey().toString());
			
			//log.info(entry.getKey().toString()+" = "+entry.getValue());
			serialize(out,entry.getValue());
			if(it.hasNext()) out.markPropertySeparator();
		}
		out.markEndObject();
	}
	
	// Extension points
	/**
	 * Pre processes an object
	 * TODO must be implemented
	 */
	public Object preProcessExtension(Object any){
		// Does nothing right now but will later
		return any;
	}
	
	/**
	 * Writes a custom data type to the output
	 * @param out
	 * @param obj
	 * @return
	 */
	protected boolean writeCustomType(Output out, Object obj){
		if(out.isCustom(obj)){
			out.writeCustom(obj);
			return true;
		} else return false;
	}

}
