package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IConnection;

public class ServiceUtils {

	private static final Log log = LogFactory.getLog(ServiceUtils.class);

	/**
	 * Returns (method, params) for the given service or (null, null) if not
	 * method was found.
	 */
	public static Object[] findMethodWithExactParameters(Object service,
			String methodName, List args) {
		Object[] arguments = new Object[args.size()];
		for (int i = 0; i < args.size(); i++) {
			arguments[i] = args.get(i);
		}

		return findMethodWithExactParameters(service, methodName, arguments);
	}

	/**
	 * Returns (method, params) for the given service or (null, null) if not
	 * method was found. XXX use ranking for method matching rather than exact
	 * type matching plus type conversion.
	 */
	public static Object[] findMethodWithExactParameters(Object service,
			String methodName, Object[] args) {
		int numParams = (args == null) ? 0 : args.length;
		List methods = ConversionUtils.findMethodsByNameAndNumParams(service,
				methodName, numParams);
		if (log.isDebugEnabled()) {
			log.debug("Found " + methods.size() + " methods");
		}
		if (methods.isEmpty()) {
			return new Object[] { null, null };
		} else if (methods.size() > 1) {
			log
					.debug("Multiple methods found with same name and parameter count.");
			log.debug("Parameter conversion will be attempted in order.");
		}

		Method method = null;
		Object[] params = null;

		// First search for method with exact parameters
		for (int i = 0; i < methods.size(); i++) {
			method = (Method) methods.get(i);
			boolean valid = true;
			Class[] paramTypes = method.getParameterTypes();
			for (int j = 0; j < args.length; j++) {
				if ((args[j] == null && paramTypes[j].isPrimitive())
						|| (args[j] != null && !args[j].getClass().equals(
								paramTypes[j]))) {
					valid = false;
					break;
				}
			}

			if (valid) {
				return new Object[] { method, args };
			}
		}

		// Then try to convert parameters
		for (int i = 0; i < methods.size(); i++) {
			try {
				method = (Method) methods.get(i);
				params = ConversionUtils.convertParams(args, method
						.getParameterTypes());
				if (args.length > 0 && (args[0] instanceof IConnection)
						&& (!(params[0] instanceof IConnection))) {
					// Don't convert first IConnection parameter
					continue;
				}

				return new Object[] { method, params };
			} catch (Exception ex) {
				log.debug("Parameter conversion failed for " + method);
			}
		}

		return new Object[] { null, null };
	}

	/**
	 * Returns (method, params) for the given service or (null, null) if not
	 * method was found.
	 */
	public static Object[] findMethodWithListParameters(Object service,
			String methodName, List args) {
		Object[] arguments = new Object[args.size()];
		for (int i = 0; i < args.size(); i++) {
			arguments[i] = args.get(i);
		}

		return findMethodWithListParameters(service, methodName, arguments);
	}

	/**
	 * Returns (method, params) for the given service or (null, null) if not
	 * method was found.
	 */
	public static Object[] findMethodWithListParameters(Object service,
			String methodName, Object[] args) {
		List methods = ConversionUtils.findMethodsByNameAndNumParams(service,
				methodName, 1);
		if (log.isDebugEnabled()) {
			log.debug("Found " + methods.size() + " methods");
		}
		if (methods.isEmpty()) {
			return new Object[] { null, null };
		} else if (methods.size() > 1) {
			log.debug("Multiple methods found with same name and parameter count.");
			log.debug("Parameter conversion will be attempted in order.");
		}

		ArrayList argsList = new ArrayList();
		if (args != null) {
			for (Object element : args) {
				argsList.add(element);
			}
		}
		args = new Object[] { argsList };

		Method method = null;
		Object[] params = null;
		for (int i = 0; i < methods.size(); i++) {
			try {
				method = (Method) methods.get(i);
				params = ConversionUtils.convertParams(args, method
						.getParameterTypes());
				if (argsList.size() > 0
						&& (argsList.get(0) instanceof IConnection)
						&& (!(params[0] instanceof IConnection))) {
					// Don't convert first IConnection parameter
					continue;
				}

				return new Object[] { method, params };
			} catch (Exception ex) {
				log.debug("Parameter conversion failed", ex);
			}
		}

		return new Object[] { null, null };
	}

}
