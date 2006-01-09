/*
 * Spark | Java Flash Server
 * For more details see: http://www.osflash.org
 * Copyright 2005, Luke Hubbard luke@codegent.com
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * See the README.txt in this package for details of changes
 */
package org.red5.server.script.javascript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

import net.sf.cglib.asm.Type;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.core.TypeUtils;
import net.sf.cglib.proxy.InterfaceMaker;

import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.script.AbstractScript;
import org.springframework.beans.factory.script.ScriptContext;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class JavaScript extends AbstractScript {

	public static final String SCOPE = "org.mozilla.javascript.ScriptableObject@scope";
	public static final String META = "com.codegent.spark.javascript.JavaScriptMeta@meta";
	
	protected Class extendsClass = Object.class;
	protected String id;
	
	public JavaScript(String id, String className, ScriptContext context) {
		super(className, context);
		this.id = id;
	}

	private String className = null;

	protected Object createObject(InputStream is) throws IOException,
			BeansException {
		// clearInterfaces();
		Class clazz = null;
		try {
			
			// Get the JavaScript into a String
			String js = "";
	
			if(isInline()) js = inlineScriptBody();
			else js = getAsString(is);
			
			// NOTE: here would be a good place to apply a preprocessor
			// Perhaps an alternative to using the meta object

			// Setup Contect and ClassLoader
			Context ctx = Context.enter();
			
			//ctx.initStandardObjects();
			
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			
			GeneratedClassLoader gcl = ctx.createClassLoader(cl);
			
			CompilerEnvirons ce = new CompilerEnvirons();
			
			//ce.setAllowMemberExprAsFunctionName(false)
			//ctx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE);
			
			ce.initFromContext(ctx);
			ce.setXmlAvailable(true);
			ce.setOptimizationLevel(9);
			
			ClassCompiler cc = new ClassCompiler(ce);
			
			cc.setTargetExtends(getExtends());
			cc.setTargetImplements(getInterfaces());

			Object[] generated = null;
			generated = cc.compileToClassFiles(js, this.getLocation(),
					0, getTempClassName());
	
			addGeneratedToClassLoader(gcl, generated);
		
			// --------------------------------------------------------------------------------
			
			// get the scope;
			ScriptableObject scope = JavaScriptScopeThreadLocal.getScope();
			if(scope==null) scope = ScriptRuntime.getGlobal(ctx);
		
			JavaScriptMeta meta  = new JavaScriptMeta();
			
			// add meta object to the scope, its used to collect info for class and interface generation
			ScriptableObject.putProperty(scope, "meta", Context.javaToJS(meta, scope));
			
			// add a log object to the scope
			ScriptableObject.putProperty(scope, "log", Context.javaToJS(LogFactory.getLog(getClassName()), scope));
			
			// load the script class 
			clazz = ((ClassLoader) gcl).loadClass((String)generated[2]);
			Script script = (Script) clazz.newInstance();

			// execute the script saving the resulting scope
			// this is a bit like calling the constuctor on an object
			// the scope contains the resulting object
			
			Scriptable result = (Scriptable) script.exec(ctx, scope);
			
			// remove the meta object from the scope, it is no longer needed
			// we just used it to collect info for creating the object, and interfaces
			scope.delete("meta");
			
			if(meta.getExtends()!=null)
				cc.setTargetExtends(cl.loadClass(meta.getExtends()));
			else cc.setTargetExtends(getExtends());
			
			if(log.isDebugEnabled())
				log.debug("Target extends: "+cc.getTargetExtends());
			
			if(meta.getImplements().length>0){
				String[] interfaces = meta.getImplements();
				for(int i=0; i<interfaces.length; i++){
					addInterface(cl.loadClass(interfaces[i]));
				}
			}
			
			if(meta.getMethodNames().length>0){
			
				Class publicInterface;
				
				try{
					publicInterface = cl.loadClass(getPublicInterfaceName());
				}
				catch(ClassNotFoundException ex){
					InterfaceMaker interfaceMaker = new InterfaceMaker();
					interfaceMaker.setClassLoader(cl);
					NamingPolicy namingPolicy = new InterfaceNamingPolicy(getPublicInterfaceName());
					interfaceMaker.setNamingPolicy(namingPolicy);
					String[] methodNames = meta.getMethodNames();
					Type[] noEx = new Type[0];
					for(int i=0; i<methodNames.length; i++){
						String descriptor = meta.getMethodDescriptor(methodNames[i]);
						if(log.isDebugEnabled())
							log.debug("Method descriptor: "+descriptor);
						interfaceMaker.add(TypeUtils.parseSignature(descriptor),noEx);
					}
					publicInterface = interfaceMaker.create();
				}				
				
				if(log.isDebugEnabled())
					log.debug("Generated public interface " + publicInterface.getName());
				
				addInterface(publicInterface);
				
			}
			
			cc.setTargetImplements(getInterfaces());
			
			try {
				
				generated = cc.compileToClassFiles(js, this.getLocation(),
					0, getClassName());
			
				addGeneratedToClassLoader(gcl, generated);
			
				clazz = ((ClassLoader) gcl).loadClass(getClassName());
				if(log.isDebugEnabled())
					log.debug("Loaded javascript class " + clazz);
			}
			catch(NoClassDefFoundError ncex){
				throw new BeanCreationException("Class not found", ncex);
			}
			
			// Call the constructor passing in the scope object
			Constructor cstr = clazz.getConstructor(new Class[]{Scriptable.class});
			Object instance = cstr.newInstance(new Object[]{scope});
			

			JavaScriptScopeThreadLocal.setScope(scope);
			
			Context.exit();
			return instance;

		} catch (RuntimeException rex){ 
			throw new BeanCreationException("Runtime exception", rex);
		} catch (Exception ex) {
			throw new BeanCreationException("Error instantiating" + clazz, ex);
		}
	}
	
	public void setExtends(Class extendsClass){
		this.extendsClass = extendsClass;
	}
	
	public Class getExtends(){
		return this.extendsClass;
	}
	
	/**
	 * @param gcl
	 * @param generated
	 */
	private void addGeneratedToClassLoader(GeneratedClassLoader gcl, Object[] generated) {
		for (int i = 0; i < generated.length; i += 2) {
			String name = (String) generated[i];
			byte[] code = (byte[]) generated[i + 1];
			gcl.defineClass(name, code);
		}
	}

	private void setClassName(String className){
		this.className = className + '_' + id;
	}
	
	private String getClassName(){
		if(className!=null) return className;
		else if(isInline()) return getInlineClassName();
		else return getSafeClassName(getLocation());
	}
	
	private String getInlineClassName(){
		return "InlineJS_"+'_'+id;
	}
	
	private String getTempClassName(){
		return "TempJS_"+'_'+id;
	}

	private String getPublicInterfaceName(){
		return getClassName()+"_Pub";
	}
	
	static String getAsString(InputStream stream) throws IOException {
		StringWriter buffer = new StringWriter();
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		String nextLine = reader.readLine();
		while (nextLine != null) {
			buffer.write(nextLine);
			buffer.write("\n");
			nextLine = reader.readLine();
		}
		return buffer.toString();
	}

	public String getSafeClassName(String unsafe){
		if(unsafe.startsWith("WEB-INF/")) 
			unsafe = unsafe.substring(8);
		if(unsafe.toLowerCase().endsWith(".js")) 
			unsafe = unsafe.substring(0, unsafe.length()-3);
		unsafe = unsafe.replace('/', '.');
		unsafe = unsafe.replace('-', '_');
		unsafe = unsafe.replace(' ', '_');
		if(unsafe.startsWith(".")) 
			unsafe = unsafe.substring(1);
		return unsafe + "_" + id;
	}
	
	class InterfaceNamingPolicy implements NamingPolicy {

		private String interfaceName;

		public InterfaceNamingPolicy(String interfaceName){
			this.interfaceName = interfaceName;
		}
		
		/* (non-Javadoc)
		 * @see net.sf.cglib.core.NamingPolicy#getClassName(java.lang.String, java.lang.String, java.lang.Object, net.sf.cglib.core.Predicate)
		 */
		public String getClassName(String arg0, String arg1, Object arg2,
				Predicate arg3) {
			return interfaceName;
		}

	}
	
}