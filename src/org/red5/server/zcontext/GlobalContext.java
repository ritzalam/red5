package org.red5.server.zcontext;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class GlobalContext 
	extends ZBaseContext {
	
	public static final String DEFAULT_HOST = "__default__";
	public String hostsPath = "hosts";
	
	protected static Log log =
        LogFactory.getLog(GlobalContext.class.getName());
	
	public GlobalContext(String configFilePath) throws BeansException {
		super(null,"./",configFilePath);
	}
	
	public GlobalContext(String configFilePath, String hostsPath) throws BeansException {
		super(null,"./",configFilePath);
		this.hostsPath = hostsPath;
	}
	
	public void initialize(){
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(this);
		ApplicationContext c = getWebContext();
		if(c!=null)
			xmlReader.loadBeanDefinitions(c.getResource(configFilePath));
		else
			xmlReader.loadBeanDefinitions(new FileSystemResource(configFilePath));
		this.refresh();
		if(log.isDebugEnabled()) {
			log.debug("Initialize global context");
		}
		loadHosts();
	}
	
	protected void loadHosts(){
		// this.getResources()
		if(log.isDebugEnabled()) {
			log.debug("Loading hosts");
		}
		try {
			Resource[] hosts = getResources(hostsPath + "/*");
			if(hosts!=null){
				for(int i=0; i<hosts.length; i++){
					Resource host = hosts[i];
					String hostname = host.getFile().getName();
					if(hostname.indexOf(".")==0) continue;
					if(log.isDebugEnabled()) {
						log.debug("hostname: "+hostname);
					}
					if(!hostname.startsWith(".")){
						addHost(hostname);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void addHost(String hostname){
		if(log.isDebugEnabled()) {
			log.debug("Add host: "+hostname);
		}
		String hostPath = this.getBaseDir() + hostsPath + "/"+ hostname;
		HostContext hostContext = new HostContext(this, hostname, hostPath);
		this.getBeanFactory().registerSingleton(hostname,hostContext);
		hostContext.initialize();
	}
	
	public HostContext getDefaultHost(){
		return getHostContext(DEFAULT_HOST);
	}
	
	public boolean hasHostContext(String hostname){
		return containsBean(hostname);
	}
	
	public HostContext getHostContext(String hostname){
		return (HostContext) getBean(hostname);
	}
	
}
