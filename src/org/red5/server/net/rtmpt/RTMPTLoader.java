package org.red5.server.net.rtmpt;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.red5.server.api.IServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Loader for the RTMPT server.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RTMPTLoader implements ApplicationContextAware {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(RTMPTLoader.class.getName());

	protected ApplicationContext applicationContext;

	protected String rtmptConfig = "classpath:/red5-rtmpt.xml";

	protected Server rtmptServer;

	protected IServer server;

	protected RTMPTHandler handler;

	public void setServer(IServer server) {
		this.server = server;
	}

	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		applicationContext = context;
	}

	public void setRTMPTHandler(RTMPTHandler handler) {
		this.handler = handler;
	}

	public void init() throws Exception {
		// So this class is left just starting jetty
		log.info("Loading RTMPT context from: " + rtmptConfig);
		XmlBeanFactory bf = new XmlBeanFactory(applicationContext.getResource(rtmptConfig));
		Server rtmptServer = (Server) bf.getBean("Server");

		// Setup configuration data in rtmptServer
//		ContextHandler contextHandler = null;
//		for (Handler handler : rtmptServer.getHandlers()) {
//			if (handler instanceof ContextHandler) {
//				contextHandler = (ContextHandler) handler;
//				break;
//			}
//		}
//		if (contextHandler == null) {
//			throw new Exception("No context handler found in the server.");
//		}
//		contextHandler.setAttribute(RTMPTHandler.HANDLER_ATTRIBUTE, this.handler);

		log.info("Starting RTMPT server");
		rtmptServer.start();

	}

}
