package org.red5.server.context;

import java.io.IOException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class GenericRed5Context extends GenericApplicationContext
	implements ApplicationContextAware {

	protected String baseDir = "";
	protected String configFilePath = "applicationContext.xml";
	protected ResourcePatternResolver resourcePatternResolver = null;
	protected ResourceLoader resourceLoader = null;
	
	public GenericRed5Context(ApplicationContext parent, String baseDir, String configFilePath){
		super();
		if(parent!=null) this.setParent(parent); 
		this.baseDir = baseDir;
		if(!this.baseDir.endsWith("/")){
			this.baseDir += "/";
		}
		this.configFilePath = configFilePath;
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ResourceLoader resourceLoader = new Red5ContextResourceLoader(this.baseDir, classLoader);
		this.setResourceLoader(resourceLoader);
		resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader, classLoader);
		
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(this);
		xmlReader.loadBeanDefinitions(new FileSystemResource(configFilePath));
		this.refresh();
	}
	
	public void initialize(){
		// 
	}
	
	public void setApplicationContext(ApplicationContext appCtx){
		this.setParent(appCtx);
	}

	public String getBaseDir() {
		return baseDir;
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	protected ResourcePatternResolver getResourcePatternResolver(){
		return resourcePatternResolver;
	}

	public Resource getResource(String uri) {
		return resourceLoader.getResource(uri);
	}

	public Resource[] getResources(String pattern) throws IOException {
		return resourcePatternResolver.getResources(pattern);
	}
	
	

}
