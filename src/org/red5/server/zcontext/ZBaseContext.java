package org.red5.server.zcontext;

import java.io.IOException;

import org.red5.server.api.IClientRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ZBaseContext extends GenericApplicationContext
	implements ApplicationContextAware {

	protected static final String CLIENT_REGISTRY = "clientRegistry";
	protected static final String SERVICE_INVOKER = "serviceInvoker";
	
	protected String baseDir = "";
	protected String configFilePath = "context.xml";
	protected ResourcePatternResolver resourcePatternResolver = null;
	
	public ZBaseContext(ApplicationContext parent, String baseDir, String configFilePath){
		
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
		ApplicationContext c = getWebContext();
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
	
	public ApplicationContext getWebContext(){
		ApplicationContext c = this;
		while (c!=null){
			if(c instanceof org.springframework.web.context.WebApplicationContext){
				return c;
			}
			c = c.getParent();
		}
		return null;
	}
	
	public Resource getResource(String uri) {
		ApplicationContext c = getWebContext();
		if(c!=null)
			return c.getResource(((uri.startsWith("./") || uri.startsWith("/"))?"":this.baseDir) + uri);
		else
			return super.getResource(uri);
		
	}
	
	public Resource[] getResources(String pattern) throws IOException {
		ApplicationContext c = getWebContext();
		if(c!=null)
			return this.getParent().getResources(((pattern.startsWith("/")||pattern.startsWith("./"))?"":this.baseDir) + pattern);
		else
			return resourcePatternResolver.getResources(pattern);
		
	}

	public boolean hasClientRegistry() {
		return containsBean(CLIENT_REGISTRY);
	}
	
	public IClientRegistry getClientRegistry() {
		IClientRegistry registry;
		if (!hasClientRegistry()) {
			// No special client registry configured, create default
			registry = new DefaultClientRegistry();
			this.getBeanFactory().registerSingleton(CLIENT_REGISTRY, registry);
		} else
			registry = (IClientRegistry) getBean(CLIENT_REGISTRY);
		return registry;
	}
}
