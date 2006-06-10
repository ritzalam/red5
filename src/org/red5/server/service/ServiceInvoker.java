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
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceHandlerProvider;
import org.red5.server.api.service.IServiceInvoker;

public class ServiceInvoker  implements IServiceInvoker {

	private static final Log log = LogFactory.getLog(ServiceInvoker.class);
	
	public static final String SERVICE_NAME = "serviceInvoker";
	
	//protected ScriptBeanFactory scriptBeanFactory = null;
	
	/*
	public void setScriptBeanFactory(ScriptBeanFactory scriptBeanFactory) {
		this.scriptBeanFactory = scriptBeanFactory;
	}
	*/

	
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
		
		// First search for method with exact parameters
		for(int i=0; i<methods.size(); i++){
			method = (Method) methods.get(i);
			boolean valid = true;
			Class[] paramTypes = method.getParameterTypes(); 
			for (int j=0; j<args.length; j++) {
				if (!args[j].getClass().equals(paramTypes[j])) {
					valid = false;
					break;
				}
			}
			
			if (valid)
				return new Object[]{method, args};
		}
		
		// Then try to convert parameters
		for(int i=0; i<methods.size(); i++){
			try {
				method = (Method) methods.get(i);
				params = ConversionUtils.convertParams(args, method.getParameterTypes());
				return new Object[]{method, params};
			} catch (Exception ex){
				log.debug("Parameter conversion failed for " + method);
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
				argsList.add(args[i]);
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
	
	/**
	 * Lookup a handler for the passed service name in the given scope.
	 * 
	 * Resolution order is:
	 * <ol>
	 * <li>a custom registered handler</li>
	 * <li>a handler in the context</li>
	 * </ol>
	 *  
	 * @param scope
	 * @param serviceName
	 * @return
	 * @see org.red5.server.api.service.IServiceHandlerProvider#registerServiceHandler(String, Object)
	 */
	private Object getServiceHandler(IScope scope, String serviceName) {
		// Get application scope handler first
		Object service = scope.getHandler();
		if (serviceName == null || serviceName.equals(""))
			// No service requested, return application scope handler
			return service;
		
		if (service instanceof IServiceHandlerProvider) {
			// Check for registered service handler
			Object handler = ((IServiceHandlerProvider) service).getServiceHandler(serviceName);
			if (handler != null)
				// The application registered a custom handler, return it.
				return handler;
		}

		// NOTE: here would the scripting support integrate...
		
		// Maybe the context has a service with the given name?
		service = scope.getContext().lookupService(serviceName);
		if (service != null)
			return service;
		
		// Requested service does not exist.
		return null;
	}
	
	public void invoke(IServiceCall call, IScope scope) {
		String serviceName = call.getServiceName();
		
		log.debug("Service name " + serviceName);
		Object service = getServiceHandler(scope, serviceName);
		
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
			return;
		} else {
			log.debug("Service found: "+serviceName);
		}
		
		invoke(call, service);
	}

	public void invoke(IServiceCall call, Object service) {
		String methodName = call.getServiceMethodName();
		
		Object[] args = call.getArguments();
		if (args != null) {
			for (int i=0; i<args.length; i++) {
				log.debug("   "+i+" => "+args[i]);
			}
		}
		
		Object[] methodResult = null;
		
		methodResult = findMethodWithExactParameters(service, methodName, args);
		if (methodResult.length == 0 || methodResult[0] == null)
			methodResult = findMethodWithListParameters(service, methodName, args);
		
		if (methodResult.length == 0 || methodResult[0] == null) {
			log.error("Method " + methodName + " not found in " + service);
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
				log.debug("result: "+result);
				if (call instanceof IPendingServiceCall)
					((IPendingServiceCall) call).setResult(result);
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
