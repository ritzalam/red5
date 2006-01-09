package org.red5.server.io.mock;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.io.DataTypes;
import org.red5.server.io.BaseOutput;

public class Output extends BaseOutput implements org.red5.server.io.Output {
	
	protected static Log log =
        LogFactory.getLog(Output.class.getName());
	
	protected List list;
	
	public Output(List list){
		super();
		this.list = list;
	}

	public boolean isCustom(Object custom) {
		// No custom types supported
		return false;
	}

	// DONE
	public void markElementSeparator() {
		list.add(new Byte(Mock.TYPE_ELEMENT_SEPARATOR));
	}

	// DONE
	public void markEndArray() {
		list.add(new Byte(Mock.TYPE_END_OF_ARRAY));
	}

	public void markEndObject(){
		list.add(new Byte(Mock.TYPE_END_OF_OBJECT));
	}
	
	public void markEndList(){
		list.add(new Byte(Mock.TYPE_END_OF_LIST));
	}

	// DONE
	public void markPropertySeparator() {
		log.info("PROPERTY SEPARATOR");
		list.add(new Byte(Mock.TYPE_PROPERTY_SEPARATOR));
	}
	
	public void markItemSeparator() {
		log.info("ITEM SEPARATOR");
		list.add(new Byte(Mock.TYPE_ITEM_SEPARATOR));
	}

	public boolean supportsDataType(byte type) {
		// does not yet support references 
		return type <= DataTypes.OPT_REFERENCE;
	}

	// DONE
	public void writeBoolean(Boolean bol) {
		list.add(new Byte(DataTypes.CORE_BOOLEAN));
		list.add(bol);
	}

	public void writeCustom(Object custom) {
		// Customs not supported by this version
	}

	public void writeDate(Date date) {
		list.add(new Byte(DataTypes.CORE_DATE));
		list.add(date);
	}

	// DONE
	public void writeNull() {
		list.add(new Byte(DataTypes.CORE_NULL));
	}

	// DONE
	public void writeNumber(Number num) {
		list.add(new Byte(DataTypes.CORE_NUMBER));
		list.add(num);
	}

	public void writePropertyName(String name) {
		list.add(name);
	}
	
	public void writeItemIndex(int index) {
		list.add(new Integer(index));
	}

	public void writeReference(Object obj) {
		list.add(new Byte(DataTypes.OPT_REFERENCE));
		list.add(new Short(getReferenceId(obj)));
	}

	public void writeStartArray(int length) {
		list.add(new Byte(DataTypes.CORE_ARRAY));
		list.add(new Integer(length));
	}
	
	public void writeStartList(int highestIndex) {
		list.add(new Byte(DataTypes.CORE_LIST));
		list.add(new Integer(highestIndex));
	}

	public void writeStartObject(String className) {
		list.add(new Byte(DataTypes.CORE_OBJECT));
		list.add(className == null ? null : className);
	}
	
	public void writeString(String string) {
		list.add(new Byte(DataTypes.CORE_STRING));
		list.add(string);
	}

	public void writeXML(String xml) {
		list.add(new Byte(DataTypes.CORE_XML));
		list.add(xml);
	}
	
}
