package org.red5.server.net.rtmp.message;

public class Ping extends Message {
	
	private static final int INITIAL_CAPACITY = 6;
	public static final int UNDEFINED = -1;
	
	private short value1 = 0;
	private int value2 = 0;
	private int value3 = UNDEFINED;
	
	public Ping(){
		super(TYPE_PING, INITIAL_CAPACITY);
	}

	public short getValue1() {
		return value1;
	}

	public void setValue1(short value1) {
		this.value1 = value1;
	}

	public int getValue2() {
		return value2;
	}

	public void setValue2(int value2) {
		this.value2 = value2;
	}
	
	public int getValue3() {
		return value3;
	}

	public void setValue3(int value3) {
		this.value3 = value3;
	}

	protected void doRelease() {
		value1 = 0;
		value2 = 0;
		value3 = UNDEFINED;
	}
	
	public String toString(){
		return "Ping: "+value1+", "+value2+", "+value3;
	}
	
}
