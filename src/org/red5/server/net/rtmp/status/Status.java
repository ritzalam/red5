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

public class Status implements StatusCodes, SerializerOpts {

	public static final String ERROR = "error";
	public static final String STATUS = "status";
	public static final String WARNING = "warning";
	
	protected String code;
	protected String level;
	protected String description = "";
	protected String details = "";
	protected int clientid = 0;
	
	public Status(){
		
	}
	
	public Status(String code){
		this.code = code;
		this.level = STATUS;
	}
	
	public Status(String code, String level, String description){
		this.code = code;
		this.level = level;
		this.description = description;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDesciption(String description) {
		this.description = description;
	}
	
	public String getLevel(){
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	
	public int getClientid() {
		return clientid;
	}



	public void setClientid(int clientid) {
		this.clientid = clientid;
	}



	public String getDetails() {
		return details;
	}



	public void setDetails(String details) {
		this.details = details;
	}



	public void setDescription(String description) {
		this.description = description;
	}

	
	public String toString(){
		return "Status: code: "+getCode()
			+ " desc: "+getDescription() 
			+ " level: "+getLevel();
	}

	public Flag getSerializerOption(SerializerOption opt) {
		if(opt == SerializerOption.SerializeClassName) return Flag.Disabled;
		return Flag.Default;
	}
	
}
