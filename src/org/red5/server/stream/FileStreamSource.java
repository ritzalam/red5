package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.message.VideoData;

public class FileStreamSource implements ISeekableStreamSource, Constants {

	protected static Log log =
        LogFactory.getLog(FileStreamSource.class.getName());
	
	private ITagReader reader = null;
	private KeyFrameMeta keyFrameMeta = null;
	
	public FileStreamSource(ITagReader reader){
		this.reader = reader;
	}
	
	public void close() {
		reader.close();
	}

	public Message dequeue() {
		
		if(!reader.hasMoreTags()) return null;
		ITag tag = reader.readTag();
		
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

	synchronized public int seek(int ts) {
		if (keyFrameMeta == null) {
			if (!(reader instanceof IKeyFrameDataAnalyzer))
				// Seeking not supported
				return ts;
		
			keyFrameMeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
		}
		
		if (keyFrameMeta.positions.length == 0) {
			// no video keyframe metainfo, it's an audio-only FLV
			// we skip the seek for now.
			// TODO add audio-seek capability
			return ts;
		}
		int frame = 0;
		for (int i = 0; i < keyFrameMeta.positions.length; i++) {
			if (keyFrameMeta.timestamps[i] > ts) break;
			frame = i;
		}
		reader.position(keyFrameMeta.positions[frame]);
		return keyFrameMeta.timestamps[frame];
	}
}
