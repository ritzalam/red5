/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved. 
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * JRubyScriptEngine.java
 * @author A. Sundararajan
 * @author Roberto Chinnici
 */

package com.sun.script.jruby;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.GenericScriptEngine;
import javax.script.Invocable;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleNamespace;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.ast.Node;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ReadonlyAccessor;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;

public class JRubyScriptEngine extends GenericScriptEngine implements
		Compilable, Invocable {

	// my factory, may be null
	private ScriptEngineFactory factory;

	private IRuby runtime;

	public JRubyScriptEngine() {
		init(System.getProperty("com.sun.script.jruby.loadpath"));
	}

	public JRubyScriptEngine(String loadPath) {
		init(loadPath);
	}

	// my implementation for CompiledScript
	private class JRubyCompiledScript extends CompiledScript {
		// my compiled code
		private Node node;

		JRubyCompiledScript(Node node) {
			this.node = node;
		}

		@Override
		public ScriptEngine getEngine() {
			return JRubyScriptEngine.this;
		}

		@Override
		public Object eval(ScriptContext ctx) throws ScriptException {
			return evalNode(node, ctx);
		}
	}

	// Compilable methods
	public CompiledScript compile(String script) throws ScriptException {
		Node node = compileScript(script, context);
		return new JRubyCompiledScript(node);
	}

	public CompiledScript compile(Reader reader) throws ScriptException {
		Node node = compileScript(reader, context);
		return new JRubyCompiledScript(node);
	}

	// Invocable methods
	public Object invokeFunction(String name, Object... args)
			throws ScriptException, NoSuchMethodException {
		return invokeImpl(null, name, args, Object.class);
	}

	public Object invokeMethod(Object obj, String name, Object... args)
			throws ScriptException {
		if (obj == null) {
			throw new IllegalArgumentException("script object is null");
		}
		return invokeImpl(obj, name, args, Object.class);
	}

	public <T> T getInterface(Object obj, Class<T> clazz) {
		if (obj == null) {
			throw new IllegalArgumentException("script object is null");
		}
		return makeInterface(obj, clazz);
	}

	public Object getInterface(Class clazz) {
		return makeInterface(null, clazz);
	}

	private <T> T makeInterface(Object obj, Class<T> clazz) {
		if (clazz == null || !clazz.isInterface()) {
			throw new IllegalArgumentException("interface Class expected");
		}
		final Object thiz = obj;
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args)
							throws Throwable {
						return invokeImpl(thiz, m.getName(), args, m
								.getReturnType());
					}
				});
	}

	// ScriptEngine methods
	public synchronized Object eval(String str, ScriptContext ctx)
			throws ScriptException {
		Node node = compileScript(str, ctx);
		return evalNode(node, ctx);
	}

	public synchronized Object eval(Reader reader, ScriptContext ctx)
			throws ScriptException {
		Node node = compileScript(reader, ctx);
		return evalNode(node, ctx);
	}

	public ScriptEngineFactory getFactory() {
		synchronized (this) {
			if (factory == null) {
				factory = new JRubyScriptEngineFactory();
			}
		}
		return factory;
	}

	public Namespace createNamespace() {
		return new SimpleNamespace();
	}

	// package-private methods
	void setFactory(ScriptEngineFactory factory) {
		this.factory = factory;
	}

	// internals only below this point

	private Object rubyToJava(IRubyObject value) {
		return rubyToJava(value, Object.class);
	}

	private Object rubyToJava(IRubyObject value, Class type) {
		return JavaUtil.convertArgument(Java.ruby_to_java(runtime.getObject(),
				value), type);
	}

	private IRubyObject javaToRuby(Object value) {
		if (value instanceof IRubyObject) {
			return (IRubyObject) value;
		}
		IRubyObject result = JavaUtil.convertJavaToRuby(runtime, value);
		if (result instanceof JavaObject) {
			return runtime.getModule("JavaUtilities")
					.callMethod("wrap", result);
		}
		return result;
	}

	private synchronized Node compileScript(String script, ScriptContext ctx)
			throws ScriptException {
		GlobalVariables oldGlobals = runtime.getGlobalVariables();
		try {
			setGlobalVariables(ctx);
			String filename = (String) ctx.getAttribute(ScriptEngine.FILENAME);
			if (filename == null) {
				filename = "<unknown>";
			}
			return runtime.parse(script, filename);
		} catch (Exception exp) {
			throw new ScriptException(exp);
		} finally {
			if (oldGlobals != null) {
				setGlobalVariables(oldGlobals);
			}
		}
	}

	private synchronized Node compileScript(Reader reader, ScriptContext ctx)
			throws ScriptException {
		GlobalVariables oldGlobals = runtime.getGlobalVariables();
		try {
			setGlobalVariables(ctx);
			String filename = (String) ctx.getAttribute(ScriptEngine.FILENAME);
			if (filename == null) {
				filename = "<unknown>";
			}
			return runtime.parse(reader, filename);
		} catch (Exception exp) {
			throw new ScriptException(exp);
		} finally {
			if (oldGlobals != null) {
				setGlobalVariables(oldGlobals);
			}
		}
	}

	private void setGlobalVariables(final ScriptContext ctx) {
		ctx.setAttribute("$context", ctx, ScriptContext.ENGINE_SCOPE);
		setGlobalVariables(new GlobalVariables(runtime) {
			GlobalVariables parent = runtime.getGlobalVariables();

			@Override
			public void define(String name, IAccessor accessor) {
				assert name != null;
				assert accessor != null;
				assert name.startsWith("$");
				synchronized (ctx) {
					Namespace engineScope = ctx
							.getNamespace(ScriptContext.ENGINE_SCOPE);
					engineScope.put(name, new GlobalVariable(accessor));
				}
			}

			@Override
			public void defineReadonly(String name, IAccessor accessor) {
				assert name != null;
				assert accessor != null;
				assert name.startsWith("$");
				synchronized (ctx) {
					Namespace engineScope = ctx
							.getNamespace(ScriptContext.ENGINE_SCOPE);
					engineScope.put(name, new GlobalVariable(
							new ReadonlyAccessor(name, accessor)));
				}
			}

			@Override
			public boolean isDefined(String name) {
				assert name != null;
				assert name.startsWith("$");
				synchronized (ctx) {
					boolean defined = ctx.getAttributesScope(name) != -1;
					if (defined) {
						return true;
					} else {
						return parent.isDefined(name);
					}
				}
			}

			@Override
			public void alias(String name, String oldName) {
				assert name != null;
				assert oldName != null;
				assert name.startsWith("$");
				assert oldName.startsWith("$");

				if (runtime.getSafeLevel() >= 4) {
					throw runtime
							.newSecurityError("Insecure: can't alias global variable");
				}

				synchronized (ctx) {
					int scope = ctx.getAttributesScope(name);
					if (scope == -1 || scope == 0) {
						scope = ScriptContext.ENGINE_SCOPE;
					}

					IRubyObject value = get(oldName);
					ctx.setAttribute(name, rubyToJava(value), scope);
				}
			}

			@Override
			public IRubyObject get(String name) {
				assert name != null;
				assert name.startsWith("$");
				synchronized (ctx) {
					int scope = ctx.getAttributesScope(name);
					if (scope == -1 || scope == 0) {
						return parent.get(name);
					} else {
						Object obj = ctx.getAttribute(name, scope);
						if (obj instanceof IAccessor) {
							return ((IAccessor) obj).getValue();
						} else {
							return javaToRuby(obj);
						}
					}
				}
			}

			@Override
			public IRubyObject set(String name, IRubyObject value) {
				assert name != null;
				assert name.startsWith("$");

				if (runtime.getSafeLevel() >= 4) {
					throw runtime
							.newSecurityError("Insecure: can't change global variable value");
				}

				synchronized (ctx) {
					int scope = ctx.getAttributesScope(name);
					if (scope == -1 || scope == 0) {
						scope = ScriptContext.ENGINE_SCOPE;
					}
					IRubyObject oldValue = get(name);
					Object obj = ctx.getAttribute(name, scope);
					if (obj instanceof IAccessor) {
						((IAccessor) obj).setValue(value);
					} else {
						ctx.setAttribute(name, rubyToJava(value), scope);
					}
					return oldValue;
				}
			}

			@Override
			public Iterator getNames() {
				List list = new ArrayList();
				synchronized (ctx) {
					// used to have 0 100 and 200
					int[] scopes = { 100, 200 };
					for (int scope : scopes) {
						Namespace b = ctx.getNamespace(scope);
						if (b != null) {
							for (String key : (Set<String>) b.keySet()) {
								list.add(key);
							}
						}
					}
				}
				for (Iterator names = parent.getNames(); names.hasNext();) {
					list.add(names.next());
				}
				return Collections.unmodifiableList(list).iterator();
			}
		});
	}

	private void setGlobalVariables(GlobalVariables globals) {
		// !!HACK!! org.jruby.Ruby does not expose setGlobalVariables() method.
		// Also, this class is final and therefore can not be extendeded.
		// I am using reflection to set the field value!!
		try {
			Field globalsField = Ruby.class.getDeclaredField("globalVariables");
			globalsField.setAccessible(true);
			globalsField.set(runtime, globals);
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	private synchronized Object evalNode(Node node, ScriptContext ctx)
			throws ScriptException {
		GlobalVariables oldGlobals = runtime.getGlobalVariables();
		try {
			setGlobalVariables(ctx);
			return rubyToJava(runtime.eval(node));
		} catch (Exception exp) {
			throw new ScriptException(exp);
		} finally {
			if (oldGlobals != null) {
				setGlobalVariables(oldGlobals);
			}
		}
	}

	private void init(String loadPath) {
		runtime = Ruby.getDefaultInstance();
		if (loadPath == null) {
			loadPath = System.getProperty("java.class.path");
		}
		List list = Arrays.asList(loadPath.split(File.pathSeparator));
		runtime.getLoadService().init(list);
		runtime.getLoadService().require("java");
	}

	public Object call(String methodName, Object[] args) throws ScriptException {
		return invokeMethod(null, methodName, args);
	}

	public Object call(String methodName, Object thiz, Object[] args)
			throws ScriptException {
		return invokeMethod(thiz, methodName, args);
	}

	private Object invokeImpl(final Object obj, String method, Object[] args,
			Class returnType) throws ScriptException {
		if (method == null) {
			throw new NullPointerException("method name is null");
		}

		try {
			IRubyObject rubyRecv = obj != null ? JavaUtil.convertJavaToRuby(
					runtime, obj) : runtime.getTopSelf();

			IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime,
					args);

			// Create Ruby proxies for any input arguments that are not
			// primitives.
			IRubyObject javaUtilities = runtime.getObject().getConstant(
					"JavaUtilities");
			for (int i = 0; i < rubyArgs.length; i++) {
				IRubyObject tmp = rubyArgs[i];
				if (tmp instanceof JavaObject) {
					rubyArgs[i] = javaUtilities.callMethod("wrap", tmp);
				}
			}

			IRubyObject result = rubyRecv.callMethod(method, rubyArgs);
			return rubyToJava(result, returnType);
		} catch (Exception exp) {
			throw new ScriptException(exp);
		}
	}
}
