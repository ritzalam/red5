/*
 * Spark | Java Flash Server
 * For more details see: http://www.osflash.org
 * Copyright 2005, Luke Hubbard luke@codegent.com
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * See the README.txt in this package for details of changes
 */
package org.red5.server.script;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class ScriptErrorBean implements ErrorBean {

	private Exception exception;
	
	public ScriptErrorBean(Exception ex){
		this.exception = ex;
	}

	/* (non-Javadoc)
	 * @see com.codegent.spark.script.ErrorBean#getException()
	 */
	public Exception getException() {
		return exception;
	}
	
	public void setException(Exception exception) {
		this.exception = exception;
	}

}
