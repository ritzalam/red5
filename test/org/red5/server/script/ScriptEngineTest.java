package org.red5.server.script;

import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple script engine tests. Some of the hello world scripts found here:
 * http://www.roesler-ac.de/wolfram/hello.htm
 *
 * @author paul.gregoire
 */
public class ScriptEngineTest {

	private static final Logger log = LoggerFactory.getLogger(ScriptEngineTest.class);

	// ScriptEngine manager
	private static ScriptEngineManager mgr = new ScriptEngineManager();

	// Javascript
	@Test
	public void testJavascriptHelloWorld() {
		ScriptEngine jsEngine = null;
		for (ScriptEngineFactory factory : mgr.getEngineFactories()) {
			if (factory.getEngineName().toLowerCase().matches(
					".*(rhino|javascript|ecma).*")) {
				jsEngine = factory.getScriptEngine();
			}
		}
		if (null == jsEngine) {
			log.error("Javascript is not supported in this build");
		}
		/*
		 try {
		 jsEngine = mgr.getEngineByName("javascript");
		 jsEngine.eval("print('Javascript - Hello, world!')");
		 } catch (Throwable ex) {
		 System.err.println("Get by name failed for: javascript");
		 //ex.printStackTrace();
		 jsEngine = null;
		 }
		 if (null == jsEngine) {
		 try {
		 jsEngine = mgr.getEngineByName("rhino");
		 jsEngine.eval("print('Javascript/Rhino - Hello, world!')");
		 } catch (Throwable ex) {
		 System.err.println("Get by name failed for: rhino");
		 //ex.printStackTrace();
		 assertFalse(true);
		 }
		 }
		 */
	}

	// Ruby
	@Test
	public void testRubyHelloWorld() {
		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
		try {
			rbEngine.eval("puts 'Ruby - Hello, world!'");
		} catch (Exception ex) {
			//ex.printStackTrace();
			assertFalse(true);
		}
	}

	// Python
	@Test
	public void testPythonHelloWorld() {
		ScriptEngine pyEngine = mgr.getEngineByName("python");
		try {
			pyEngine.eval("print \"Python - Hello, world!\"");
		} catch (Exception ex) {
			//ex.printStackTrace();
			assertFalse(true);
		}
	}

	// Groovy
	@Test
	public void testGroovyHelloWorld() {
		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
		try {
			gvyEngine.eval("println  \"Groovy - Hello, world!\"");
		} catch (Exception ex) {
			//ex.printStackTrace();
			assertFalse(true);
		}
	}

	// Judoscript
	//	@Test
	//	public void testJudoscriptHelloWorld() {
	//		ScriptEngine jdEngine = mgr.getEngineByName("judo");
	//		try {
	//			jdEngine.eval(". \'Judoscript - Hello World\';");
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	// Haskell
	// @Test
	// public void testHaskellHelloWorld()
	// {
	// ScriptEngine hkEngine = mgr.getEngineByName("jaskell");
	// try
	// {
	// StringBuilder sb = new StringBuilder();
	// sb.append("module Hello where ");
	// sb.append("hello::String ");
	// sb.append("hello = 'Haskell - Hello World!'");
	// hkEngine.eval(sb.toString());
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// Tcl
	//	@Test
	//	public void testTclHelloWorld() {
	//		ScriptEngine tEngine = mgr.getEngineByName("tcl");
	//		try {
	//			StringBuilder sb = new StringBuilder();
	//			sb.append("#!/usr/local/bin/tclsh\n");
	//			sb.append("puts \"Tcl - Hello World!\"");
	//			tEngine.eval(sb.toString());
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	// Awk
	// @Test
	// public void testAwkHelloWorld()
	// {
	// ScriptEngine aEngine = mgr.getEngineByName("awk");
	// try
	// {
	// StringBuilder sb = new StringBuilder();
	// sb.append("BEGIN { print 'Awk - Hello World!' } END");
	// aEngine.eval(sb.toString());
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// E4X
	@Test
	public void testE4XHelloWorld() {
		ScriptEngine eEngine = mgr.getEngineByName("rhino");
		try {
			//Compilable compiler = (Compilable) eEngine;
			//CompiledScript script = compiler.compile("var d = new XML('<d><item>Hello</item><item>World!</item></d>');print(d..item);");
			//Namespace ns = eEngine.createNamespace();
			//ns.put('d', "new XML('<d><item>Hello</item><item>World!</item></d>');");
			//System.out.println("E4X - " + script.eval(ns));
			eEngine
					.eval("var d = new XML('<d><item>Hello</item><item>World!</item></d>');print('E4X - ' + d..item);");
		} catch (Exception ex) {
			//ex.printStackTrace();
			assertFalse(true);
		}
	}

