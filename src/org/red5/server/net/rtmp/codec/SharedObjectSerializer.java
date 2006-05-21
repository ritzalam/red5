package org.red5.server.net.rtmp.codec;

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

/**
 * Data serializer for shared objects.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.BeanMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;

public class SharedObjectSerializer extends Serializer {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(SharedObjectSerializer.class.getName());
	

	/**
	 * Writes a map to the output.
	 * 
	 * @param out
	 * 			output stream
	 * @param map
	 * 			Map object to serialize
	 */
	public void writeMap(Output out, Map map){
		if(log.isDebugEnabled()) {
			log.debug("writeMap");
		}
		
		final Set set = map.entrySet();
		// NOTE: we need to encode maps as objects for shared objects
		out.writeStartObject(null);
		Iterator it = set.iterator();
		boolean isBeanMap = (map instanceof BeanMap);
		while(it.hasNext()){
			Map.Entry entry = (Map.Entry) it.next();
			if(isBeanMap && ((String)entry.getKey()).equals("class")) continue;
			out.writeItemKey(entry.getKey().toString());
			serialize(out,entry.getValue());
			if(it.hasNext()) out.markPropertySeparator();
		}
		out.markEndObject();
	}
	
}
