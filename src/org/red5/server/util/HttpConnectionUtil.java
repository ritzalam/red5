package org.red5.server.util;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for using HTTP connections.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class HttpConnectionUtil {

	private static Logger log = LoggerFactory.getLogger(HttpConnectionUtil.class);

	private static final String userAgent = "Mozilla/4.0 (compatible; Red5 Server)";
	
	private static ThreadSafeClientConnManager connectionManager;
	
	private static int connectionTimeout = 7000;
	
	static {
		// Create an HttpClient with the ThreadSafeClientConnManager.
		// This connection manager must be used if more than one thread will
		// be using the HttpClient.
		connectionManager = new ThreadSafeClientConnManager();
		connectionManager.setMaxTotal(40);
	}

	/**
	 * Returns a client with all our selected properties / params.
	 * @return
	 */
	public static final DefaultHttpClient getClient() {
		// create a singular HttpClient object
		DefaultHttpClient client = new DefaultHttpClient(connectionManager);
		// dont retry
		client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
		// get the params for the client
		HttpParams params = client.getParams();
		// establish a connection within x seconds
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, connectionTimeout);
		// no redirects
		params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
		// set custom ua
		params.setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
		// set the proxy if the user has one set
		if ((System.getProperty("http.proxyHost") != null) && (System.getProperty("http.proxyPort") != null)) {
            HttpHost proxy = new HttpHost(System.getProperty("http.proxyHost").toString(), Integer.valueOf(System.getProperty("http.proxyPort")));
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
		return client;
	}
	
	/**
	 * Logs details about the request error.
	 * 
	 * @param response
	 * @param entity
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void handleError(HttpResponse response) throws ParseException, IOException {
		log.debug("{}", response.getStatusLine().toString());
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			log.debug("{}", EntityUtils.toString(entity));
		}
	}	
	
	/**
	 * @return the connectionTimeout
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		HttpConnectionUtil.connectionTimeout = connectionTimeout;
	}
	
}
