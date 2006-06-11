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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceInvoker;

public class ServiceInvoker  implements IServiceInvoker {

	private static final Log log = LogFactory.getLog(ServiceInvoker.class);
	
	public static final String SERVICE_NAME = "serviceInvoker";
	private Set<IServiceResolver> serviceResolvers = new HashSet<IServiceResolver>();

	public void setServiceResolvers(Set<IServiceResolver> resolvers) {
		serviceResolvers = resolvers;
	}
	
	/**
	 * Lookup a handler for the passed service name in the given scope.
	 * 
	 * @param scope
	 * @param serviceName
	 * @return
	 */
	private Object getServiceHandler(IScope scope, String serviceName) {
		// Get application scope handler first
		Object service = scope.getHandler();
		if (serviceName == null || serviceName.equals(""))
			// No service requested, return application scope handler
			return service;
		
		// Search service resolver that knows about service name
		for (IServiceResolver resolver: serviceResolvers) {
			service = resolver.resolveService(scope, serviceName);
			if (service != null)
				return service;
		}
		
		// Requested service does not exist.
		return null;
	}
	
	public void invoke(IServiceCall call, IScope scope) {
		String serviceName = call.getServiceName();
		
		log.debug("Service name " + serviceName);
		Object service = getServiceHandler(scope, serviceName);
		
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
		methodResult = ServiceUtils.findMethodWithExactParameters(service, methodName, args);
		if (methodResult.length == 0 || methodResult[0] == null)
			methodResult = ServiceUtils.findMethodWithListParameters(service, methodName, args);
		
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
