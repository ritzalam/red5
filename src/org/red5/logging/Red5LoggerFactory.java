package org.red5.logging;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.impl.StaticLoggerBinder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;

/**
 * LoggerFactory to simplify requests for Logger instances within
 * Red5 applications. This class is expected to be run only once per
 * logger request and is optimized as such.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Red5LoggerFactory {

	@SuppressWarnings("unchecked")
	public static Logger getLogger(Class clazz) {
		//determine the red5 app name or servlet context name
		String contextName = null;
		
		/* TODO: For a future day, the context or application will be determined
		//get a reference to our caller
		Class caller = Reflection.getCallerClass(2);
		//System.err.printf("Caller class: %s classloader: %s\n", caller, caller.getClassLoader());

		try {
        	//check to see if we've been called by a servlet
			Class sub = caller.asSubclass(Servlet.class);
        	//System.err.println("Caller is a Servlet");
        
        	//Method[] methods = caller.getMethods();
        	//for (Method meth : methods) {
        	//	System.err.printf("Method: %s\n", meth.getName());
        	//}
        	
        	Method getContext = caller.getMethod("getServletContext", new Class[0]);
        	//System.err.printf("got context method - %s\n", getContext);
        	ServletContext context = (ServletContext) getContext.invoke(caller, null);
        	System.err.printf("invoked context\n");
        
        	contextName = context.getServletContextName();
        	//System.err.printf("Servlet context name: %s\n", contextName);

        	Method getContextName = context.getClass().getMethod("getServletContextName", new Class[0]);
        	System.err.printf("got context name\n");
        	Object ctxName = getContextName.invoke(null, new Object[0]);				
        	
        	System.err.printf("Servlet context result: %s\n", ctxName);
        	if (ctxName != null && ctxName instanceof String) {
        		contextName = ctxName.toString();
        	}	
        } catch (Exception ex) {
        	//ex.printStackTrace();
        }
		*/
		
		return getLogger(clazz, contextName);
	}

	@SuppressWarnings("unchecked")
	public static Logger getLogger(Class clazz, String contextName) {
		//get the context selector
		ContextSelector selector = StaticLoggerBinder.getSingleton()
				.getContextSelector();
		//get the context for the given context name or default if null
		LoggerContext ctx = null;
		if (contextName != null && contextName.length() > 0)
		{
			ctx = selector.getLoggerContext(contextName);
		}
		// and if we get here, fall back to the default context
		ctx = selector.getLoggerContext(); 
		//debug
		//StatusPrinter.print(ctx);
		
		return ctx != null ? ctx.getLogger(clazz) : null;
	}

}