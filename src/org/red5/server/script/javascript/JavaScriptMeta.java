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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class JavaScriptMeta  {

	protected String className = null;
	protected String extendClassName = null;
	protected ArrayList interfaceList = new ArrayList();
	protected HashMap methodMap = new HashMap();

    public String getClassName() {
        return className;
    }
	
	public String getExtends(){
		return extendClassName;
	}
	
	public String[] getImplements(){
		return (String[]) interfaceList.toArray(new String[0]);
	}
	
	public String[] getMethodNames(){
		return (String[]) methodMap.keySet().toArray(new String[0]);
	}
	
	public String getMethodDescriptor(String name){
		if(!methodMap.containsKey(name)) return null;
		String[] method = (String[]) methodMap.get(name);
		String sig = ( (method[1]==null||method[1].length()==0) ? "void" : method[1] );
		sig += ' ' + name + "(";
		sig += ( (method[0]==null||method[0].length()==0) ?  "" : method[0] ) + ")";
		return sig;
	}
	
	public void setClassName(String className){
		this.className = className;
	}
	
	public void setExtends(String extendClassName){
		this.extendClassName = extendClassName;
	}
	
	public void addInterface(String className){
		interfaceList.add(className);
	}
	
	public void addMethod(String name, String param, String returns){
		methodMap.put(name,new String[]{param,returns});
	}
	
	//NOTE: include a nice tostring method here, so we can call it from the generator
	
	
}
