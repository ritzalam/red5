package org.red5.server.jetty;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.red5.server.api.IApplicationLoader;

/**
 * Class that can load new applications in Jetty.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class JettyApplicationLoader implements IApplicationLoader {

    /**
     * Logger
     */
	protected static Log log = LogFactory
			.getLog(JettyApplicationContext.class.getName());

	/** Stores reference to the Jetty server. */
	private Server server;
	
	/**
	 * Create new application loader for Jetty servers.
	 * 
	 * @param server
	 */
	public JettyApplicationLoader(Server server) {
		this.server = server;
	}
	
	/** {@inheritDoc} */
	public void loadApplication(String contextPath, String directory) throws Exception {
		String[] handlersArr = new String[] {
				"org.mortbay.jetty.webapp.WebInfConfiguration",
				"org.mortbay.jetty.webapp.WebXmlConfiguration",
				"org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
				"org.mortbay.jetty.webapp.TagLibConfiguration",
				"org.red5.server.jetty.Red5WebPropertiesConfiguration" };

		WebAppContext context = new WebAppContext();
		context.setContextPath(contextPath);
		context.setConfigurationClasses(handlersArr);
		context.setDefaultsDescriptor("web-default.xml");
		context.setExtractWAR(true);
		context.setWar(directory);
		context.setParentLoaderPriority(true);
		context.setServer(server);
		context.start();
	}

}
