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
	
	/**
     * Getter for property 'testByte'.
     *
     * @return Value for property 'testByte'.
     */
    public byte getTestByte() {
		return testByte;
	}
	/**
     * Setter for property 'testByte'.
     *
     * @param testByte Value to set for property 'testByte'.
     */
    public void setTestByte(byte testByte) {
		this.testByte = testByte;
	}
	/**
     * Getter for property 'testBoolean'.
     *
     * @return Value for property 'testBoolean'.
     */
    public boolean isTestBoolean() {
		return testBoolean;
	}
	/**
     * Setter for property 'testBoolean'.
     *
     * @param testBoolean Value to set for property 'testBoolean'.
     */
    public void setTestBoolean(boolean testBoolean) {
		this.testBoolean = testBoolean;
	}
	/**
     * Getter for property 'testBooleanObject'.
     *
     * @return Value for property 'testBooleanObject'.
     */
    public Boolean getTestBooleanObject() {
		return testBooleanObject;
	}
	/**
     * Setter for property 'testBooleanObject'.
     *
     * @param testBooleanObject Value to set for property 'testBooleanObject'.
     */
    public void setTestBooleanObject(Boolean testBooleanObject) {
		this.testBooleanObject = testBooleanObject;
	}
	/**
     * Getter for property 'testDate'.
     *
     * @return Value for property 'testDate'.
     */
    public Date getTestDate() {
		return testDate;
	}
	/**
     * Setter for property 'testDate'.
     *
     * @param testDate Value to set for property 'testDate'.
     */
    public void setTestDate(Date testDate) {
		this.testDate = testDate;
	}
	/**
     * Getter for property 'testNumberObject'.
     *
     * @return Value for property 'testNumberObject'.
     */
    public Integer getTestNumberObject() {
		return testNumberObject;
	}
	/**
     * Setter for property 'testNumberObject'.
     *
     * @param testNumberObject Value to set for property 'testNumberObject'.
     */
    public void setTestNumberObject(Integer testNumberObject) {
		this.testNumberObject = testNumberObject;
	}
	/**
     * Getter for property 'testPrimitiveNumber'.
     *
     * @return Value for property 'testPrimitiveNumber'.
     */
    public int getTestPrimitiveNumber() {
		return testPrimitiveNumber;
	}
	/**
     * Setter for property 'testPrimitiveNumber'.
     *
     * @param testPrimitiveNumber Value to set for property 'testPrimitiveNumber'.
     */
    public void setTestPrimitiveNumber(int testPrimitiveNumber) {
		this.testPrimitiveNumber = testPrimitiveNumber;
	}
	/**
     * Getter for property 'testString'.
     *
     * @return Value for property 'testString'.
     */
    public String getTestString() {
		return testString;
	}
	/**
     * Setter for property 'testString'.
     *
     * @param testString Value to set for property 'testString'.
     */
    public void setTestString(String testString) {
		this.testString = testString;
	}
	
}
