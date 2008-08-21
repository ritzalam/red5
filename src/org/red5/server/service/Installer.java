package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.red5.compatibility.flex.messaging.io.ArrayCollection;
import org.red5.server.LoaderMBean;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.jmx.JMXFactory;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides the means to list, download, install, and un-install 
 * applications from a given url.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Installer {

	private Logger log = LoggerFactory.getLogger(Installer.class);
	
	private String applicationRepositoryUrl;

	{
		log.info("Installer service created");
	}
	
	public String getApplicationRepositoryUrl() {
		return applicationRepositoryUrl;
	}

	public void setApplicationRepositoryUrl(String applicationRepositoryUrl) {
		this.applicationRepositoryUrl = applicationRepositoryUrl;
	}
	
	/**
	 * Returns the LoaderMBean.
	 * @return
	 */
	public LoaderMBean getLoader() {
		MBeanServer mbs = JMXFactory.getMBeanServer();
		
		ObjectName oName = JMXFactory.createObjectName("type", "TomcatLoader");
		
		LoaderMBean proxy = null;
		if (mbs.isRegistered(oName)) {
			proxy = (LoaderMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, oName, LoaderMBean.class, true);
			log.debug("Loader was found");
		} else {
			log.warn("Loader not found");
		}
		return proxy;
	}	
	
	/**
	 * Returns a Map containing all of the application wars in the snapshot repository.
	 * 
	 * @return
	 */
	public ArrayCollection<String> getApplicationList() {
		ArrayCollection<String> list = new ArrayCollection<String>();
		
		// create a singular HttpClient object
		HttpClient client = new HttpClient();
		// establish a connection within 5 seconds
		client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
		//try the wav version first
		HttpMethod method = new GetMethod(applicationRepositoryUrl);
		method.setFollowRedirects(true);
		// execute the method
		try {
			int code = client.executeMethod(method);
			log.debug("HTTP response code: {}", code);
			log.debug("Response: {}", method.getResponseBodyAsString());
			//
		} catch (HttpException he) {
			log.error("Http error connecting to {}", applicationRepositoryUrl, he);
		} catch (IOException ioe) {
			log.error("Unable to connect to {}", applicationRepositoryUrl, ioe);
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}			
		
		return list;
	}
	
	/**
	 * Installs a given application.
	 * 
	 * @param applicationWarName
	 * @return
	 */
	public boolean install(String applicationWarName) {
		IConnection conn = Red5.getConnectionLocal();
		
		boolean result = false;

		//strip everything except the applications name
		String application = applicationWarName.substring(0, applicationWarName.indexOf('-'));
		log.debug("Application name: {}", application);		
		
		//get webapp location
		String webappsDir = System.getProperty("red5.webapp.root");			
		log.debug("Webapp folder: {}", webappsDir);		
		
		//setup context
		String contextPath = '/' + application;
		String contextDir = webappsDir + contextPath;		
		
		//verify this is a unique app
		File appDir = new File(webappsDir, application);
		if (appDir.exists()) {
			if (appDir.isDirectory()) {
				log.debug("Application directory exists");
			} else {
				log.warn("Application destination is not a directory");
			}

			org.red5.server.api.service.ServiceUtils.invokeOnConnection(conn, "onAlert", new Object[]{String.format("Application %s already installed, please un-install before attempting another install", application)});			
			return false;
		} else {
			//use the system temp directory for moving files around
			String srcDir = System.getProperty("java.io.tmpdir");
			log.debug("Source directory: {}", srcDir);
			//look for archive containing application (war, zip, etc..)
			File dir = new File(srcDir);
			if (!dir.exists()) {
				log.warn("Source directory not found");
			} else {
				if (!dir.isDirectory()) {
					log.warn("Source directory is not a directory");
				}
			}
			//get a list of temp files
			File[] files = dir.listFiles();
			for (File f : files) {
				String fileName = f.getName();
				if (fileName.equals(applicationWarName)) {
					log.debug("File found matching application name");
					result = true;
					break;
				}
			}
			dir = null;
			
			//if the file was not found then download it
			if (!result) {
				// create a singular HttpClient object
				HttpClient client = new HttpClient();
				// establish a connection within 5 seconds
				client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
				//try the wav version first
				HttpMethod method = new GetMethod(applicationRepositoryUrl + applicationWarName);
				method.setFollowRedirects(true);
				FileOutputStream fos = null;				
				// execute the method
				try {
					int code = client.executeMethod(method);
					log.debug("HTTP response code: {}", code);
					//create output file
					fos = new FileOutputStream(srcDir + '/' + applicationWarName);
					log.debug("Writing response to {}/{}", srcDir, applicationWarName);								
					InputStream is = method.getResponseBodyAsStream();
					byte[] buf = new byte[512];
					while (is.read(buf) != -1) {
						fos.write(buf);
					}
					is.close();
					fos.flush();
					//
					result = true;
				} catch (HttpException he) {
					log.error("Http error connecting to {}", applicationRepositoryUrl, he);
				} catch (IOException ioe) {
					log.error("Unable to connect to {}", applicationRepositoryUrl, ioe);
				} finally {
					try {
						fos.close();
					} catch (IOException e) {
					}
					if (method != null) {
						method.releaseConnection();
					}
				}				
			}
			
			//if we've found or downloaded the war
			if (result) {
    			//un-archive it to app dir
    			FileUtil.unzip(srcDir + '/' + applicationWarName, contextDir);
			}			
		}
		appDir = null;
				
		//get the webapp loader
		LoaderMBean loader = getLoader();
		if (loader != null) {
			//load and start the context
			loader.startWebApplication(application);	
		}	

		org.red5.server.api.service.ServiceUtils.invokeOnConnection(conn, "onAlert", new Object[]{String.format("Application %s was %s", application, (result ? "installed" : "not installed"))});
		
		return result;		
	}
	
	/**
	 * Un-installs a given application.
	 * 
	 * @param applicationName
	 * @return
	 */
	public boolean uninstall(String applicationName) {
		return false;
	}	
	
}
