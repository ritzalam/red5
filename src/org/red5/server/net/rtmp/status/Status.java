package org.red5.server.net.rtmp.status;

import org.red5.io.object.SerializerOpts;

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

/**
 * Represents status object that are transferred between server and client
 */
public class Status implements StatusCodes, SerializerOpts {

	public static final String ERROR = "error";

	public static final String STATUS = "status";

	public static final String WARNING = "warning";

	protected String code;

	protected String level;

	protected String description = "";

	protected String details = "";

	protected int clientid = 0;

	/** Constructs a new Status. */
    public Status() {

	}

	public Status(String code) {
		this.code = code;
		this.level = STATUS;
	}

	public Status(String code, String level, String description) {
		this.code = code;
		this.level = level;
		this.description = description;
	}

	/**
     * Getter for property 'code'.
     *
     * @return Value for property 'code'.
     */
    public String getCode() {
		return code;
	}

	/**
     * Setter for property 'code'.
     *
     * @param code Value to set for property 'code'.
     */
    public void setCode(String code) {
		this.code = code;
	}

	/**
     * Getter for property 'description'.
     *
     * @return Value for property 'description'.
     */
    public String getDescription() {
		return description;
	}

	/**
     * Setter for property 'desciption'.
     *
     * @param description Value to set for property 'desciption'.
     */
    public void setDesciption(String description) {
		this.description = description;
	}

	/**
     * Getter for property 'level'.
     *
     * @return Value for property 'level'.
     */
    public String getLevel() {
		return level;
	}

	/**
     * Setter for property 'level'.
     *
     * @param level Value to set for property 'level'.
     */
    public void setLevel(String level) {
		this.level = level;
	}

	/**
     * Getter for property 'clientid'.
     *
     * @return Value for property 'clientid'.
     */
    public int getClientid() {
		return clientid;
	}

	/**
     * Setter for property 'clientid'.
     *
     * @param clientid Value to set for property 'clientid'.
     */
    public void setClientid(int clientid) {
		this.clientid = clientid;
	}

	/**
     * Getter for property 'details'.
     *
     * @return Value for property 'details'.
     */
    public String getDetails() {
		return details;
	}

	/**
     * Setter for property 'details'.
     *
     * @param details Value to set for property 'details'.
     */
    public void setDetails(String details) {
		this.details = details;
	}

	/**
     * Setter for property 'description'.
     *
     * @param description Value to set for property 'description'.
     */
    public void setDescription(String description) {
		this.description = description;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "Status: code: " + getCode() + " desc: " + getDescription()
				+ " level: " + getLevel();
	}

	/** {@inheritDoc} */
    public Flag getSerializerOption(SerializerOption opt) {
		if (opt == SerializerOption.SerializeClassName) {
			return Flag.Disabled;
		}
		return Flag.Default;
	}

}
