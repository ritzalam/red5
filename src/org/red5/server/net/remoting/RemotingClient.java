package org.red5.server.net.remoting;

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

import java.util.HashMap;
import java.util.Map;

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
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.net.servlet.ServletUtils;
import org.red5.server.pooling.ThreadPool;
import org.red5.server.pooling.WorkerThread;

/**
 * Client interface for remoting calls.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class RemotingClient {

	protected static Log log = LogFactory
			.getLog(RemotingClient.class.getName());

	/** Default timeout to use. */
	public static final int DEFAULT_TIMEOUT = 30000;

	/** Content type for HTTP requests. */
	private static final String CONTENT_TYPE = "application/x-amf";

	/** Name of the bean defining the thread pool. */
	private static final String POOL_BEAN_ID = "remotingPool";

	/** Manages HTTP connections. */
	private static HttpConnectionManager connectionMgr = new MultiThreadedHttpConnectionManager();

	/** HTTP client for remoting calls. */
	private HttpClient client;

	/** Url to connect to. */
	private String url;

	/** Additonal string to use while connecting. */
	private String appendToUrl = "";

	/** Headers to send to the server. */
	protected Map<String, RemotingHeader> headers = new HashMap<String, RemotingHeader>();

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
		client.getHttpConnectionManager().getParams().setConnectionTimeout(
				timeout);
		this.url = url;
	}

	/**
	 * Encode the method call.
	 * 
	 * @param method
	 * @param params
	 * @return
	 */
	private synchronized ByteBuffer encodeInvoke(String method, Object[] params) {
		ByteBuffer result = ByteBuffer.allocate(1024);
		result.setAutoExpand(true);
		Output out = new Output(result);

		// XXX: which is the correct version?
		result.putShort((short) 0);
		// Headers
		result.putShort((short) headers.size());
		for (RemotingHeader header : headers.values()) {
			Output.putString(result, header.name);
			result.put(header.required ? (byte) 0x01 : (byte) 0x00);

			ByteBuffer tmp = ByteBuffer.allocate(1024);
			tmp.setAutoExpand(true);
			Output tmpOut = new Output(tmp);
			Serializer tmpSer = new Serializer();
			tmpSer.serialize(tmpOut, header.data);
			tmp.flip();
			// Size of header data
			result.putInt(tmp.limit());
			// Header data
			result.put(tmp);
			tmp.release();
		}
		// One body
		result.putShort((short) 1);

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
	 * Process any headers sent in the response.
	 * 
	 * @param in
	 */
	protected void processHeaders(ByteBuffer in) {
		int version = in.getUnsignedShort(); // skip the version
		int count = in.getUnsignedShort();
		Deserializer deserializer = new Deserializer();
		Input input = new Input(in);
		for (int i = 0; i < count; i++) {
			String name = Input.getString(in);
			boolean required = (in.get() == 0x01);
			int len = in.getInt();
			Object value = deserializer.deserialize(input);

			// XXX: this is pretty much untested!!!
			if (name.equals(RemotingHeader.APPEND_TO_GATEWAY_URL)) {
				// Append string to gateway url
				appendToUrl = (String) value;
			} else if (name.equals(RemotingHeader.REPLACE_GATEWAY_URL)) {
				// Replace complete gateway url
				url = (String) value;
				// XXX: reset the <appendToUrl< here?
			} else if (name.equals(RemotingHeader.PERSISTENT_HEADER)) {
				// Send a new header with each following request
				if (value instanceof Map) {
					Map<String, Object> valueMap = (Map) value;
					RemotingHeader header = new RemotingHeader(
							(String) valueMap.get("name"), (Boolean) valueMap
									.get("mustUnderstand"), valueMap
									.get("data"));
					headers.put(header.name, header);
				} else {
					log.error("Expected Map but received " + value);
				}
			} else {
				log.warn("Unsupported remoting header \"" + name
						+ "\" received with value " + value);
			}
		}
	}

	/**
	 * Decode response received from remoting server.
	 * 
	 * @param data
	 * @return
	 */
	private synchronized Object decodeResult(ByteBuffer data) {
		processHeaders(data);
		int count = data.getUnsignedShort();
		if (count != 1) {
			throw new RuntimeException("Expected exactly one result but got "
					+ count);
		}

		// Read return value
		Input input = new Input(data);
		Deserializer deserializer = new Deserializer();
		String target = Input.getString(data);
		String response = Input.getString(data); // "null"
		int tmp = data.getInt(); // -1
		return deserializer.deserialize(input);
	}

	/**
	 * Send authentication data with each remoting request.
	 * 
	 * @param userid
	 * @param password
	 */
	public synchronized void setCredentials(String userid, String password) {
		Map<String, String> data = new HashMap<String, String>();
		data.put("userid", userid);
		data.put("password", password);
		RemotingHeader header = new RemotingHeader(RemotingHeader.CREDENTIALS,
				true, data);
		headers.put(RemotingHeader.CREDENTIALS, header);
	}

	/**
	 * Stop sending authentication data.
	 *
	 */
	public synchronized void resetCredentials() {
		removeHeader(RemotingHeader.CREDENTIALS);
	}

	/**
	 * Send an additional header to the server.
	 * 
	 * @param name
	 * @param required
	 * @param value
	 */
	public synchronized void addHeader(String name, boolean required,
			Object value) {
		RemotingHeader header = new RemotingHeader(name, required, value);
		headers.put(name, header);
	}

	/**
	 * Stop sending a given header.
	 * 
	 * @param name
	 */
	public synchronized void removeHeader(String name) {
		headers.remove(name);
	}

	/**
	 * Invoke a method synchronously on the remoting server.
	 * 
	 * @param method
	 * @param params
	 * @return the result of the method call
	 */
	public Object invokeMethod(String method, Object[] params) {
		PostMethod post = new PostMethod(this.url + appendToUrl);
		ByteBuffer resultBuffer = null;
		ByteBuffer data = encodeInvoke(method, params);
		post.setRequestEntity(new InputStreamRequestEntity(
				data.asInputStream(), data.limit(), CONTENT_TYPE));
		try {
			int resultCode = client.executeMethod(post);
			if (resultCode / 100 != 2) {
				throw new RuntimeException(
						"Didn't receive success from remoting server.");
			}

			resultBuffer = ByteBuffer.allocate((int) post
					.getResponseContentLength());
			ServletUtils.copy(post.getResponseBodyAsStream(), resultBuffer
					.asOutputStream());
			resultBuffer.flip();
			Object result = decodeResult(resultBuffer);
			if (result instanceof RecordSet) {
				// Make sure we can retrieve paged results
				((RecordSet) result).setRemotingClient(this);
			}
			return result;
		} catch (Exception ex) {
			log.error("Error while invoking remoting method.", ex);
		} finally {
			post.releaseConnection();
			if (resultBuffer != null) {
				resultBuffer.release();
			}
			data.release();
		}
		return null;
	}

	/**
	 * Invoke a method asynchronously on the remoting server.
	 * 
	 * @param method
	 * @param methodParams
	 * @param callback
     * @param methodParams
	 */
	public void invokeMethod(String method, Object[] methodParams,
			IRemotingCallback callback) {
		IScope scope = Red5.getConnectionLocal().getScope();

		ThreadPool pool = (ThreadPool) scope.getContext().getBean(POOL_BEAN_ID);
		try {
			WorkerThread wt = (WorkerThread) pool.borrowObject();
			Object[] params = new Object[] { this, method, methodParams,
					callback };
			Class[] paramTypes = new Class[] { RemotingClient.class,
					String.class, Object[].class, IRemotingCallback.class };
			wt
					.execute(
							"org.red5.server.net.remoting.RemotingClient$RemotingWorker",
							"executeTask", params, paramTypes, null);
		} catch (Exception err) {
			log.warn("Exception invoking method: " + method);
		}
	}

	/**
	 * Worker class that is used for asynchronous remoting calls.
	 */
	public static class RemotingWorker {

		public void executeTask(RemotingClient client, String method,
				Object[] params, IRemotingCallback callback) {
			try {
				Object result = client.invokeMethod(method, params);
				callback.resultReceived(client, method, params, result);
			} catch (Exception err) {
				callback.errorReceived(client, method, params, err);
			}
		}
	}

}
