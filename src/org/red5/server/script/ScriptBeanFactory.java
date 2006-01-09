/*
 * Spark | Java Flash Server
 * For more details see: http://www.osflash.org
 * Copyright 2005, Luke Hubbard luke@codegent.com
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * See the README.txt in this package for details of changes
 */
package org.red5.server.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.red5.server.script.javascript.JavaScriptFactory;
import org.red5.server.script.javascript.JavaScriptScopeThreadLocal;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * @author Luke Hubbard <luke@codegent.com>
 */
public class ScriptBeanFactory extends DefaultListableBeanFactory
	implements ApplicationContextAware {

	private static final Log log = LogFactory.getLog(ScriptBeanFactory.class);
	
	protected static String FILE_PATTERN = "*.js";
	protected String path = "WEB-INF/services/";
	protected boolean lazyLoading = false;
	protected boolean productionMode = false;
	protected ScriptObjectModel spark;
	protected JavaScriptFactory factory;
	protected ApplicationContext appCtx;
		
	public ScriptBeanFactory(){
		super();
	}
	
	// Public setters
		
	public void setApplicationContext(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public boolean isLazyLoading() {
		return lazyLoading;
	}

	public void setLazyLoading(boolean lazyLoading) {
		this.lazyLoading = lazyLoading;
	}
	
	public boolean isProductionMode() {
		return productionMode;
	}
	
	public void setProductionMode(boolean productionMode) {
		this.productionMode = productionMode;
	}

	// Init method
	
	public void startup() throws Exception {
		spark = new ScriptObjectModel(appCtx);
		setupScriptScope();
		setupJavaScriptFactory();
		if(!isLazyLoading()) initScripts();
	}

	// Setup methods
	
	protected void setupScriptScope(){
		Context ctx = Context.enter();
		ScriptableObject scope = ScriptRuntime.getGlobal(ctx);
		ScriptableObject.putProperty(scope, "spark", Context.javaToJS(spark, scope));
		JavaScriptScopeThreadLocal.setScope(scope);
	}
	
	protected void setupJavaScriptFactory() {
		try{
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					JavaScriptFactory.class.getName(), null, null, null, getClassLoader());
			bd.setSingleton(true);
			bd.setBeanClass(JavaScriptFactory.class);
			registerBeanDefinition("jsFactory",bd);		
			factory = (JavaScriptFactory) super.getBean("jsFactory",null,null);
			factory.setResourceLoader(appCtx);
		} catch(Exception ex){
			log.error("Error creating js factory", ex);
		} 
	}
	
	protected void initScripts() throws Exception {
		Resource[] res = appCtx.getResources(path+FILE_PATTERN);
		if(res == null || res.length==0){
			log.info("No scripts found in location: "+path+FILE_PATTERN);
		} else {
			for(int i=0; i<res.length; i++){
				Resource resource = res[i];
				String name = resource.getFilename();
				Object bean = null;
				log.info("Loading script for first time: "+name);
				try {
					bean = getBean(name);
				} catch(Exception ex){
					log.error("Error creating script: "+name,ex);
				}
				if(bean==null){
					log.error("Script bean is null: "+name);
				}
			}
		}
	}
	
	protected ClassLoader getClassLoader(){
		return Thread.currentThread().getContextClassLoader();
	}
	
	// Bean registration methods
	
	protected void registerScriptBeanDefinition(String beanName) {
		try {
			// Check the file is there, it will throw an exception if its not
			Resource resource = appCtx.getResource(path + beanName);
			resource.getFile().lastModified();
			// Continue to load bean definition
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, path + beanName);
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
				null, null, cargs, null, this.getClassLoader());
			bd.setFactoryBeanName("jsFactory");
			bd.setFactoryMethodName("create");
			bd.setSingleton(true);
			registerBeanDefinition(beanName,bd);	
		} catch(Exception ex){
			registerErrorBeanDefinition(beanName, ex);
		}
	}
	
	/*
	protected void registerWrapperBeanDefinition(Object bean){
		try {
			TransactionProxyFactoryBean proxy = new TransactionProxyFactoryBean();
			Transaction wrapped = (Transaction) bean;
			
			String tmName = wrapped.getTransactionManagerName();
			Properties tAttr = wrapped.getTransactionAttributes();
			PlatformTransactionManager tm = (PlatformTransactionManager) appCtx.getBean(tmName);
			if(false && tm==null) {
				log.error("Could not find transaction manager called '"+tmName);
				return;
			} 
			proxy.setTransactionManager(tm);
			proxy.setTransactionAttributes(tAttr);
			
			Class[] iClazz = bean.getClass().getInterfaces();
			String[] iNames = new String[iClazz.length-1];
			int j = 0;
			for(int k=0; k<iClazz.length; k++){
				if(iClazz[k]!=Transaction.class){
					iNames[j++] = iClazz[k].getName();
				}
			}
			proxy.setProxyInterfaces(iNames);
			proxy.setTarget(bean);
			proxy.setBeanFactory(appCtx);
			proxy.afterPropertiesSet();			
			registerSingleton(bean.getClass().getName()+"_Wrapped",proxy.getObject());
		
		} catch(Exception ex){
			log.error("Error creating wrapper",ex);
		}
		
	}
	
	protected Object wrapIfNeeded(Object bean){
		if(!(bean instanceof Transaction)) return bean;
		if(!containsBean(bean.getClass().getName()+"_Wrapped"))
			registerWrapperBeanDefinition(bean);
		return super.getBean(bean.getClass().getName()+"_Wrapped");
	}*/
	
	protected void registerErrorBeanDefinition(String beanName, Exception ex){
		try{
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, ex);
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
				ScriptErrorBean.class.getName(),null,cargs,null,this.getClassLoader());
			bd.setSingleton(true);
			registerBeanDefinition(beanName,bd);	
		} catch(ClassNotFoundException ex2){
			log.error("Class not found exception while creating error bean", ex2);
		}
	}
	
	// Public beanFactory methods
	
	public Object getBean(String name) throws BeansException {
		return getBean(name, null, null);
	}
		
	public Object getBean(String name, Class requiredType) throws BeansException {
		return getBean(name, requiredType, null);
	}

	/**
	 * Return the bean with the given name,
	 * checking the parent bean factory if not found.
	 * @param name the name of the bean to retrieve
	 * @param args arguments to use if creating a prototype using explicit arguments to a
	 * static factory method. It is invalid to use a non-null args value in any other case.
	 */
	public Object getBean(String name, Object[] args) throws BeansException {
		return getBean(name, null, args);
	}

	public Object getBean(String name, Class requiredType, Object[] args) throws BeansException {
		setupScriptScope();
		Object bean = null;
		if(containsBean(name)){
			bean = super.getBean(name, requiredType, args);
			// if production mode, dont check for reload
			// if(productionMode) return wrapIfNeeded(bean);
			if(productionMode) return bean;
			// otherwise check the script object
			org.springframework.beans.factory.script.Script script = null;
			if(factory!=null) script = factory.lookupScript(bean);
			if(script!=null && script.isModified()) 
				registerScriptBeanDefinition(name);
			else { 
				return bean;
				//return wrapIfNeeded(bean); 
			}
		} else registerScriptBeanDefinition(name);
		return super.getBean(name, requiredType, args); //wrapIfNeeded(super.getBean(name, requiredType, args));
	}
	
}


