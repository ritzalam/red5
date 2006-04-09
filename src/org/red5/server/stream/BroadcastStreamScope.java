package org.red5.server.stream;

import org.red5.server.BasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Message;

public class BroadcastStreamScope extends BasicScope implements
		IBroadcastStream, IBasicScope, IEventDispatcher {

	private BroadcastStream stream;
	
	public BroadcastStreamScope(IScope parent, RTMPConnection conn, String name) {
		super(parent, IBroadcastStream.TYPE, name, false);
		stream = new BroadcastStream(parent, conn);
	}
	
	public int getStreamId() {
		return stream.getStreamId();
	}
	
	public void setStreamId(int streamId) {
		this.stream.setStreamId(streamId);
	}
	
	public void setDownstream(OutputStream downstream) {
		this.stream.setDownstream(downstream);
	}
	
	public void subscribe(ISubscriberStream stream) {
		this.stream.subscribe(stream);
	}

	public void unsubscribe(ISubscriberStream stream) {
		this.stream.unsubscribe(stream);
	}

	public int getCurrentPosition() {
		return stream.getCurrentPosition();
	}

	public boolean hasAudio() {
		return stream.hasAudio();
	}

	public boolean hasVideo() {
		return stream.hasVideo();
	}

	public String getVideoCodecName() {
		return stream.getVideoCodecName();
	}

	public String getAudioCodecName() {
		return stream.getAudioCodecName();
	}

	public IScope getScope() {
		return stream.getScope();
	}

	public void close() {
		stream.close();
	}
	
	public void dispatchEvent(Object event) {
		if (!(event instanceof Message))
			return;
		
		stream.publish((Message) event);
	}
	
	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
			(event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		dispatchEvent(event.getObject());
	}
}
