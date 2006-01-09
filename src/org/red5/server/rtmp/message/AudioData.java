package org.red5.server.rtmp.message;

public class AudioData extends Message {

	private static final int INITIAL_CAPACITY = 2048;
	
	public AudioData(){
		super(TYPE_AUDIO_DATA, INITIAL_CAPACITY);
	}

	public String toString(){
		return "Video ts: "+getTimestamp()+" size: "+getData().limit(); 
	}
	
}
