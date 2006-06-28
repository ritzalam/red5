package org.red5.server.net.rtmp.status;

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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.BeanMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;

public class StatusObjectService implements StatusCodes {
	
	// Note all status object should aim to be under 128 bytes
	
	protected static Log log =
        LogFactory.getLog(StatusObjectService.class.getName());
	
	protected Serializer serializer;
	
	protected Map<String, StatusObject> statusObjects;
	protected Map<String, byte[]> cachedStatusObjects; 

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public void initialize(){
		log.debug("Loading status objects");
		loadStatusObjects();
		log.debug("Caching status objects");
		cacheStatusObjects();
		log.debug("Status service ready");
	}
	
	public void loadStatusObjects(){
		statusObjects = new HashMap<String, StatusObject>();
		
		statusObjects.put(NC_CALL_FAILED,new StatusObject(NC_CALL_FAILED,StatusObject.ERROR,""));
		statusObjects.put(NC_CALL_BADVERSION,new StatusObject(NC_CALL_BADVERSION,StatusObject.ERROR,""));
		
		statusObjects.put(NC_CONNECT_APPSHUTDOWN,new StatusObject(NC_CONNECT_APPSHUTDOWN,StatusObject.ERROR,""));
		statusObjects.put(NC_CONNECT_CLOSED,new StatusObject(NC_CONNECT_CLOSED,StatusObject.STATUS,""));
		statusObjects.put(NC_CONNECT_FAILED,new StatusObject(NC_CONNECT_FAILED,StatusObject.ERROR,""));
		statusObjects.put(NC_CONNECT_REJECTED,new StatusObject(NC_CONNECT_REJECTED,StatusObject.ERROR,""));
		statusObjects.put(NC_CONNECT_SUCCESS,new StatusObject(NC_CONNECT_SUCCESS,StatusObject.STATUS,""));
		
		statusObjects.put(NS_CLEAR_SUCCESS,new StatusObject(NS_CLEAR_SUCCESS,StatusObject.STATUS,""));
		statusObjects.put(NS_CLEAR_FAILED,new StatusObject(NS_CLEAR_FAILED,StatusObject.ERROR,""));
		
		statusObjects.put(NS_PUBLISH_START,new StatusObject(NS_PUBLISH_START,StatusObject.STATUS,""));
		statusObjects.put(NS_PUBLISH_BADNAME,new StatusObject(NS_PUBLISH_BADNAME,StatusObject.ERROR,""));
		statusObjects.put(NS_FAILED,new StatusObject(NS_FAILED,StatusObject.ERROR,""));
		statusObjects.put(NS_UNPUBLISHED_SUCCESS,new StatusObject(NS_UNPUBLISHED_SUCCESS,StatusObject.STATUS,""));
		
		statusObjects.put(NS_RECORD_START,new StatusObject(NS_RECORD_START,StatusObject.STATUS,""));
		statusObjects.put(NS_RECOED_NOACCESS,new StatusObject(NS_RECOED_NOACCESS,StatusObject.ERROR,""));
		statusObjects.put(NS_RECORD_STOP,new StatusObject(NS_RECORD_STOP,StatusObject.STATUS,""));
		statusObjects.put(NS_RECORD_FAILED,new StatusObject(NS_RECORD_FAILED,StatusObject.ERROR,""));
		
		statusObjects.put(NS_PLAY_INSUFFICIENT_BW,new RuntimeStatusObject(NS_PLAY_INSUFFICIENT_BW,StatusObject.WARNING,""));
		statusObjects.put(NS_PLAY_START, new RuntimeStatusObject(NS_PLAY_START,StatusObject.STATUS,""));
		statusObjects.put(NS_PLAY_STREAMNOTFOUND,new RuntimeStatusObject(NS_PLAY_STREAMNOTFOUND,StatusObject.ERROR,""));
		statusObjects.put(NS_PLAY_STOP,new RuntimeStatusObject(NS_PLAY_STOP,StatusObject.STATUS,""));
		statusObjects.put(NS_PLAY_FAILED,new RuntimeStatusObject(NS_PLAY_FAILED,StatusObject.ERROR,""));
		statusObjects.put(NS_PLAY_RESET,new RuntimeStatusObject(NS_PLAY_RESET,StatusObject.STATUS,""));
		statusObjects.put(NS_PLAY_PUBLISHNOTIFY,new RuntimeStatusObject(NS_PLAY_PUBLISHNOTIFY,StatusObject.STATUS,""));
		statusObjects.put(NS_PLAY_UNPUBLISHNOTIFY,new RuntimeStatusObject(NS_PLAY_UNPUBLISHNOTIFY,StatusObject.STATUS,""));
		
		statusObjects.put(NS_DATA_START,new StatusObject(NS_DATA_START,StatusObject.STATUS,""));
		
		statusObjects.put(APP_SCRIPT_ERROR,new StatusObject(APP_SCRIPT_ERROR,StatusObject.STATUS,""));
		statusObjects.put(APP_SCRIPT_WARNING,new StatusObject(APP_SCRIPT_WARNING,StatusObject.STATUS,""));
		statusObjects.put(APP_RESOURCE_LOWMEMORY,new StatusObject(APP_RESOURCE_LOWMEMORY,StatusObject.STATUS,""));
		statusObjects.put(APP_SHUTDOWN,new StatusObject(APP_SHUTDOWN,StatusObject.STATUS,""));
		statusObjects.put(APP_GC,new StatusObject(APP_GC,StatusObject.STATUS,""));
		
	}
	
	public void cacheStatusObjects(){
		
		cachedStatusObjects = new HashMap<String, byte[]>();
		
		Iterator<String> it = statusObjects.keySet().iterator();
		
		String statusCode;
		ByteBuffer out = ByteBuffer.allocate(256);
		out.setAutoExpand(true);
		
		while(it.hasNext()){
			statusCode = it.next();
			StatusObject statusObject = statusObjects.get(statusCode);
			if(statusObject instanceof RuntimeStatusObject) continue;
			serializeStatusObject(out, statusObject);
			out.flip();
			log.debug(HexDump.formatHexDump(out.getHexDump()));
			byte[] cachedBytes = new byte[out.limit()];
			out.get(cachedBytes);
			out.clear();
			cachedStatusObjects.put(statusCode,cachedBytes);
		}
		out.release();
	}
	
	public void serializeStatusObject(ByteBuffer out, StatusObject statusObject){
		Map statusMap = new BeanMap(statusObject);
		Output output = new Output(out);
		serializer.serialize(output, statusMap);
	}
	
	public StatusObject getStatusObject(String statusCode){
		return statusObjects.get(statusCode);
	}
	
	public byte[] getCachedStatusObjectAsByteArray(String statusCode){
		return cachedStatusObjects.get(statusCode);
	}
	
	/*
	public StatusObject getStatusObject(String statusCode, String message){
		return null;
	}
	
	public StatusObject getStatusObject(String statusCode, Throwable ex){
		return null;
	}
	
	public StatusObject getStatusObject(String statusCode, String message, Throwable ex){
		return null;
	}
	*/
}
