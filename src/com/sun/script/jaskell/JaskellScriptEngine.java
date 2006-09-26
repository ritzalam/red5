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
 * JaskellScriptEngine.java
 * @author A. Sundararajan
 */
package com.sun.script.jaskell;

import java.io.IOException;
import java.io.Reader;
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

import jfun.jaskell.DefaultResolver;
import jfun.jaskell.Jaskell;
import jfun.jaskell.JaskellException;
import jfun.jaskell.Tuple;
import jfun.jaskell.ast.Expr;
import jfun.jaskell.ast.FunDef;
import jfun.jaskell.function.Function;
import jfun.parsec.ParserException;

public class JaskellScriptEngine extends GenericScriptEngine implements
		Compilable, Invocable {

	private static final Object[] EMPTY_ARGS = new Object[0];

	private Jaskell runtime;

	// lazily initialized factory
	private volatile ScriptEngineFactory factory;

	public JaskellScriptEngine() {
		this(null);
	}

	JaskellScriptEngine(ScriptEngineFactory factory) {
		this.factory = factory;
		this.runtime = Jaskell.defaultInstance(getClassLoader());
		this.runtime = runtime.importStandardClasses().importPrelude(true);
	}

	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
		return eval(readFully(reader), context);
	}

	public Object eval(String script, ScriptContext ctx) throws ScriptException {
		return evalExpr(compileCode(script, ctx), ctx);
	}

	public Namespace createNamespace() {
		return new SimpleNamespace();
	}

	public ScriptEngineFactory getFactory() {
		if (factory == null) {
			synchronized (this) {
				if (factory == null) {
					factory = new JaskellScriptEngineFactory();
				}
			}
		}
		return factory;
	}

	// javax.script.Compilable methods
	private class JaskellCompiledScript extends CompiledScript {
		Expr expr;

		private JaskellCompiledScript(Expr expr) {
			this.expr = expr;
		}

		private JaskellCompiledScript() {
			this(null);
		}

		@Override
		public Object eval(ScriptContext ctx) throws ScriptException {
			if (expr != null) {
				return evalExpr(expr, ctx);
			} else {
				return null;
			}
		}

		@Override
		public ScriptEngine getEngine() {
			return JaskellScriptEngine.this;
		}
	};

	public CompiledScript compile(String script) throws ScriptException {
		return new JaskellCompiledScript(compileCode(script, context));
	}

	public CompiledScript compile(Reader reader) throws ScriptException {
		return compile(readFully(reader));
	}

	// javax.script.Invocable methods.
	public Object invokeFunction(String name, Object... args)
			throws ScriptException, NoSuchMethodException {
		if (name == null) {
			throw new NullPointerException("method name is null");
		}

		Object tmp = getCurrentRuntime(context).eval(name);
		Function f;
		if (tmp instanceof Function) {
			f = (Function) tmp;
		} else {
			throw new NoSuchMethodException();
		}

		Object result;
		if (args == null || args.length == 0) {
			result = f.f(jfun.util.List.nil);
		} else {
			result = f.apply(args);
		}
		return result;
	}

	public Object invokeMethod(Object thiz, String name, Object... args)
			throws ScriptException {
		if (thiz == null) {
			throw new IllegalArgumentException("script object is null");
		}
		if (name == null) {
			throw new NullPointerException("method name is null");
		}

		return invokeMethodImpl(thiz, name, args, Object.class);
	}

	public Object getInterface(Class clazz) {
		return makeInterface(null, clazz);
	}

	public <T> T getInterface(Object thiz, Class<T> clasz) {
		if (thiz == null) {
			throw new IllegalArgumentException("script object is null");
		}
		return makeInterface(thiz, clasz);
	}

	// -- Internals only below this point

	private static class ScriptContextResolver extends DefaultResolver {
		private ScriptContext ctx;

		private ScriptContextResolver(ScriptContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public Object resolveVar(String name, Object def) {
			synchronized (ctx) {
				int scope = ctx.getAttributesScope(name);
				if (scope != -1) {
					return ctx.getAttribute(name);
				}
			} // fall thru..
			return super.resolveVar(name, def);
		}
	}

	private Jaskell getCurrentRuntime(ScriptContext ctx) {
		return runtime.setResolver(new ScriptContextResolver(ctx));
	}

	public Object call(String methodName, Object[] args) throws ScriptException {
		return invokeMethod(null, methodName, args);
	}

	public Object call(String methodName, Object thiz, Object[] args)
			throws ScriptException {
		return invokeMethod(thiz, methodName, args);
	}

	// invokes the specified method/function on the given object.
	private Object invokeMethodImpl(Object thiz, String name, Object[] args,
			Class returnType) throws ScriptException {
		Function f;
		Object result;
		Jaskell jaskell = getCurrentRuntime(context);
		Object tmp = jaskell.eval("\\obj->obj." + name);
		if (tmp instanceof Function) {
			f = (Function) tmp;
		} else {
			throw new ScriptException("No such method: " + name);
		}

		tmp = f.f(thiz);
		if (tmp instanceof Function) {
			f = (Function) tmp;
		} else {
			throw new ScriptException("No such method: " + name);
		}

		if (thiz instanceof Tuple) {
			if (args == null || args.length == 0) {
				result = f.f(jfun.util.List.nil);
			} else {
				result = f.apply(args);
			}
		} else {
			if (args == null) {
				args = EMPTY_ARGS;
			}
			result = f.f(args);
		}

		if (returnType != Object.class) {
			result = Jaskell.castToJava(returnType, result, "conversion");
		}
		return result;
	}

	private <T> T makeInterface(Object obj, Class<T> clazz) {
		final Object thiz = obj;
		if (clazz == null || !clazz.isInterface()) {
			throw new IllegalArgumentException("interface Class expected");
		}
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
				new Class[] { clazz }, new InvocationHandler() {
					public Object invoke(Object proxy, Method m, Object[] args)
							throws Throwable {
						return invokeMethodImpl(thiz, m.getName(), args, m
								.getReturnType());
					}
				});
	}

	private ClassLoader getClassLoader() {
		// check whether thread context loader can "see" Jaskell class
		ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
		try {
			Class c = ctxtLoader.loadClass("jfun.jaskell.Jaskell");
			if (c == Jaskell.class) {
				return ctxtLoader;
			}
		} catch (ClassNotFoundException cnfe) {
		}
		// exception was thrown or we get wrong class
		return Jaskell.class.getClassLoader();
	}

	private String readFully(Reader reader) throws ScriptException {
		char[] arr = new char[8 * 1024]; // 8K at a time
		StringBuffer buf = new StringBuffer();
		int numChars;
		try {
			while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
				buf.append(arr, 0, numChars);
			}
		} catch (IOException exp) {
			throw new ScriptException(exp);
		}
		return buf.toString();
	}

	private static final String MODULE_ID = "jaskell_engine";

	private static final String MODULE_NAME = "Jaskell Script Engine";

	private static final String GLOBALS_TUPLE = "globals-tuple";

	private Expr compileCode(String code, ScriptContext ctx)
			throws ScriptException {
		ctx.setAttribute("context", ctx, ScriptContext.ENGINE_SCOPE);
		try {
			Jaskell jaskell = getCurrentRuntime(ctx);
			Object parsed = Jaskell
					.parseExprOrLib(MODULE_ID, MODULE_NAME, code);
			if (parsed instanceof Expr) {
				return Jaskell.compileExpr((Expr) parsed);
			} else {
				Tuple tuple = jaskell.evalLib(Jaskell
						.compileLib((FunDef[]) parsed));
				if (tuple.containsKey("jaskell")) {
					tuple = tuple.remove("jaskell");
				}
				Object oldTuple = ctx.getAttribute(GLOBALS_TUPLE,
						ScriptContext.ENGINE_SCOPE);
				if (oldTuple instanceof Tuple) {
					tuple = Tuple.includesTuple((Tuple) oldTuple, tuple);
				}
				synchronized (ctx) {
					ctx.setAttribute(GLOBALS_TUPLE, tuple,
							ScriptContext.ENGINE_SCOPE);
				}
				return null;
			}
		} catch (ParserException pexp) {
			throw new ScriptException(pexp);
		} catch (JaskellException jexp) {
			throw new ScriptException(jexp);
		}
	}

	private Object evalExpr(Expr expr, ScriptContext ctx)
			throws ScriptException {
		if (expr == null) {
			return null;
		}

		ctx.setAttribute("context", ctx, ScriptContext.ENGINE_SCOPE);
		Object tmp = ctx
				.getAttribute(GLOBALS_TUPLE, ScriptContext.ENGINE_SCOPE);
		try {
			Tuple tuple = (tmp instanceof Tuple) ? (Tuple) tmp : null;
			Jaskell jaskell = getCurrentRuntime(ctx);
			jaskell = jaskell.importTuple(null, tuple);
			return jaskell.eval(expr);
		} catch (JaskellException jexp) {
			throw new ScriptException(jexp);
		}
	}
}