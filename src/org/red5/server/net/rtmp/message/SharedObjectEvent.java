package org.red5.server.net.rtmp.message;


public class SharedObjectEvent implements Constants {

	private byte type = 1;
	private String key = null;
	private Object value = null;
	
	public  SharedObjectEvent(byte t, String key, Object value){
		type = t;
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public byte getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public void setValue(Object value) {
		this.value = value;
	}
		
	public String toString(){
		final StringBuffer sb = new StringBuffer();
		sb.append(typeToString(type));
		if(key != null) sb.append(" | key: ").append(key);
		if(value != null) sb.append(" value: ").append(value);
		return sb.toString();
	}
	

	public static String typeToString(byte type){
		switch(type){
			case SO_CONNECT:
				return "Connect";
			case SO_CONNECT_OK:
				return "Connect OK";
			case SO_CLIENT_DELETE_DATA:
				return "Client Delete Data";
			case SO_CLIENT_INITIAL_DATA:
				return "Client Initial Data";
			case SO_CLIENT_STATUS:
				return "Status";
			case SO_CLIENT_UPDATE_DATA:
				return "Client Update Data";
			case SO_CLIENT_UPDATE_ATTRIBUTE:
				return "Client Update Attribute";
			case SO_SET_ATTRIBUTE:
				return "Set Attribute";
			case SO_SEND_MESSAGE:
				return "Send Message";
			case SO_CLEAR:
				return "Clear";
		}
		return "Unknown " + type;
	}
}
