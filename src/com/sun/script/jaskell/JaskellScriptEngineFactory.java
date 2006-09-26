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
 * JaskellScriptEngineFactory.java
 * @author A. Sundararajan
 */

package com.sun.script.jaskell;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class JaskellScriptEngineFactory implements ScriptEngineFactory {

	public String getEngineName() {
		return "jaskell";
	}

	public String getEngineVersion() {
		return "1.0";
	}

	public String getLanguageName() {
		return "jaskell";
	}

	public String getLanguageVersion() {
		return "1.0";
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String[] getMimeTypes() {
		return mimeTypes;
	}

	public String[] getNames() {
		return names;
	}

	public Object getParameter(String key) {
		if (key.equals(ScriptEngine.ENGINE)) {
			return getEngineName();
		} else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
			return getEngineVersion();
		} else if (key.equals(ScriptEngine.LANGUAGE)) {
			return getLanguageName();
		} else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
			return getLanguageVersion();
		} else if (key.equals("THREADING")) {
			return "MULTITHREADED";
		} else {
			throw new IllegalArgumentException("Invalid key");
		}
	}

	public ScriptEngine getScriptEngine() {
		return new JaskellScriptEngine(this);
	}

	public String getMethodCallSyntax(String obj, String method, String... args) {
		String ret = obj + "." + method + " [";
		int len = args.length;
		if (len == 0) {
			ret += "]";
			return ret;
		}

		for (int i = 0; i < len; i++) {
			ret += args[i];
			if (i != len - 1) {
				ret += ", ";
			} else {
				ret += "]";
			}
		}
		return ret;
	}

	public String getOutputStatement(String toDisplay) {
		StringBuffer buf = new StringBuffer();
		buf.append("print(\"");
		int len = toDisplay.length();
		for (int i = 0; i < len; i++) {
			char ch = toDisplay.charAt(i);
			switch (ch) {
			case '"':
				buf.append("\\\"");
				break;
			case '\\':
				buf.append("\\\\");
				break;
			default:
				buf.append(ch);
				break;
			}
		}
		buf.append("\")");
		return buf.toString();
	}

	public String getProgram(String... statements) {
		StringBuffer ret = new StringBuffer();
		int len = statements.length;
		ret.append("let ");
		for (int i = 0; i < len; i++) {
			ret.append(statements[i]);
			ret.append(";\n");
		}
		ret.append(" end");
		return ret.toString();
	}

	private static String[] names;

	private static String[] extensions;

	private static String[] mimeTypes;

	static {
		names = new String[] { "jaskell" };
		extensions = new String[] { "jaskell", "jsl" };
		mimeTypes = new String[] { "application/jaskell", "text/jaskell" };
	}
}
