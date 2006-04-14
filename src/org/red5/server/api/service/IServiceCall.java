package org.red5.server.api.service;

/**
 * Container for a Service Call 
 */
public interface IServiceCall  {
	
	public abstract boolean isSuccess();

	public abstract String getServiceMethodName();

	public abstract String getServiceName();

	public abstract Object[] getArguments();

	public abstract byte getStatus();

	public abstract Exception getException();

	public abstract void setStatus(byte status);

	public abstract void setException(Exception exception);

}