	// PHP
	// @Test
	// public void testPHPHelloWorld()
	// {
	// //have to add php lib to java env
	// //java.library.path
	// //System.setProperty("java.library.path", "C:\\PHP;" +
	// System.getProperty("java.library.path"));
	// ScriptEngine pEngine = mgr.getEngineByName("php");
	// try
	// {
	// pEngine.eval("<? echo 'PHP - Hello World'; ?>");
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	// @Test
	// public void testE4X()
	// {
	// // Javascript
	// ScriptEngine jsEngine = mgr.getEngineByName("rhino");
	// try
	// {
	// System.out.println("Engine: " + jsEngine.getClass().getName());
	// jsEngine.eval(new FileReader("samples/E4X/e4x_example.js"));
	// }
	// catch (Exception ex)
	// {
	// //ex.printStackTrace();
	// assertFalse(true);
	// }
	// }

	//	@Test
	//	public void testJavascriptApplication() {
	//		ScriptEngine jsEngine = mgr.getEngineByName("rhino");
	//		try {
	//			// jsEngine.eval(new FileReader("samples/application.js"));
	//			jsEngine.eval(new FileReader("samples/application2.js"));
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}
	//
	//	@Test
	//	public void testRubyApplication() {
	//		ScriptEngine rbEngine = mgr.getEngineByName("ruby");
	//		try {
	//			rbEngine.eval(new FileReader("samples/application.rb"));
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}
	//
	//	@Test
	//	public void testGroovyApplication() {
	//		ScriptEngine gvyEngine = mgr.getEngineByName("groovy");
	//		try {
	//			gvyEngine.eval(new FileReader("samples/application.groovy"));
	//			// gvyEngine.eval("def ap = new Application();println
	//			// ap.toString();");
	//		} catch (Exception ex) {
	//			//ex.printStackTrace();
	//			assertFalse(true);
	//		}
	//	}

	@Test
	public void testEngines() {
		Map<String, ScriptEngineFactory> engineFactories = new HashMap<String, ScriptEngineFactory>(
				7);
		//List<ScriptEngineFactory> factories = mgr.getEngineFactories(); //jdk6
		//ScriptEngineFactory[] factories = mgr.getEngineFactories(); //jdk5
		for (ScriptEngineFactory factory : mgr.getEngineFactories()) {
			try {
				System.out
						.println("\n--------------------------------------------------------------");
				String engName = factory.getEngineName();
				String engVersion = factory.getEngineVersion();
				String langName = factory.getLanguageName();
				String langVersion = factory.getLanguageVersion();
				System.out.printf("Script Engine: %s (%s) Language: %s (%s)",
						engName, engVersion, langName, langVersion);
				engineFactories.put(engName, factory);
				System.out.print("\nEngine Alias(es):");
				for (String name : factory.getNames()) {
					System.out.printf("%s ", name);
				}
				System.out.printf("\nExtension: ");
				for (String name : factory.getExtensions()) {
					System.out.printf("%s ", name);
				}
			} catch (Throwable e) {
				log.error("{}", e);
			}
		}
	}

}
