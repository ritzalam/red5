package org.red5.server.service;

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
 
import org.red5.server.api.service.IServiceCall;

/**
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Call implements IServiceCall {

	public static final byte STATUS_PENDING = 0x01;

	public static final byte STATUS_SUCCESS_RESULT = 0x02;

	public static final byte STATUS_SUCCESS_NULL = 0x03;

	public static final byte STATUS_SUCCESS_VOID = 0x04;
	
	public static final byte STATUS_SERVICE_NOT_FOUND = 0x10;

	public static final byte STATUS_METHOD_NOT_FOUND = 0x11;

	public static final byte STATUS_ACCESS_DENIED = 0x12;

	public static final byte STATUS_INVOCATION_EXCEPTION = 0x13;

	public static final byte STATUS_GENERAL_EXCEPTION = 0x14;
	
    protected String serviceName = null;

    protected String serviceMethodName = null;

    protected Object[] arguments = null;

    protected byte status = STATUS_PENDING;

    protected Exception exception = null;
    
    public Call(String method){
		serviceMethodName = method;
    }
    
    public Call(String method, Object[] args){
		serviceMethodName = method;
		arguments = args;
    }
    
    public Call(String name, String method, Object[] args){
    		serviceName = name;
    		serviceMethodName = method;
    		arguments = args;
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#isSuccess()
	 */
    public boolean isSuccess(){
		return (status == STATUS_SUCCESS_RESULT)
				|| (status == STATUS_SUCCESS_NULL)
				|| (status == STATUS_SUCCESS_VOID);
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#getServiceMethodName()
	 */
	public String getServiceMethodName() {
		return serviceMethodName;
	}

	public void setServiceMethodName(String serviceMethodName) {
		this.serviceMethodName = serviceMethodName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#getServiceName()
	 */
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#getArguments()
	 */
	public Object[] getArguments() {
		return arguments;
	}

	public void setArguments(Object[] args) {
		arguments = args;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#getStatus()
	 */
	public byte getStatus() {
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.temp#setStatus(byte)
	 */
	public void setStatus(byte status) {
		this.status = status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.ServiceCall#getException()
	 */
	public Exception getException() {
		return exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.service.temp#setException(java.lang.Exception)
	 */
	public void setException(Exception exception) {
		this.exception = exception;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("Service: "+serviceName+" Method: "+serviceMethodName);
		if(arguments!=null) {
			sb.append(" Num Params: "+arguments.length);
			for(int i=0; i<arguments.length; i++){
				sb.append(i).append(": ").append(arguments[i]);
			}
		} else
			sb.append(" No params");
		return sb.toString();
	}
	
}
