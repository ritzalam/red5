package org.red5.server.stream;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.message.Message;

public class MultiStreamSink extends BaseStreamSink implements IStreamSink, ISinkContainer {

	protected static Log log =
        LogFactory.getLog(MultiStreamSink.class.getName());
	
	protected LinkedList streams = new LinkedList();

	public void connect(IStreamSink stream){
		streams.add(stream);
		stream.setVideoCodec(this.videoCodec);
		stream.setSinkContainer(this);
	}

	public void disconnect(IStreamSink stream) {
		streams.remove(stream);
	}
	
	public void setVideoCodec(IVideoStreamCodec codec) {
		super.setVideoCodec(codec);
		
		// Update already connected streams
		Iterator it = streams.iterator();
		while (it.hasNext()) {
			IStreamSink stream = (IStreamSink) it.next();
			stream.setVideoCodec(codec);
		}
	}

	// push message to all connected streams
	public void enqueue(Message message) {
		final Iterator it = streams.iterator();
		while (it.hasNext()){
			IStreamSink stream = (IStreamSink) it.next();
			if (log.isDebugEnabled())
				log.info("Sending");
			if (stream.canAccept()){
				stream.enqueue(message);
			} else {
				log.warn("Out cant accept");
			}
		}
	}

	public void close(){
		final Iterator it = streams.iterator();
		while (it.hasNext()){
			IStreamSink stream = (IStreamSink) it.next();
			stream.close();
		}
		streams.clear();
		
		super.close();
	}
}
