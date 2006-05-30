package org.red5.server.jetty;

import java.util.EventListener;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;

public class Red5WebPropertiesConfiguration implements Configuration, EventListener {

	// Initialize Logging
	protected static Log log =
        LogFactory.getLog(Red5WebPropertiesConfiguration.class.getName());
	
	protected static WebAppContext _context;
	
	public void setWebAppContext(WebAppContext context) {
		_context = context;
	}
	
	public WebAppContext getWebAppContext() {
		return _context;
	}

	public void configureClassLoader() throws Exception {
		// TODO Auto-generated method stub
	}

	public void configureDefaults() throws Exception {
		// TODO Auto-generated method stub
	}
	
	public void configureWebApp () throws Exception{
        
		if (getWebAppContext().isStarted()) {
            log.debug("Cannot configure webapp after it is started"); 
            return;
        }
               
        Resource webInf= getWebAppContext().getWebInf();
        if(webInf!=null&&webInf.isDirectory()){
            Resource config = webInf.addPath("red5-web.properties");       
            if(config.exists()){
            	log.debug("Configuring red5-web.properties");
            	Properties props = new Properties();
            	props.load(config.getInputStream());
            	String contextPath = props.getProperty("webapp.contextPath");
            	String virtualHosts = props.getProperty("webapp.virtualHosts");
        		String[] hostnames = virtualHosts.split(",");
        		for (int i = 0; i < hostnames.length; i++) {
        			hostnames[i] = hostnames[i].trim();
        			if(hostnames[i].equals("*")) {
        				// A virtual host "null" must be used so requests for any host
        				// will be server.
        				hostnames = null;
        				break;
        			}
        		}
    			getWebAppContext().setVirtualHosts(hostnames);
    			getWebAppContext().setContextPath(contextPath);
            }
        }
    }
    
	public void deconfigureWebApp() throws Exception {
		// TODO Auto-generated method stub
	}

}
