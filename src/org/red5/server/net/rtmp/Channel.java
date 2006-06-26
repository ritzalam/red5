package org.red5.server.net.rtmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;

public class Channel {

	protected static Log log =
        LogFactory.getLog(Channel.class.getName());
	
	private RTMPConnection connection = null;
	private byte id = 0;
	//private Stream stream;

	public Channel(RTMPConnection conn, byte channelId){
		connection = conn;
		id = channelId;
	}
	
	public void close() {
		connection.closeChannel(id);
	}
	
	public byte getId(){
		return id;
	}
	
	/*
	public Stream getStream() {
		return stream;
	}
	*/

	public void write(IRTMPEvent event){
		final IClientStream stream = connection.getStreamByChannelId(id);
		/*
		final int streamId = (
				stream==null || (
						message.getDataType() != Constants.TYPE_AUDIO_DATA && 
						message.getDataType() != Constants.TYPE_VIDEO_DATA )
				) ? 0 : stream.getStreamId();
				*/
		final int streamId = ( stream==null ) ? 0 : stream.getStreamId();
		write(event, streamId);
	}
	
	private void write(IRTMPEvent event, int streamId){
		
		final Header header = new Header();
		final Packet packet = new Packet(header, event);
		
		header.setChannelId(id);
		header.setTimer(event.getTimestamp());
		header.setStreamId(streamId);
		header.setDataType(event.getDataType());
		
		// should use RTMPConnection specific method.. 
		connection.write(packet);
		
	}

	public void sendStatus(Status status) {
		final boolean andReturn = !status.getCode().equals(Status.NS_DATA_START);
		final Invoke invoke;
		if (andReturn) {
			final PendingCall call = new PendingCall(null,"onStatus",new Object[]{status});
			invoke = new Invoke();
			invoke.setInvokeId(1);
			invoke.setCall(call);
		} else {
			final Call call = new Call(null,"onStatus",new Object[]{status});
			invoke = (Invoke) new Notify();
			invoke.setInvokeId(1);
			((Notify) invoke).setCall(call);
		}
		write(invoke);
	}

}
