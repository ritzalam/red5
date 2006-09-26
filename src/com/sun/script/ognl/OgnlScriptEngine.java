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
 * OgnlScriptEngine.java
 * @author A. Sundararajan
 */

package com.sun.script.ognl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.GenericScriptEngine;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleNamespace;

import ognl.NoSuchPropertyException;
import ognl.ObjectPropertyAccessor;
import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlParser;
import ognl.OgnlRuntime;
import ognl.ParseException;
import ognl.PropertyAccessor;
import ognl.TokenMgrError;

public class OgnlScriptEngine extends GenericScriptEngine implements Compilable {

	// my factory, may be null
	private ScriptEngineFactory factory;

	static {
		OgnlRuntime.setPropertyAccessor(ScriptContext.class,
				new PropertyAccessor() {
					private ObjectPropertyAccessor objPropAccess = new ObjectPropertyAccessor();

					public synchronized Object getProperty(Map context,
							Object target, Object name) throws OgnlException {
						ScriptContext ctx = (ScriptContext) target;
						synchronized (ctx) {
							if (ctx.getAttributesScope((String) name) != -1) {
								return ctx.getAttribute((String) name);
							} else {
								throw new NoSuchPropertyException(target, name);
							}
						}
					}

					public synchronized void setProperty(Map context,
							Object target, Object name, Object value)
							throws OgnlException {
						ScriptContext ctx = (ScriptContext) target;
						int scope;
						synchronized (ctx) {
							if ((scope = ctx.getAttributesScope((String) name)) != -1) {
								ctx.setAttribute((String) name, value, scope);
							} else {
								// create a new property in engine scope
								ctx.setAttribute((String) name, value,
										ScriptContext.ENGINE_SCOPE);
							}
						}
					}
				});
	}

	// my implementation for CompiledScript
	private class OgnlCompiledScript extends CompiledScript {
		// parsed Ognl Tree
		private Object tree;

		OgnlCompiledScript(Object tree) {
			this.tree = tree;
		}

		@Override
		public ScriptEngine getEngine() {
			return OgnlScriptEngine.this;
		}

		@Override
		public Object eval(ScriptContext ctx) throws ScriptException {
			return evalTree(tree, ctx);
		}
	}

	public CompiledScript compile(String script) throws ScriptException {
		Object tree = parse(script);
		return new OgnlCompiledScript(tree);
	}

	public CompiledScript compile(Reader reader) throws ScriptException {
		Object tree = parse(reader);
		return new OgnlCompiledScript(tree);
	}

	public Object eval(String str, ScriptContext ctx) throws ScriptException {
		return eval(new StringReader(str), ctx);
	}

	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
		Object expr = parse(reader);
		return evalTree(expr, ctx);
	}

	public ScriptEngineFactory getFactory() {
		synchronized (this) {
			if (factory == null) {
				factory = new OgnlScriptEngineFactory();
			}
		}
		return factory;
	}

	public Namespace createNamespace() {
		return new SimpleNamespace();
	}

	void setFactory(ScriptEngineFactory factory) {
		this.factory = factory;
	}

	private Object parse(String str) throws ScriptException {
		return parse(new StringReader(str));
	}

	private Object parse(Reader reader) throws ScriptException {
		try {
			OgnlParser parser = new OgnlParser(reader);
			return parser.topLevelExpression();
		} catch (ParseException e) {
			throw new ScriptException(e.getMessage());
		} catch (TokenMgrError e) {
			throw new ScriptException(e.getMessage());
		}
	}

	private Object evalTree(Object tree, ScriptContext ctx)
			throws ScriptException {
		Namespace engineScope = ctx.getNamespace(ScriptContext.ENGINE_SCOPE);
		try {
			engineScope.put("engine", this);
			engineScope.put("context", ctx);
			return Ognl.getValue(tree, engineScope, ctx);
		} catch (OgnlException e) {
			throw new ScriptException(e);
		}
	}
}
