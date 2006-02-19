package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.flv.FLV;
import org.red5.io.flv.Reader;
import org.red5.io.flv.Tag;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.message.VideoData;

public class FileStreamSource implements IStreamSource, Constants {

	protected static Log log =
        LogFactory.getLog(FileStreamSource.class.getName());
	
	private Reader reader = null;
	
	public FileStreamSource(Reader reader){
		this.reader = reader;
	}
	
	public void close() {
		reader.close();
	}

	public Message dequeue() {
		
		if(!reader.hasMoreTags()) return null;
		Tag tag = reader.readTag();
		
		Message msg = null;
		switch(tag.getDataType()){
		case TYPE_AUDIO_DATA:
			msg = new AudioData();
			break;
		case TYPE_VIDEO_DATA:
			msg = new VideoData();
			break;
		case TYPE_INVOKE:
			msg = new Invoke();
			break;
		case TYPE_NOTIFY:
			msg = new Notify();
			break;
		default:
			log.warn("Unexpected type? "+tag.getDataType());
			msg = new Unknown(tag.getDataType());
			break;
		}
		msg.setData(tag.getBody());
		msg.setTimestamp(tag.getTimestamp());
		msg.setSealed(true);
		return msg;
	}

	public boolean hasMore() {
		return reader.hasMoreTags();
	}
	
}
