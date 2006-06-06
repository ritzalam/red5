package org.red5.server.net.remoting;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;

import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.servlet.ServletUtils;

/**
 * Client interface for remoting calls.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class RemotingClient {

	protected static Log log = LogFactory.getLog(RemotingClient.class.getName());
	
	/** Default timeout to use. */
	public static final int DEFAULT_TIMEOUT = 30000;

	/** Content type for HTTP requests. */
	private static final String CONTENT_TYPE = "application/x-amf";

	/** Manages HTTP connections. */
	private static HttpConnectionManager connectionMgr = new MultiThreadedHttpConnectionManager();
	
	/** HTTP client for remoting calls. */
	private HttpClient client;
	
	/** Url to connect to. */
	private String url;
	
	/**
	 * Create new remoting client for the given url.
	 * 
	 * @param url
	 * 			url to connect to
	 */
	public RemotingClient(String url) {
		this(url, DEFAULT_TIMEOUT);
	}
	
	/**
	 * Create new remoting client for the given url and given timeout.
	 * 
	 * @param url
	 * 			url to connect to
	 * @param timeout
	 * 			timeout for one request in milliseconds
	 */
	public RemotingClient(String url, int timeout) {
		client = new HttpClient(connectionMgr);
        client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
		this.url = url;
	}
	
	/**
	 * Encode the method call.
	 * 
	 * @param method
	 * @param params
	 * @return
	 */
	private ByteBuffer encodeInvoke(String method, Object[] params) {
		ByteBuffer result = ByteBuffer.allocate(1024);
		result.setAutoExpand(true);
		// XXX: which is the correct version?
		result.putShort((short) 0);
		// No headers
		result.putShort((short) 0);
		// One body
		result.putShort((short) 1);
		
		Output out = new Output(result);
		// Method name
		Output.putString(result, method);
		
		// Client callback for response
		Output.putString(result, "");
		
		// Serialize parameters
		ByteBuffer tmp = ByteBuffer.allocate(1024);
		tmp.setAutoExpand(true);
		Output tmpOut = new Output(tmp);
		Serializer serializer = new Serializer();
		tmpOut.writeStartArray(params.length);
		for (Object param : params) {
			serializer.serialize(tmpOut, param);
		}
		tmpOut.markEndArray();
		tmp.flip();
		
		// Store size and parameters
		result.putInt(tmp.limit());
		result.put(tmp);
		tmp.release();
		
		result.flip();
		return result;
	}

	/**
	 * Skip any headers sent in the response.
	 * 
	 * @param in
	 */
	protected void skipHeaders(ByteBuffer in){
		log.debug("Skip headers");
		int version = in.getUnsignedShort(); // skip the version
		int count = in.getUnsignedShort();
		log.debug("Version: "+version);
		log.debug("Count: "+count);
		for(int i=0; i<count; i++){
			log.debug("Header: "+Input.getString(in));
			boolean required = in.get() == 0x01;
			log.debug("Required: "+required);
			in.skip(in.getInt());
		}
	}
	
	/**
	 * Decode response received from remoting server.
	 * 
	 * @param data
	 * @return
	 */
	private Object decodeResult(ByteBuffer data) {
		// Skip headers, they are not supported yet.
		skipHeaders(data);
		int count = data.getUnsignedShort();
		if (count != 1)
			throw new RuntimeException("Expected exactly one result but got " + count);
		
		// Read return value
		Input input = new Input(data);
		Deserializer deserializer = new Deserializer();
		String target = Input.getString(data);
		String response = Input.getString(data);  // "null"
		int tmp = data.getInt();  // -1
		return deserializer.deserialize(input);
	}
	
	/**
	 * Invoke a method on the remoting server.
	 * 
	 * @param method
	 * @param params
	 * @return
	 */
	public Object invokeMethod(String method, Object[] params) {
		PostMethod post = new PostMethod(this.url);
		ByteBuffer resultBuffer = null;
		ByteBuffer data = encodeInvoke(method, params);
		post.setRequestEntity(new InputStreamRequestEntity(data.asInputStream(), data.limit(), CONTENT_TYPE));
        try {
            int resultCode = client.executeMethod(post);
            if (resultCode % 100 != 2)
            	throw new RuntimeException("Didn't receive success from remoting server."); 
            	
            resultBuffer = ByteBuffer.allocate((int) post.getResponseContentLength());
            ServletUtils.copy(post.getResponseBodyAsStream(), resultBuffer.asOutputStream());
            resultBuffer.flip();
            return decodeResult(resultBuffer);
        } catch (Exception ex) {
        	log.error("Error while invoking remoting method.", ex);
        } finally {
            post.releaseConnection();
            if (resultBuffer != null)
            	resultBuffer.release();
            data.release();
        }
        return null;
	}
	
}
