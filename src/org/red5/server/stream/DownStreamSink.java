package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;

public class DownStreamSink extends BaseStreamSink implements IStreamSink, Constants {

	protected static Log log =
        LogFactory.getLog(DownStreamSink.class.getName());
	
	private Channel video;
	private Channel audio;
	private Channel data;
	
	public DownStreamSink(Channel video, Channel audio, Channel data){
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
	
	public void enqueue(Message message){
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
