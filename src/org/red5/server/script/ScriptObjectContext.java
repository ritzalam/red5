/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */
package org.red5.server.script;

import java.io.FileReader;
import java.io.IOException;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleNamespace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Script object model
 * 
 * @author Luke Hubbard <luke@codegent.com>
 * @author Paul Gregoire <mondain@gmail.com>
 */
public class ScriptObjectContext implements ApplicationContextAware,
		ResourceLoader, ResourcePatternResolver {

	protected static Log log = LogFactory.getLog(ScriptObjectContext.class
			.getName());

	private ApplicationContext parentContext;

	private ApplicationContext appCtx;

	private ScriptBeanFactory scriptBeanFactory;

	// ScriptEngine manager
	private static ScriptEngineManager scriptManager;

	public void init() {
		log.info("Loading scripting");
		try {
			scriptBeanFactory.setApplicationContext(appCtx);
			// scriptBeanFactory.startup();
		} catch (Exception e) {
			log.warn("Problem starting script bean factory", e);
		}
	}

	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}	
	
	public void setParentContext(ApplicationContext parentContext) {
		log.debug("Parent context for scripts: "
				+ parentContext.getClass().getName());
		this.parentContext = parentContext;
	}

	public ScriptEngineManager getScriptManager() {
		return scriptManager;
	}

	public void setScriptManager(ScriptEngineManager scriptManager) {
		ScriptObjectContext.scriptManager = scriptManager;
	}

	public ListableBeanFactory getBeans() {
		return appCtx;
	}

	public ApplicationContext getApplicationContext() {
		return appCtx;
	}

	public void setApplicationContext(ApplicationContext appCtx) {
		log.debug("App context for scripts: " + appCtx.getClass().getName());
		this.appCtx = appCtx;
	}

	public ScriptBeanFactory getScriptBeanFactory() {
		return scriptBeanFactory;
	}

	public void setScriptBeanFactory(ScriptBeanFactory scriptBeanFactory) {
		this.scriptBeanFactory = scriptBeanFactory;
	}

	public MessageSource getMessageSource() {
		return appCtx;
	}

	public Resource getResource(String path) {
		return appCtx.getResource(path);
	}

	public Resource[] getResources(String pattern) throws IOException {
		return appCtx.getResources(pattern);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ScriptObjectContext ctx = new ScriptObjectContext();
		ctx.init();
		if (null == scriptManager) {
			scriptManager = new ScriptEngineManager();
		}

		// Javascript
		ScriptEngine jsEngine = scriptManager.getEngineByName("rhino");
		jsEngine.put(ScriptEngine.FILENAME, "soc_test.js");

		try {
			System.out.println("Engine: " + jsEngine.getClass().getName()
					+ "\n" + jsEngine.getFactory().getEngineVersion());
			// jsEngine.eval(new
			// FileReader("D:/tmp/red5/java/scripting/branches/paulg_0.6/samples/E4X/e4x_example.js"));
			// jsEngine.eval(new
			// FileReader("D:/tmp/red5/java/scripting/branches/paulg_0.6/samples/application2.js"));
			// ScriptContext cx = jsEngine.getContext();

			// Setup Contect and ClassLoader
			// Context.enter();
			// Context cx = Context.getCurrentContext();
			// ScriptableObject scope = ScriptRuntime.getGlobal(cx);
			// ScriptableObject scope = ScriptRuntime.getGlobal((Context) cx);

			// ClassLoader cl = Thread.currentThread().getContextClassLoader();
			//			
			// GeneratedClassLoader gcl = ctx.createClassLoader(cl);
			//			
			// CompilerEnvirons ce = new CompilerEnvirons();

			// ce.setAllowMemberExprAsFunctionName(false)
			// ctx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE);
			// ce.initFromContext(ctx);
			// ce.setXmlAvailable(true);
			// ce.setOptimizationLevel(9);
			//			
			// ClassCompiler cc = new ClassCompiler(ce);
			// cc.setTargetExtends(getExtends());
			// cc.setTargetImplements(getInterfaces());
			//
			// Object[] generated = null;
			// generated = cc.compileToClassFiles(js, this.getLocation(), 0,
			// getTempClassName());
			// addGeneratedToClassLoader(gcl, generated);

			// load the script class
			// clazz = ((ClassLoader) gcl).loadClass((String)generated[2]);
			// Script script = (Script) clazz.newInstance();

			// execute the script saving the resulting scope
			// this is a bit like calling the constuctor on an object
			// the scope contains the resulting object
			// Scriptable result = (Scriptable) script.exec(ctx, scope);

			// set engine scope namespace
			Namespace n = new SimpleNamespace();
			jsEngine.setNamespace(n, ScriptContext.ENGINE_SCOPE);

			n.put("log", log);
			n.put("currentTime", new Long(System.currentTimeMillis()));

			Compilable eng = (Compilable) jsEngine;
			CompiledScript scr = eng.compile(new FileReader(
					"samples/application2.js"));

			// jsEngine.eval(new FileReader("samples/application2.js"));

			scr.eval();

			// Context.exit();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
