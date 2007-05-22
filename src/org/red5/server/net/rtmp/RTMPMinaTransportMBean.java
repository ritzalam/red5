package org.red5.server.net.rtmp;

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