/*
 * Spark | Java Flash Server
 * For more details see: http://www.osflash.org
 * Copyright 2005, Luke Hubbard luke@codegent.com
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.red5.server.script;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public interface ErrorBean {

	public abstract Exception getException();

}