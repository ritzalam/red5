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
 * JudoScriptEngine.java
 * @author A. Sundararajan
 */

package com.sun.script.judo;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

import com.judoscript.Expr;
import com.judoscript.JudoUtil;
import com.judoscript.ObjectInstance;
import com.judoscript.RT;
import com.judoscript.RuntimeGlobalContext;
import com.judoscript.Script;
import com.judoscript.ValueSpecial;
import com.judoscript.Variable;
import com.judoscript.bio.JavaObject;
import com.judoscript.parser.helper.ParserHelper;
import com.judoscript.util.LinePrintWriter;

public class JudoScriptEngine extends GenericScriptEngine implements
		Compilable, Invocable {

	// my factory, may be null
	private ScriptEngineFactory factory;

	private volatile Script judoscript;

	// my implementation for CompiledScript
	private class JudoCompiledScript extends CompiledScript {
		// my compiled code
		private Object script;

		JudoCompiledScript(Object script) {
			this.script = script;
		}

		@Override
		public ScriptEngine getEngine() {
			return JudoScriptEngine.this;
		}

		@Override
		public Object eval(ScriptContext ctx) throws ScriptException {
			return evalScript(script, ctx);
		}
	}

	// Compilable methods
	public CompiledScript compile(String code) throws ScriptException {
		return compile(new StringReader(code));
	}

	public CompiledScript compile(Reader reader) throws ScriptException {
		return new JudoCompiledScript(compileScript(reader, context));
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
		synchronized (this) {
			if (judoscript == null) {
				return null;
			}
		}
		if (obj == null) {
			throw new IllegalArgumentException("script object is null");
		}
		return makeInterface(obj, clazz);
	}

	public Object getInterface(Class clazz) {
		return makeInterface(null, clazz);
	}

	private <T> T makeInterface(final Object obj, Class<T> clazz) {
		if (clazz == null || !clazz.isInterface()) {
			throw new IllegalArgumentException("interface Class expected");
		}
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args)
							throws Throwable {
						return invokeImpl(obj, m.getName(), args, m
								.getReturnType());
					}
				});
	}

	// ScriptEngine methods
	public Object eval(String str, ScriptContext ctx) throws ScriptException {
		return eval(new StringReader(str), ctx);
	}

	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
		Object script = compileScript(reader, ctx);
		return evalScript(script, ctx);
	}

	public ScriptEngineFactory getFactory() {
		synchronized (this) {
			if (factory == null) {
				factory = new JudoScriptEngineFactory();
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
	private Object compileScript(Reader reader, ScriptContext ctx)
			throws ScriptException {
		String fileName = (String) ctx.getAttribute(ScriptEngine.FILENAME);
		if (fileName == null) {
			fileName = "<unknown>";
		}
		try {
			return ParserHelper.parse(null, null, reader, null, 0, false);
		} catch (Exception exp) {
			throw new ScriptException(exp);
		}
	}

	private static class MyRuntimeGlobalContext extends RuntimeGlobalContext {
		private BufferedReader in;

		private LinePrintWriter out;

		private LinePrintWriter err;

		private ScriptContext ctx;

		<T extends ScriptContext> void setScriptContext(T ctx) {
			this.ctx = ctx;
			in = new BufferedReader(ctx.getReader());
			out = new LinePrintWriter(ctx.getWriter());
			err = new LinePrintWriter(ctx.getErrorWriter());
		}

		ScriptContext getScriptContext() {
			return ctx;
		}

		@Override
		public Variable resolveVariable(String name) throws Throwable {
			Variable v = super.resolveVariable(name);
			if (v == ValueSpecial.UNDEFINED) {
				synchronized (ctx) {
					int scope = ctx.getAttributesScope(name);
					if (scope != -1) {
						Object obj = ctx.getAttribute(name, scope);
						v = JudoUtil.toVariable(obj);
					}
				}
			}
			return v;
		}

		@Override
		public BufferedReader getIn() {
			return in;
		}

		@Override
		public LinePrintWriter getOut() {
			return out;
		}

		@Override
		public LinePrintWriter getErr() {
			return err;
		}
	}

	private ThreadLocal<MyRuntimeGlobalContext> cache = new ThreadLocal<MyRuntimeGlobalContext>();

	private MyRuntimeGlobalContext getRuntimeContext() {
		MyRuntimeGlobalContext rtc = cache.get();
		if (rtc == null) {
			rtc = new MyRuntimeGlobalContext();
			cache.set(rtc);
			RT.pushContext(rtc);
		}
		return rtc;
	}

	private synchronized void saveScript(Script script) {
		if (judoscript != null) {
			script.acceptDecls(judoscript);
		}
		judoscript = script;
	}

	private Object evalScript(Object script, ScriptContext ctx)
			throws ScriptException {
		MyRuntimeGlobalContext rtc = getRuntimeContext();
		ctx.setAttribute("context", ctx, ScriptContext.ENGINE_SCOPE);
		ScriptContext oldContext = rtc.getScriptContext();
		try {
			saveScript((Script) script);
			rtc.setScriptContext(ctx);
			rtc.setScript(judoscript);
			judoscript.start(rtc);
			return null;
		} catch (Exception e) {
			throw new ScriptException(e);
		} finally {
			if (oldContext != null) {
				rtc.setScriptContext(oldContext);
			}
		}
	}

	private Object judoToJava(Variable value, Class type) throws Throwable {
		if (type.isPrimitive()) {
			if (type == Boolean.TYPE) {
				return new Boolean(value.getBoolValue());
			} else if (type == Byte.TYPE) {
				return new Byte((byte) value.getLongValue());
			} else if (type == Short.TYPE) {
				return new Short((short) value.getLongValue());
			} else if (type == Integer.TYPE) {
				return new Integer((int) value.getLongValue());
			} else if (type == Long.TYPE) {
				return new Long(value.getLongValue());
			} else if (type == Float.TYPE) {
				return new Float((float) value.getDoubleValue());
			} else if (type == Double.TYPE) {
				return new Double(value.getDoubleValue());
			} else if (type == Character.TYPE) {
				String s = value.getStringValue();
				if (s.length() == 1) {
					return new Character(s.charAt(0));
				}
			}
		}
		return value.isNil() ? null : value.getObjectValue();
	}

	public Object call(String methodName, Object[] args) throws ScriptException {
		return invokeMethod(null, methodName, args);
	}

	public Object call(String methodName, Object thiz, Object[] args)
			throws ScriptException {
		return invokeMethod(thiz, methodName, args);
	}

	private Object invokeImpl(Object obj, String name, Object[] args,
			Class returnType) throws ScriptException {
		if (name == null) {
			throw new NullPointerException("method name is null");
		}
		synchronized (this) {
			if (judoscript == null) {
				throw new ScriptException(name);
			}
		}
		ObjectInstance thiz = null;
		if (obj != null) {
			if (obj instanceof ObjectInstance) {
				thiz = (ObjectInstance) obj;
			} else {
				thiz = new JavaObject(obj);
			}
		}
		MyRuntimeGlobalContext rtc = getRuntimeContext();
		context.setAttribute("context", context, ScriptContext.ENGINE_SCOPE);
		ScriptContext oldContext = rtc.getScriptContext();
		try {
			int len = (args == null) ? 0 : args.length;
			Expr[] params = new Expr[len];
			for (int i = 0; i < len; i++) {
				params[i] = (args[i] == null) ? ValueSpecial.NIL : JudoUtil
						.toVariable(args[i]);
			}
			Variable result;
			if (thiz == null) {
				result = judoscript.invoke(name, params, null);
			} else {
				result = thiz.invoke(name, params, null);
			}
			return judoToJava(result, returnType);
		} catch (Exception exp) {
			throw new ScriptException(exp);
		} catch (Throwable t) {
			throw (ScriptException) new ScriptException(t.getMessage())
					.initCause(t);
		} finally {
			if (oldContext != null) {
				rtc.setScriptContext(oldContext);
			}
		}
	}
}
