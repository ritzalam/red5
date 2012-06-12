/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

/**
 * Enum for RTMP types.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum RTMPType {
	
	TYPE_CHUNK_SIZE(0x01), TYPE_ABORT(0x02), TYPE_BYTES_READ(0x03), TYPE_PING(0x04), TYPE_SERVER_BANDWIDTH(0x05), TYPE_CLIENT_BANDWIDTH(0x06), TYPE_EDGE_ORIGIN(0x07), TYPE_AUDIO_DATA(0x08), TYPE_VIDEO_DATA(0x09), TYPE_FLEX_SHARED_OBJECT(0x10), TYPE_FLEX_MESSAGE(0x11), TYPE_NOTIFY(0x12), TYPE_SHARED_OBJECT(0x13), TYPE_INVOKE(0x14), TYPE_AGGREGATE(0x16);
    
    private final byte type;
    
    RTMPType(byte type) {
    	this.type = type;
    }

    RTMPType(int type) {
    	this.type = (byte) type;
    }

	public byte getType() {
		return type;
	}

	public static String valueOf(byte dataType) {
		return RTMPType.values()[(int) dataType].name();
	}    
    
}
