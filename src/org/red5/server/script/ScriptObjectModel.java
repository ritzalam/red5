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

import java.io.IOException;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;


/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class ScriptObjectModel implements ResourceLoader, ResourcePatternResolver {

	ApplicationContext appCtx;
	
	public ScriptObjectModel(ApplicationContext appCtx){
		this.appCtx = appCtx;
	}
	
	/* REMOVED FOR NOW
	public HttpServletRequest getRequest(){
		return RequestContext.getHttpServletRequest();
	}

	public HttpSession getSession(){
		return RequestContext.getHttpSession();
	}
	
	public HttpSession getSession(boolean create){
		return RequestContext.getHttpSession(create);
	}
	
	public Cookie[] getCookies(){
		return RequestContext.getHttpServletRequest().getCookies();
	}
	
	
	public Cookie getCookieByName(String name){
		return getCookie(RequestContext.getHttpServletRequest(),name);
	}
	
	public Cookie createCookie(String name, String value){
		return new Cookie(name,value);
	}
	*/
	
	/*
	public void addCookie(String name, String value, Integer age, String path, String domain, Boolean secure){
		Cookie cookie = new Cookie(name,value);
		if(cookie==null) return;
		if(age!=null) cookie.setMaxAge(age.intValue());
		if(path!=null) cookie.setPath(path);
		if(domain!=null) cookie.setDomain(domain);
		if(secure!=null) cookie.setSecure(secure.booleanValue());
		addCookie(cookie);
	}*/
	
	/*
	public void addCookie(Cookie cookie){
		RequestContext.getHttpServletResponse().addCookie(cookie);
	}
	*/
	
	public ListableBeanFactory getBeans(){
		return appCtx;
	}
	
	public ApplicationContext getApplicationContext(){
		return appCtx;
	}
	
	/*
	public ListableBeanFactory getScripts(){
		return getScriptBeanFactory();
	}
	
	public ListableBeanFactory getScriptBeanFactory(){
		return appCtx;
	}*/
	
	public MessageSource getMessageSource(){
		return appCtx;
	}
	
	public Resource getResource(String path) {
		return appCtx.getResource(path);
	}
	
	public Resource[] getResources(String pattern) throws IOException {
		return appCtx.getResources(pattern);
	}
	
	/*
	protected Cookie getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if(cookies==null) return null;
		for(int i=0; i<cookies.length; i++){
			if(cookies[i].getName().equals(name)) 
				return cookies[i];
		}
		return null;
	}
	*/
	
}
