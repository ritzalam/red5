package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;

public class OutputStream extends BaseStream implements IEventDispatcher, Constants {

	protected static Log log =
        LogFactory.getLog(OutputStream.class.getName());
	
	private Channel video;
	private Channel audio;
	private Channel data;
	
	public OutputStream(IScope scope, Channel video, Channel audio, Channel data){
		super(scope);
		this.video = video;
		this.audio = audio;
		this.data = data;
	}
	
	public void close() {
		this.video.close();
		this.audio.close();
		this.data.close();
		super.close();
	}

	public boolean hasAudio() {
		return (audio != null);
	}
	
	public boolean hasVideo() {
		return (video != null);
	}
	
	public String getAudioCodecName() {
		// TODO: implement this
		return null;
	}
	
	public void dispatchEvent(Object obj){
		if (!(obj instanceof Message))
			return;
		
		final Message message = (Message) obj;
		//log.info("out ts:"+message.getTimestamp());
		switch(message.getDataType()){
		case TYPE_VIDEO_DATA:
		case TYPE_STREAM_METADATA:
			//log.debug("write video");
			video.write(message);
			break;
		case TYPE_AUDIO_DATA:
			audio.write(message);
			//log.debug("write audio");
			break;
		default:
			data.write(message);
			//log.debug("write other");
			break;
		}
		position = message.getTimestamp();
	}

	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
			(event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		dispatchEvent(event.getObject());
	}
	
	public Channel getAudio() {
		return audio;
	}

	public Channel getData() {
		return data;
	}

	public Channel getVideo() {
		return video;
	}
}
