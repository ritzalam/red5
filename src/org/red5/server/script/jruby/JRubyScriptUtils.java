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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.ast.ClassNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for handling JRuby-scripted objects.
 * 
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class JRubyScriptUtils {

	/**
	 * Create a new JRuby-scripted object from the given script source.
	 * @param scriptSource the script source text
	 * @param interfaces the interfaces that the scripted Java object
	 * is supposed to implement
	 * @return the scripted Java object
	 * @throws JumpException in case of JRuby parsing failure
	 */
	public static Object createJRubyObject(String scriptSource,
			Class[] interfaces) throws JumpException {
		IRuby ruby = Ruby.getDefaultInstance();
		//node is a compiled script
		Node scriptRootNode = ruby.parse(scriptSource, "");
		//rubyObj is the result from an evaluated compiled script
		IRubyObject rubyObject = ruby.eval(scriptRootNode);
		//if the script does not return an instance then call the constructor
		if (rubyObject instanceof RubyNil) {
			String className = findClassName(scriptRootNode);
			rubyObject = ruby.evalScript("\n" + className + ".new");
		}
		// still null?
		if (rubyObject instanceof RubyNil) {
			throw new ScriptCompilationException(
					"Compilation of JRuby script returned '" + rubyObject + "' ");
		}
		return Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(),
				interfaces, new RubyObjectInvocationHandler(rubyObject, ruby));
	}

	/**
	 * Given the root {@link Node} in a JRuby AST will locate the name of the class defined
	 * by that AST.
     * @param rootNode
     * @return
	 * @throws IllegalArgumentException if no class is defined by the supplied AST.
	 */
	private static String findClassName(Node rootNode) {
		ClassNode classNode = findClassNode(rootNode);
		if (classNode == null) {
			throw new IllegalArgumentException(
					"Unable to determine class name for root node '" + rootNode + "' ");
		}
		Colon2Node node = (Colon2Node) classNode.getCPath();
		return node.getName();
	}

	/**
	 * Finds the first {@link ClassNode} under the supplied {@link Node}. Returns
	 * '<code>null</code>' if no {@link ClassNode} is found.
     * @param node
     * @return
     */
	private static ClassNode findClassNode(Node node) {
		if (node instanceof ClassNode) {
			return (ClassNode) node;
		}
		List children = node.childNodes();
		for (int i = 0; i < children.size(); i++) {
			Node child = (Node) children.get(i);
			if (child instanceof ClassNode) {
				return (ClassNode) child;
			} else if (child instanceof NewlineNode) {
				NewlineNode nn = (NewlineNode) child;
				Node found = findClassNode(nn.getNextNode());
				if (found instanceof ClassNode) {
					return (ClassNode) found;
				}
			}
		}

		for (int i = 0; i < children.size(); i++) {
			Node child = (Node) children.get(i);
			Node found = findClassNode(child);
			if (found instanceof ClassNode) {
				return (ClassNode) child;
			}
		}
		return null;
	}

	/**
	 * InvocationHandler that invokes a JRuby script method.
	 */
	private static class RubyObjectInvocationHandler implements
			InvocationHandler {
		//scripted instance
		private final IRubyObject rubyObject;

		//script engine
		private final IRuby ruby;

		public RubyObjectInvocationHandler(IRubyObject rubyObject, IRuby ruby) {
			this.rubyObject = rubyObject;
			this.ruby = ruby;
		}

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			IRubyObject[] rubyArgs = convertToRuby(args);
			IRubyObject result = this.rubyObject.callMethod(method.getName(),
					rubyArgs);
			return JavaUtil.convertRubyToJava(result);
		}

		private IRubyObject[] convertToRuby(Object[] javaArgs) {
			if (javaArgs == null || javaArgs.length == 0) {
				return new IRubyObject[0];
			}
			return JavaUtil.convertJavaArrayToRuby(this.ruby, javaArgs);
		}
	}

}
