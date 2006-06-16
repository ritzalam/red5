package org.red5.server.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceUtils {

	private static final Log log = LogFactory.getLog(ServiceUtils.class);

	/**
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	public static Object[] findMethodWithExactParameters(Object service, String methodName, List args) {
		Object[] arguments = new Object[args.size()];
		for (int i=0; i<args.size(); i++) {
			arguments[i] = args.get(i);
		}
		
		return findMethodWithExactParameters(service, methodName, arguments);
	}

	/**
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	public static Object[] findMethodWithExactParameters(Object service, String methodName, Object[] args) {
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
				if ((args[j]==null && paramTypes[j].isPrimitive()) || (args[j]!=null && !args[j].getClass().equals(paramTypes[j]))) {
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
	
	/**
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	public static Object[] findMethodWithListParameters(Object service, String methodName, List args) {
		Object[] arguments = new Object[args.size()];
		for (int i=0; i<args.size(); i++) {
			arguments[i] = args.get(i);
		}
		
		return findMethodWithListParameters(service, methodName, arguments);
	}
	
	/**
	 * Returns (method, params) for the given service or (null, null) if not method was found.
	 */
	public static Object[] findMethodWithListParameters(Object service, String methodName, Object[] args) {
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
	
}
