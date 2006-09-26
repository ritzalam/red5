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
 * JythonScope.java
 * @author A. Sundararajan
 */

package com.sun.script.jython;

import java.util.Set;

import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;

final class JythonScope extends PyObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ScriptContext ctx;

	JythonScope(ScriptEngine engine, ScriptContext ctx) {
		this.ctx = ctx;
		// global module's name is expected to be 'main'
		__setitem__("__name__", new PyString("main"));
		// JSR-223 requirement: context is exposed as variable
		__setitem__("context", Py.java2py(ctx));
		// expose current engine as another top-level variable
		__setitem__("engine", Py.java2py(engine));
	}

	@Override
	public synchronized PyObject invoke(String name) {
		if (name.equals("keys")) {
			int[] scopes = { 0, 100, 200 };
			// special case for "keys" so that dir() will
			// work for the global "module"
			PyList keys = new PyList();
			synchronized (ctx) {
				// List<Integer> scopes = ctx.getScopes();
				// for (int scope : ctx.getScopes()) {
				for (int scope : scopes) {
					Namespace b = ctx.getNamespace(scope);
					if (b != null) {
						for (String key : (Set<String>) b.keySet()) {
							keys.append(new PyString(key));
						}
					}
				}
			}
			return keys;
		} else {
			return super.invoke(name);
		}
	}

	@Override
	public PyObject __findattr__(String key) {
		return __finditem__(key);
	}

	@Override
	public synchronized PyObject __finditem__(String key) {
		synchronized (ctx) {
			int scope = ctx.getAttributesScope(key);
			if (scope == -1) {
				return null;
			} else {
				Object value = ctx.getAttribute(key, scope);
				return JythonScriptEngine.java2py(value);
			}
		}
	}

	@Override
	public void __setattr__(String key, PyObject value) {
		__setitem__(key, value);
	}

	@Override
	public synchronized void __setitem__(String key, PyObject value) {
		synchronized (ctx) {
			int scope = ctx.getAttributesScope(key);
			if (scope == -1 || scope == 0) {
				scope = ScriptContext.ENGINE_SCOPE;
			}
			Object obj = JythonScriptEngine.py2java(value);
			ctx.setAttribute(key, obj, scope);
		}
	}

	@Override
	public void __delattr__(String key) {
		__delitem__(key);
	}

	@Override
	public synchronized void __delitem__(String key) {
		synchronized (ctx) {
			int scope = ctx.getAttributesScope(key);
			if (scope != -1) {
				ctx.removeAttribute(key, scope);
			}
		}
	}

	@Override
	public String toString() {
		return "<global scope at " + hashCode() + ">";
	}
}
