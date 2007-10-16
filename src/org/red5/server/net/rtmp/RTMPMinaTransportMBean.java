package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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
 
import org.apache.mina.common.IoHandlerAdapter;

/** 
 * 
 */
public interface RTMPMinaTransportMBean {

	public void setAddress(String address);
	public void setPort(int port);
	public void setIoThreads(int ioThreads);
	public void setEventThreadsCore(int eventThreadsCore);
	public void setEventThreadsMax(int eventThreadsMax);
	public void setEventThreadsKeepalive(int eventThreadsKeepalive);
	public void setEventThreadsQueue(int eventThreadsQueue);
	public void setIsLoggingTraffic(boolean isLoggingTraffic);
	public void setIoHandler(IoHandlerAdapter rtmpIOHandler);
	public void setReceiveBufferSize(int receiveBufferSize);
	public void setSendBufferSize(int sendBufferSize);
	public void setTcpNoDelay(boolean tcpNoDelay);
	public void setUseHeapBuffers(boolean useHeapBuffers);
	public void start() throws Exception;
	public void stop();
}