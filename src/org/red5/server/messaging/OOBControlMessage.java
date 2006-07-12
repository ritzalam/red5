package org.red5.server.messaging;

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

import java.io.Serializable;
import java.util.Map;

/**
 * Out-of-band control message used by inter-components communication
 * which are connected with pipes.
 * <tt>'Target'</tt> is used to represent the receiver who may be
 * interested for receiving. It's a string of any form.
 * XXX shall we design a standard form for Target, like "class.instance"?
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class OOBControlMessage implements Serializable {
	private static final long serialVersionUID = -6037348177653934300L;

	private String target;
	private String serviceName;
	private Map serviceParamMap;
	private Object result;
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public Map getServiceParamMap() {
		return serviceParamMap;
	}
	public void setServiceParamMap(Map serviceParamMap) {
		this.serviceParamMap = serviceParamMap;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
}
