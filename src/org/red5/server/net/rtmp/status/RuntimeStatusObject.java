package org.red5.server.net.rtmp.status;

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

public class RuntimeStatusObject extends StatusObject {

	protected String details = "";
	protected int clientid = 0;
	
	public RuntimeStatusObject(){
		super();
	}
	
	public RuntimeStatusObject(String code, String level, String description){
		super(code, level, description);
	}
	
	public RuntimeStatusObject(String code, String level, String description, 
			String details, int clientid){
		super(code, level, description);
		this.details = details;
		this.clientid = clientid;
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
	
}
