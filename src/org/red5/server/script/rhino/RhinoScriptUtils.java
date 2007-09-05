/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.script.rhino;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.script.*;

import org.apache.log4j.Logger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for handling Rhino / Javascript objects.
 * 
 * @author Paul Gregoire
 * @since 0.6
 */
public class RhinoScriptUtils {

	private static final Logger log = Logger.getLogger(RhinoScriptUtils.class);

	// keep track of whether we are using pre-java6
	// private static boolean isJava6 = false;

	// ScriptEngine manager
	private static ScriptEngineManager mgr = new ScriptEngineManager();

	// Javascript wrapper
	private static final String jsWrapper = "function Wrapper(obj){return new JSAdapter(){ __has__ : function(name){return true;}, __get__ : function(name){if(name in obj){return obj[name];}else if(typeof(obj['doesNotUnderstand']) == 'function'){return function(){return obj.doesNotUnderstand(name, arguments);}}else{return undefined;}}};}";

	/**
	 * Create a new Rhino-scripted object from the given script source.
	 * 
	 * @param scriptSource
	 *            the script source text
	 * @param interfaces
	 *            the interfaces that the scripted Java object is supposed to
	 *            implement
	 * @param extendedClass
	 * @return the scripted Java object
	 * @throws ScriptCompilationException
	 *             in case of Rhino parsing failure
	 * @throws java.io.IOException
	 */
	public static Object createRhinoObject(String scriptSource,
			Class[] interfaces, Class extendedClass)
			throws ScriptCompilationException, IOException, Exception {
		if (log.isDebugEnabled()) {
			log.debug("Script Engine Manager: " + mgr.getClass().getName());
		}
		ScriptEngine engine = mgr.getEngineByName("javascript");
		// hack to support java5
		// ScriptEngine engine = null;
		// for (ScriptEngineFactory factory : mgr.getEngineFactories()) {
		// if (factory.getEngineName().toLowerCase().matches(
		// ".*(rhino|javascript|ecma).*")) {
		// engine = factory.getScriptEngine();
		// }
		// }
		if (null == engine) {
			log.fatal("Javascript is not supported in this build");
		}
		// set engine scope namespace
		Bindings nameSpace = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		// part2 of java5 hack
		// Map<String, Object> nameSpace = null;
		// use reflection to determine which api is supported
		// Method method = null;
		// try {
		// method = engine.getClass().getMethod("getBindings",
		// new Class[] { int.class });
		// isJava6 = true;
		// } catch (NoSuchMethodException e) {
		// method = engine.getClass().getMethod("getNamespace",
		// new Class[] { int.class });
		// }
		// nameSpace = (Map<String, Object>) method.invoke(engine,
		// new Object[] { ScriptContext.ENGINE_SCOPE });

		// add the logger to the script
		nameSpace.put("log", log);
		// compile the wrapper script
		CompiledScript wrapper = ((Compilable) engine).compile(jsWrapper);
		nameSpace.put("Wrapper", wrapper);

		// get the function name ie. class name / ctor
		String funcName = RhinoScriptUtils.getFunctionName(scriptSource);
		if (log.isDebugEnabled()) {
			log.debug("New script: " + funcName);
		}
		// set the 'filename'
		nameSpace.put(ScriptEngine.FILENAME, funcName);

		if (null != interfaces) {
			nameSpace.put("interfaces", interfaces);
		}

		if (null != extendedClass) {
			if (log.isDebugEnabled()) {
				log.debug("Extended: " + extendedClass.getName());
			}
			nameSpace.put("supa", extendedClass.newInstance());
		}
		//
		// compile the script
		CompiledScript script = ((Compilable) engine).compile(scriptSource);
		// eval the script with the associated namespace
		Object o = null;
		try {
			o = script.eval();
		} catch (Exception e) {
			log.error("Problem evaluating script", e);
		}
		if (log.isDebugEnabled()) {
			log.debug("Result of script call: " + o);
		}
		// script didnt return anything we can use so try the wrapper
		if (null == o) {
			wrapper.eval();
		} else {
			wrapper.eval();
			o = ((Invocable) engine).invokeFunction("Wrapper",
					new Object[] { engine.get(funcName) });
			// if (isJava6) {
			// method = ((Invocable) engine).getClass().getMethod(
			// "invokeFunction",
			// new Class[] { String.class, Object[].class });
			// o = method.invoke(engine, new Object[] { "Wrapper",
			// new Object[] { engine.get(funcName) } });
			// } else {
			// method = ((Invocable) engine).getClass().getMethod("call",
			// new Class[] { String.class, Object[].class });
			// o = method.invoke(engine, new Object[] { "Wrapper",
			// new Object[] { engine.get(funcName) } });
			// }
			if (log.isDebugEnabled()) {
				log.debug("Result of invokeFunction: " + o);
			}
		}
		return Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(),
				interfaces, new RhinoObjectInvocationHandler(engine, o));
	}

	/**
	 * InvocationHandler that invokes a Rhino script method.
	 */
	private static class RhinoObjectInvocationHandler implements
			InvocationHandler {

		private final ScriptEngine engine;

		private final Object instance;

		public RhinoObjectInvocationHandler(ScriptEngine engine, Object instance) {
			this.engine = engine;
			this.instance = instance;
		}

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			Object o = null;
			// ensure a set of args are available
			if (args == null || args.length == 0) {
				args = new Object[] { "" };
			}
			String name = method.getName();
			if (log.isDebugEnabled()) {
				log.debug("Calling: " + name);
			}
			try {
				Method apiMethod = null;
				Invocable invocable = (Invocable) engine;
				if (null == instance) {
					o = invocable.invokeFunction(name, args);
					// if (isJava6) {
					// apiMethod = invocable.getClass().getMethod(
					// "invokeFunction",
					// new Class[] { String.class, Object[].class });
					// o = apiMethod.invoke(invocable, new Object[] { name,
					// args });
					// } else {
					// apiMethod = invocable.getClass().getMethod("call",
					// new Class[] { String.class, Object[].class });
					// o = apiMethod.invoke(invocable, new Object[] { name,
					// args });
					// }
				} else {
					try {
						o = invocable.invokeMethod(instance, name, args);
						// if (isJava6) {
						// apiMethod = invocable.getClass().getMethod(
						// "invokeMethod",
						// new Class[] { Object.class, String.class,
						// Object[].class });
						// o = apiMethod.invoke(invocable, new Object[] {
						// instance, name, args });
						// } else {
						// apiMethod = invocable.getClass().getMethod(
						// "call",
						// new Class[] { Object.class, String.class,
						// Object[].class });
						// o = apiMethod.invoke(invocable, new Object[] {
						// instance, name, args });
						// }
					} catch (NoSuchMethodException nex) {
						log.debug("Method not found: " + name);
						try {
							// try to invoke it directly, this will work if the
							// function is in the engine context
							// ie. the script has been already evaluated
							o = invocable.invokeFunction(name, args);
							// if (isJava6) {
							// apiMethod = invocable.getClass().getMethod(
							// "invokeFunction",
							// new Class[] { String.class,
							// Object[].class });
							// o = apiMethod.invoke(invocable, new Object[] {
							// name, args });
							// } else {
							// apiMethod = invocable.getClass().getMethod(
							// "call",
							// new Class[] { String.class,
							// Object[].class });
							// o = apiMethod.invoke(invocable, new Object[] {
							// name, args });
							// }
						} catch (Exception ex) {
							log.debug("Function not found: " + name);
							Class[] interfaces = (Class[]) engine
									.get("interfaces");
							for (Class clazz : interfaces) {
								// java6 style
								o = invocable.getInterface(engine
										.get((String) engine.get("className")),
										clazz);
								// if (isJava6) {
								// apiMethod = invocable.getClass().getMethod(
								// "getInterface",
								// new Class[] { Object.class,
								// Class.class });
								// o = apiMethod.invoke(invocable,
								// new Object[] {
								// ((String) engine
								// .get("className")),
								// clazz });
								// } else {
								// apiMethod = invocable.getClass().getMethod(
								// "getInterface",
								// new Class[] { Class.class });
								// o = apiMethod.invoke(invocable,
								// new Object[] { clazz });
								// }

								if (null != o) {
									log.debug("Interface return type: "
											+ o.getClass().getName());
									break;
								}
							}
						}
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("Invocable result: " + o);
				}
			} catch (NoSuchMethodException nex) {
				log.warn("Method not found");
			} catch (Throwable t) {
				log.warn(t);
			}
			return o;
		}
	}

	/**
	 * Uses a regex to get the first "function" name, this name is used to
	 * create an instance of the javascript object.
	 * 
	 * @param scriptSource
	 * @return
	 */
	private static String getFunctionName(String scriptSource) {
		String ret = "undefined";
		try {
			ret = scriptSource.replaceAll(
					"[\\S\\w\\s]*?function ([\\w]+)\\(\\)[\\S\\w\\s]+", "$1");
			// if undefined then look for the first var
			if (ret.equals("undefined") || ret.length() > 64) {
				ret = scriptSource.replaceAll(
						"[\\S\\w\\s]*?var ([\\w]+)[\\S\\w\\s]+", "$1");
			}
		} catch (PatternSyntaxException ex) {
			log.error("Syntax error in the regular expression");
		} catch (IllegalArgumentException ex) {
			log
					.error("Syntax error in the replacement text (unescaped $ signs?)");
		} catch (IndexOutOfBoundsException ex) {
			log.error("Non-existent backreference used the replacement text");
		}
		log.debug("Got a function name: " + ret);
		return ret;
	}

}
