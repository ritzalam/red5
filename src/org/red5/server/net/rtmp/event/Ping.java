package org.red5.server.net.rtmp.event;

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

public class Ping extends BaseEvent {
	
	public static final int UNDEFINED = -1;
	
	private short value1 = 0; // XXX: can someone suggest better names? 
	private int value2 = 0;
	private int value3 = UNDEFINED;
	private String debug = "";
	
	public Ping(){
		super(Type.SYSTEM);
	}
	
	public Ping(short value1, int value2){
		super(Type.SYSTEM);
		this.value1 = value1;
		this.value2 = value2;
	}
	
	public Ping(short value1, int value2, int value3){
		super(Type.SYSTEM);
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
	}

	public byte getDataType() {
		return TYPE_PING;
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

	
	
	public String getDebug() {
		return debug;
	}

	public void setDebug(String debug) {
		this.debug = debug;
	}

	protected void doRelease() {
		value1 = 0;
		value2 = 0;
		value3 = UNDEFINED;
	}
	
	public String toString(){
		return "Ping: "+value1+", "+value2+", "+value3+" \n" + debug;
	}
	
}