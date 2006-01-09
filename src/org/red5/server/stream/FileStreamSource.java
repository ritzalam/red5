package org.red5.server.stream;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.io.flv2.FLVReader;
import org.red5.server.io.flv2.FLVTag;
import org.red5.server.rtmp.message.AudioData;
import org.red5.server.rtmp.message.Constants;
import org.red5.server.rtmp.message.Invoke;
import org.red5.server.rtmp.message.Message;
import org.red5.server.rtmp.message.Notify;
import org.red5.server.rtmp.message.Unknown;
import org.red5.server.rtmp.message.VideoData;

public class FileStreamSource implements IStreamSource, Constants {

	protected static Log log =
        LogFactory.getLog(FileStreamSource.class.getName());
	
	private FLVReader reader = null;
	
	public FileStreamSource(File file){
		try {
			reader = new FLVReader(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		// should close reader ?, or remove reader ?
	}

	public Message dequeue() {
		final FLVTag tag = reader.getNextTag();
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
	
	int i=0;

	public boolean hasMore() {
		//return false;
		//if(i++>20) return false;
		return reader.hasMoreTags();
	}
	
}
