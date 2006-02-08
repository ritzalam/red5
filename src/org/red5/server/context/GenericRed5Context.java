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
		
		if(this.getClass().getName().endsWith(".GlobalContext"))
			return;
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(this);
		ApplicationContext c = getWebContent();
		if(c!=null)
			xmlReader.loadBeanDefinitions(c.getResource(configFilePath));
		else
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
	public ApplicationContext getWebContent()
	{
		ApplicationContext c = this;
		while (c!=null)
		{
			if(c instanceof org.springframework.web.context.WebApplicationContext)
			{
				return c;
				
			}
			c = c.getParent();
		}
		return null;
	}
	public Resource getResource(String uri) {
		ApplicationContext c = getWebContent();
		if(c!=null)
			return c.getResource(((uri.startsWith("./") || uri.startsWith("/"))?"":this.baseDir) + uri);
		else
			return super.getResource(uri);
		
	}
	
	public Resource[] getResources(String pattern) throws IOException {
		ApplicationContext c = getWebContent();
		if(c!=null)
			return this.getParent().getResources(((pattern.startsWith("/")||pattern.startsWith("./"))?"":this.baseDir) + pattern);
		else
			return resourcePatternResolver.getResources(pattern);
		
	}
	
	

}
