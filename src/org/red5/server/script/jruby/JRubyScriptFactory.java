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

package org.red5.server.script.jruby;

import java.io.IOException;

import org.jruby.exceptions.JumpException;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.scripting.ScriptFactory} implementation
 * for a JRuby script.
 * 
 * <p>Typically used in combination with a
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor Javadoc}
 * for a configuration example.
 * 
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 * @see JRubyScriptUtils
 */
public class JRubyScriptFactory implements ScriptFactory {

	private final String scriptSourceLocator;

	private final Class[] scriptInterfaces;

	/**
	 * Create a new JRubyScriptFactory for the given script source.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param scriptInterfaces the Java interfaces that the scripted object
	 * is supposed to implement
	 * @throws IllegalArgumentException if either of the supplied arguments is <code>null</code>;
	 * or the supplied <code>scriptSourceLocator</code> argument is composed wholly of whitespace;
	 * or if the supplied <code>scriptInterfaces</code> argument array has no elements
	 */
	public JRubyScriptFactory(String scriptSourceLocator, Class[] scriptInterfaces) {
		Assert.hasText(scriptSourceLocator);
		Assert.notEmpty(scriptInterfaces);
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}

	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	public Class[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	/**
	 * JRuby scripts do require a config interface.
	 * @return <code>true</code> always
	 */
	public boolean requiresConfigInterface() {
		return true;
	}

	/**
	 * Load and parse the JRuby script via JRubyScriptUtils.
	 * @see JRubyScriptUtils#createJRubyObject(String, Class[])
	 */
	public Object getScriptedObject(ScriptSource actualScriptSource, Class[] actualInterfaces)
			throws IOException, ScriptCompilationException {
		try {
			return JRubyScriptUtils.createJRubyObject(actualScriptSource.getScriptAsString(), actualInterfaces);
		}
		catch (JumpException ex) {
			throw new ScriptCompilationException("Could not compile JRuby script: " + actualScriptSource, ex);
		}
	}

}
