package org.red5.server.stream;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.message.Message;

public class MultiStreamSink implements IStreamSink {

	protected static Log log =
        LogFactory.getLog(MultiStreamSink.class.getName());
	
	protected LinkedList outs = new LinkedList();
	private IVideoStreamCodec videoCodec = null;

	public void connect(IStreamSink out){
		outs.add(out);
		out.setVideoCodec(this.videoCodec);
	}

	public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
		
		// Update already connected streams
		Iterator it = outs.iterator();
		while (it.hasNext()) {
			IStreamSink stream = (IStreamSink) it.next();
			stream.setVideoCodec(codec);
		}
	}

	public boolean canAccept(){
		return true;
	}
	
	// push message to all connected streams
	public void enqueue(Message message) {
		final Iterator it = outs.iterator();
		while(it.hasNext()){
			IStreamSink out = (IStreamSink) it.next();
			if(log.isDebugEnabled())
				log.info("Sending");
			if(out.canAccept()){
				out.enqueue(message);
			} else {
				log.warn("Out cant accept");
			}
		}
	}

	public void close(){
		final Iterator it = outs.iterator();
		while(it.hasNext()){
			IStreamSink out = (IStreamSink) it.next();
			out.close();
		}
		outs.clear();
	}
	
}
