package org.red5.server.io.test;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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

import java.util.Date;

public class TestJavaBean {

	private byte testByte = 65;
	private int testPrimitiveNumber = 3;
	private Integer testNumberObject = new Integer(33);
	private String testString = "red5 rocks!";
	private Date testDate = new Date();
	private Boolean testBooleanObject = Boolean.FALSE;
	private boolean testBoolean = true;
	
	public byte getTestByte() {
		return testByte;
	}
	public void setTestByte(byte testByte) {
		this.testByte = testByte;
	}
	public boolean isTestBoolean() {
		return testBoolean;
	}
	public void setTestBoolean(boolean testBoolean) {
		this.testBoolean = testBoolean;
	}
	public Boolean getTestBooleanObject() {
		return testBooleanObject;
	}
	public void setTestBooleanObject(Boolean testBooleanObject) {
		this.testBooleanObject = testBooleanObject;
	}
	public Date getTestDate() {
		return testDate;
	}
	public void setTestDate(Date testDate) {
		this.testDate = testDate;
	}
	public Integer getTestNumberObject() {
		return testNumberObject;
	}
	public void setTestNumberObject(Integer testNumberObject) {
		this.testNumberObject = testNumberObject;
	}
	public int getTestPrimitiveNumber() {
		return testPrimitiveNumber;
	}
	public void setTestPrimitiveNumber(int testPrimitiveNumber) {
		this.testPrimitiveNumber = testPrimitiveNumber;
	}
	public String getTestString() {
		return testString;
	}
	public void setTestString(String testString) {
		this.testString = testString;
	}
	
}
