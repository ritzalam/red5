package org.red5.server.stream;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Message;

public class BroadcastStream extends Stream
	implements IBroadcastStream {

	protected static Log log =
        LogFactory.getLog(BroadcastStream.class.getName());
	
	protected LinkedList<ISubscriberStream> streams = new LinkedList<ISubscriberStream>();

	public BroadcastStream(IScope scope, RTMPConnection conn) {
		super(scope, conn, Stream.MODE_LIVE);
	}
	
	public void subscribe(ISubscriberStream stream){
		synchronized (streams) {
			streams.add(stream);
		}
		
		if (stream instanceof BaseStream)
			((BaseStream) stream).setVideoCodec(this.videoCodec);
		
		if (stream instanceof SubscriberStream)
			((SubscriberStream) stream).setBroadcastStream(this);
	}

	public void unsubscribe(ISubscriberStream stream) {
		synchronized (streams) {
			streams.remove(stream);
		}
		
		if (stream instanceof SubscriberStream)
			((SubscriberStream) stream).setBroadcastStream(null);
	}
	
	public void setVideoCodec(IVideoStreamCodec codec) {
		super.setVideoCodec(codec);
		
		// Update already connected streams
		synchronized (streams) {
			Iterator<ISubscriberStream> it = streams.iterator();
			while (it.hasNext()) {
				ISubscriberStream stream = it.next();
				if (stream instanceof BaseStream)
					((BaseStream) stream).setVideoCodec(codec);
			}
		}
	}

	// push message to all connected streams
	public void dispatchEvent(Object message) {
		if (!(message instanceof Message))
			return;
		
		synchronized (streams) {
			final Iterator<ISubscriberStream> it = streams.iterator();
			while (it.hasNext()){
				ISubscriberStream stream = it.next();
				log.debug("Sending");
				if (stream instanceof IEventDispatcher)
					((IEventDispatcher) stream).dispatchEvent(message);
			}
		}
	}

	// push message to all connected streams
	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
			(event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		dispatchEvent(event.getObject());
	}

	public void close(){
		synchronized (streams) {
			final Iterator<ISubscriberStream> it = streams.iterator();
			while (it.hasNext()){
				ISubscriberStream stream = it.next();
				stream.close();
			}
			streams.clear();
		}
		
		super.close();
	}
}
