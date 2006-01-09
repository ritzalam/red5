package org.red5.server.io.mock;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.io.BaseInput;

public class Input extends BaseInput implements org.red5.server.io.Input  {

	protected static Log log =
        LogFactory.getLog(Input.class.getName());
	
	protected List list;
	protected int idx;
	
	public Input(List list){
		super();
		this.list = list;
		this.idx = 0;
	}

	protected Object getNext(){
		return list.get(idx++);
	}
		
	public byte readDataType(){
		Byte b = (Byte) getNext();
		return b.byteValue();
	}
	
	// Basic
	
	public Object readNull() {
		return null;
	}

	public Boolean readBoolean() {
		return (Boolean) getNext();
	}

	public Number readNumber() {
		return (Number) getNext();
	}

	public String readString(){
		return (String) getNext();
	}

	public Date readDate() {
		return (Date) getNext();
	}

	// Array
	
	public int readStartArray() {
		Integer i = (Integer) getNext();
		return i.intValue();
	}

	public void skipElementSeparator() {
		getNext();
	}

	public void skipEndArray() {
		// SKIP
	}
	
	public boolean hasMoreItems() {
		Object next = list.get(idx);
		if (! (next instanceof Byte)) return true;
		Byte b = (Byte) next;
		return (b.byteValue() != Mock.TYPE_END_OF_LIST);
	}

	public int readItemIndex() {
		return ((Integer) getNext()).intValue();
	}

	public int readStartList() {
		return ((Integer) getNext()).intValue();
	}

	public void skipEndList() {
		getNext();
	}

	public void skipItemSeparator() {
		getNext();
	}
	
	// Object

	public String readStartObject() {
		return readString();
	}

	public boolean hasMoreProperties() {
		Object next = list.get(idx);
		if (! (next instanceof Byte)) return true;
		Byte b = (Byte) next;
		return (b.byteValue() != Mock.TYPE_END_OF_OBJECT);
	}

	public String readPropertyName() {
		return (String) getNext();
	}

	public void skipPropertySeparator(){
		getNext();
	}

	public void skipEndObject() {
		getNext();
	}

    // Others
	
	public String readXML() {
		return readString();
	}

	public Object readCustom(){
		// Not supported
		return null;
	}

	public Object readReference() {
		final Short num = (Short) getNext();
		return getReference(num.shortValue());
	}


	
	
	
}
