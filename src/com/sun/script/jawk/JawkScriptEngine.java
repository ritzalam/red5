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
 * JawkScriptEngine.java
 * @author A. Sundararajan
 */

package com.sun.script.jawk;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;

import javax.script.GenericScriptEngine;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleNamespace;

import org.jawk.Awk;

public class JawkScriptEngine extends GenericScriptEngine {

	// pre-defined special variables
	public static final String FS = "FS";

	public static final String ARGUMENTS = "arguments";

	public static final String STDIN = "stdin";

	public static final String STDOUT = "stdout";

	public static final String STDERR = "stderr";

	private static final String[] EMPTY_ARGUMENTS = new String[0];

	// my factory, may be null
	private volatile ScriptEngineFactory factory;

	public JawkScriptEngine() {
		this(null);
	}

	JawkScriptEngine(ScriptEngineFactory factory) {
		this.factory = factory;
	}

	public Object eval(String script, ScriptContext ctx) throws ScriptException {
		Object tmp;
		InputStream stdin;
		PrintStream stdout;
		PrintStream stderr;

		tmp = ctx.getAttribute(STDIN);
		if (tmp instanceof InputStream) {
			stdin = (InputStream) tmp;
		} else {
			stdin = System.in;
		}

		tmp = ctx.getAttribute(STDOUT);
		if (tmp instanceof PrintStream) {
			stdout = (PrintStream) tmp;
		} else {
			stdout = System.out;
		}

		tmp = ctx.getAttribute(STDERR);
		if (tmp instanceof PrintStream) {
			stderr = (PrintStream) tmp;
		} else {
			stderr = System.err;
		}

		String[] arguments;
		tmp = ctx.getAttribute(ARGUMENTS);
		if (tmp instanceof String[]) {
			arguments = (String[]) tmp;
		} else {
			arguments = EMPTY_ARGUMENTS;
		}

		String fs;
		tmp = ctx.getAttribute(FS);
		if (tmp instanceof String) {
			fs = (String) tmp;
		} else {
			fs = null;
		}

		final int len = arguments.length + 1 + ((fs != null) ? 2 : 0);
		String[] args = new String[len];
		int index;
		if (fs != null) {
			args[0] = "-F";
			args[1] = fs;
			index = 2;
		} else {
			index = 0;
		}
		args[index] = script;
		System.arraycopy(arguments, 0, args, index + 1, arguments.length);

		try {
			new Awk(args, stdin, stdout, stderr);
			return stdout;
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
		return eval(readFully(reader), ctx);
	}

	public ScriptEngineFactory getFactory() {
		if (factory == null) {
			synchronized (this) {
				if (factory == null) {
					factory = new JawkScriptEngineFactory();
				}
			}
		}
		return factory;
	}

	public Namespace createNamespace() {
		return new SimpleNamespace();
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
}
