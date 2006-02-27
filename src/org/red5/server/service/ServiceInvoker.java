package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

public class ServiceInvoker  {

	private static final Log log = LogFactory.getLog(ServiceInvoker.class);
	
	public static final String SERVICE_NAME = "serviceInvoker";
	
	protected ApplicationContext serviceContext = null;
	
	//protected ScriptBeanFactory scriptBeanFactory = null;
	
	/*
	public void setScriptBeanFactory(ScriptBeanFactory scriptBeanFactory) {
		this.scriptBeanFactory = scriptBeanFactory;
	}
	*/

	public void setServiceContext(ApplicationContext serviceContext){
		this.serviceContext = serviceContext;
	}
	
	public void invoke(Call call) {
		invoke(call, serviceContext);
	}
	
	/*
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	private Object[] findMethodWithExactParameters(Object service, String methodName, Object[] args)
	{
		int numParams = (args==null) ? 0 : args.length;
		List methods = ConversionUtils.findMethodsByNameAndNumParams(service, methodName, numParams);
		log.debug("Found " + methods.size() + " methods");
		if (methods.isEmpty())
			return new Object[]{null, null};
		else if (methods.size() > 1) {
			log.debug("Multiple methods found with same name and parameter count.");
			log.debug("Parameter conversion will be attempted in order.");
		}
		
		Method method = null;
		Object[] params = null;
		for(int i=0; i<methods.size(); i++){
			try {
				method = (Method) methods.get(i);
				params = ConversionUtils.convertParams(args, method.getParameterTypes());
				return new Object[]{method, params};
			} catch (Exception ex){
				log.debug("Parameter conversion failed", ex);
			}
		}
		
		return new Object[]{null, null};
	}
	
	/*
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	private Object[] findMethodWithListParameters(Object service, String methodName, Object[] args)
	{
		List methods = ConversionUtils.findMethodsByNameAndNumParams(service, methodName, 1);
		log.debug("Found " + methods.size() + " methods");
		if (methods.isEmpty())
			return new Object[]{null, null};
		else if (methods.size() > 1) {
			log.debug("Multiple methods found with same name and parameter count.");
			log.debug("Parameter conversion will be attempted in order.");
		}
		
		ArrayList argsList = new ArrayList();
		if(args!=null){
			for(int i=0; i<args.length; i++){
				argsList.add(args[0]);
			}
		}
		args = new Object[]{argsList};

		Method method = null;
		Object[] params = null;
		for(int i=0; i<methods.size(); i++){
			try {
				method = (Method) methods.get(i);
				params = ConversionUtils.convertParams(args, method.getParameterTypes());
				return new Object[]{method, params};
			} catch (Exception ex){
				log.debug("Parameter conversion failed", ex);
			}
		}
		
		return new Object[]{null, null};
	}
	
	public void invoke(Call call, ApplicationContext serviceContext) {
		
		String serviceName = call.getServiceName();
		String methodName = call.getServiceMethodName();
		log.debug("Service name " + serviceName);
		log.debug("Service method " + methodName);
		
		Object service = null;
		
		if(serviceContext.containsBean(serviceName)){
			service = serviceContext.getBean(serviceName);
		} 
		
		/*
		if(service == null && serviceContext.containsBean("scriptBeanFactory")){
			scriptBeanFactory = (ScriptBeanFactory) serviceContext.getBean("scriptBeanFactory");
		}
		
		if(service == null && scriptBeanFactory !=null) {
			// lets see if its a script.
			service = scriptBeanFactory.getBean(serviceName);
		}
		*/

		if(service == null) {
			call.setException(new ServiceNotFoundException(serviceName));
			call.setStatus(Call.STATUS_SERVICE_NOT_FOUND);
			log.warn("Service not found: "+serviceName);
			return; // EXIT
		} else {
			log.debug("Service found: "+serviceName);
		}
		
		
		Object[] args = call.getArguments();
		for (int i=0; i<args.length; i++) {
			log.debug("   "+i+" => "+args[i]);
		}
		
		Object[] methodResult = null;
		
		methodResult = findMethodWithExactParameters(service, methodName, args);
		if (methodResult.length == 0 || methodResult[0] == null)
			methodResult = findMethodWithListParameters(service, methodName, args);
		
		if (methodResult.length == 0 || methodResult[0] == null) {
			call.setStatus(Call.STATUS_METHOD_NOT_FOUND);
			call.setException(new MethodNotFoundException(methodName));
			return;
		}
		
		Object result = null;
		Method method = (Method) methodResult[0];
		Object[] params = (Object[]) methodResult[1];
		
		try {
			log.debug("Invoking method: "+method.toString());
			if (method.getReturnType() == Void.class) {
				method.invoke(service, params);
				call.setStatus(Call.STATUS_SUCCESS_VOID);
			} else {
				result = method.invoke(service, params);
				call.setResult(result);
				call.setStatus( result==null ? Call.STATUS_SUCCESS_NULL : Call.STATUS_SUCCESS_RESULT );
			}
		} catch (IllegalAccessException accessEx){
			call.setException(accessEx);
			call.setStatus(Call.STATUS_ACCESS_DENIED);
			log.error("Service invocation error",accessEx);
		} catch (InvocationTargetException invocationEx){
			call.setException(invocationEx);
			call.setStatus(Call.STATUS_INVOCATION_EXCEPTION);
			log.error("Service invocation error",invocationEx);
		} catch (Exception ex){
			call.setException(ex);
			call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
			log.error("Service invocation error",ex);
		}

	
	}

}
