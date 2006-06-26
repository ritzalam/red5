package org.red5.server.net.rtmp.message;

import org.red5.server.so.ISharedObjectEvent.Type;

public class SharedObjectTypeMapping {

	public static final Type[] typeMap = new Type[]{
		null,
		Type.SERVER_CONNECT, // 01
		Type.SERVER_DISCONNECT, // 02
		Type.SERVER_SET_ATTRIBUTE, // 03
		Type.CLIENT_UPDATE_DATA, // 04 
		Type.CLIENT_UPDATE_ATTRIBUTE, // 05
		Type.SERVER_SEND_MESSAGE, // 06
		Type.CLIENT_STATUS, // 07
		Type.CLIENT_CONNECT, // 08
		Type.CLIENT_DELETE_DATA, // 09
		Type.SERVER_DELETE_ATTRIBUTE, // 0A
		Type.CLIENT_INITIAL_DATA, // 0B
	};
	
	public static Type toType(byte rtmpType){
		return typeMap[rtmpType];
	}
	
	public static byte toByte(Type type){
		switch(type){
		case SERVER_CONNECT: return 0x01;
		case SERVER_DISCONNECT: return 0x02;
		case SERVER_SET_ATTRIBUTE: return 0x03;
		case CLIENT_UPDATE_DATA: return 0x04;
		case CLIENT_UPDATE_ATTRIBUTE: return 0x05;
		case SERVER_SEND_MESSAGE: return 0x06;
		case CLIENT_STATUS: return 0x07;
		case CLIENT_CONNECT: return 0x08;
		case CLIENT_DELETE_DATA: return 0x09;
		case SERVER_DELETE_ATTRIBUTE: return 0x0A;
		case CLIENT_INITIAL_DATA: return 0x0B;
		default: return 0x00;
		}
	}
	
	public static String toString(Type type){
		switch(type){
		case SERVER_CONNECT: return "server connect";
		case SERVER_DISCONNECT: return "server_disconnect";
		case SERVER_SET_ATTRIBUTE: return "server_set_attribute";
		case CLIENT_UPDATE_DATA: return "client_update_data";
		case CLIENT_UPDATE_ATTRIBUTE: return "client_update_attribute";
		case SERVER_SEND_MESSAGE: return "server_send_message";
		case CLIENT_STATUS: return "client_status";
		case CLIENT_CONNECT: return "client_connect";
		case CLIENT_DELETE_DATA: return "client_delete_data";
		case SERVER_DELETE_ATTRIBUTE: return "server_delete_attribute";
		case CLIENT_INITIAL_DATA: return "client_initial_data";
		default: return "unknown";
		}
	}
	
}