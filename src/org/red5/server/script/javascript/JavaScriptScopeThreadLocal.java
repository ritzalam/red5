/*
 * Spark | Java Flash Server
 * For more details see: http://www.osflash.org
 * Copyright 2005, Luke Hubbard luke@codegent.com
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * See the README.txt in this package for details of changes
 */
package org.red5.server.script.javascript;

import org.mozilla.javascript.ScriptableObject;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class JavaScriptScopeThreadLocal {

	private JavaScriptScopeThreadLocal() {
		// private constructor
	}
	
	static private ThreadLocal localScope = new ThreadLocal();
	static private ThreadLocal localMeta = new ThreadLocal();
	
	static public void setScope(ScriptableObject scope) {
		localScope.set(scope);
	}
	
	static public ScriptableObject getScope() {
		return (ScriptableObject) localScope.get();
	}
	
	static public void setMeta(JavaScriptMeta meta) {
		localMeta.set(meta);
	}
	
	static public JavaScriptMeta getMeta() {
		return (JavaScriptMeta) localMeta.get();
	}
	
}
