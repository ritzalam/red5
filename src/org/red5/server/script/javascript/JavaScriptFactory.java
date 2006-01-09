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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.script.AbstractScriptFactory;
import org.springframework.beans.factory.script.Script;
import org.springframework.beans.factory.script.ScriptInterfaceException;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class JavaScriptFactory extends AbstractScriptFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3258693191396898612L;
	private long id = 0;
	
	protected final Log log = LogFactory.getLog(getClass());
	

	public JavaScriptFactory(){
		this.setExpirySeconds(1);
	}
	
	public Script lookupScript(Object o) {
		return super.lookupScript(o);
	}


	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.script.AbstractScriptFactory#createScript(java.lang.String)
	 */
	protected Script createScript(String location) throws BeansException {
		
		if(log.isDebugEnabled())
			log.debug("Creating script from: "+location);
		
		return new JavaScript(Long.toString(id++),location, this);
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.script.AbstractScriptFactory#requiresConfigInterface()
	 */
	protected boolean requiresConfigInterface() {
		return false;
	}

	public Script configuredScript(String location, String[] interfaceNames) throws BeansException {
		
		JavaScript script = (JavaScript) createScript(location);
		
		if(interfaceNames!=null && interfaceNames.length > 0){
		
			try {
				Class first = Class.forName(interfaceNames[0]);
				if(!first.isInterface()) script.setExtends(first);
				String[] trueInterfaceNames = new String[interfaceNames.length-1];
				for(int i=0; i<trueInterfaceNames.length; i++){
					trueInterfaceNames[i] = interfaceNames[i+1];
				}
				interfaceNames = trueInterfaceNames;
			}
			catch (ClassNotFoundException ex) {
				throw new ScriptInterfaceException(ex);
			}
		
		}
		
		// Add interfaces. This will not include any config interface.
		try {
			Class[] interfaces = AopUtils.toInterfaceArray(interfaceNames);
			for (int i = 0; i < interfaces.length; i++) {
				script.addInterface(interfaces[i]);
			}
			
			return script;
		}
		catch (ClassNotFoundException ex) {
			throw new ScriptInterfaceException(ex);
		}
	}
	
}
