package org.red5.server.net.protocol;

public class ProtocolState {

	public static final String SESSION_KEY = "protocol_state";
	
	public static byte DECODER_OK = 0x00;
	public static byte DECODER_CONTINUE= 0x01;
	public static byte DECODER_BUFFER = 0x02;
	
	// Classes like the RTMP state object will extend this marker interface
	
	private int decoderBufferAmount = 0;
	private byte decoderState = DECODER_OK;
	
	public int getDecoderBufferAmount() {
		return decoderBufferAmount;
	}
	
	public void bufferDecoding(int amount){
		decoderState = DECODER_BUFFER;
		decoderBufferAmount = amount;
	}
	
	public void continueDecoding(){
		decoderState = DECODER_CONTINUE;
	}
	
	public boolean canStartDecoding(int remaining){
		if(remaining >= decoderBufferAmount){
			return true;
		} else {
			return false;
		}
	}
	
	public void startDecoding(){
		decoderState = DECODER_OK;
		decoderBufferAmount = 0;
	}
	
	public boolean hasDecodedObject(){
		return (decoderState == DECODER_OK);
	}
	
	public boolean canContinueDecoding(){
		return (decoderState != DECODER_BUFFER);
	}
	
}