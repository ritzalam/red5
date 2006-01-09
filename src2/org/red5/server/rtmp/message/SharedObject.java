package org.red5.server.rtmp.message;

import java.util.LinkedList;

public class SharedObject extends Unknown {

	private String name;
	private LinkedList numbers=new LinkedList();
	private long id;
	private String key;
	private Object value;
	private int timer;

	public SharedObject(){
		super(TYPE_SHARED_OBJECT);
	}
	
	public long getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	public LinkedList getNumbers(){
		return numbers;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
	}

	public String toString(){
		final StringBuffer sb = new StringBuffer();
		sb.append("SharedObject: ").append(name).append(" | ");
		sb.append("key: ").append(key);
		sb.append("value: ").append(value);
		sb.append(" | ").append(getData().getHexDump());
		return sb.toString();
	}
	
}
