package org.red5.server.stream;

import java.io.IOException;

import org.red5.io.flv.Tag;
import org.red5.io.flv.TagImpl;
import org.red5.io.flv.Writer;
import org.red5.server.net.rtmp.message.Message;

public class FileStreamSink implements IStreamSink {

	private Writer writer;
	
	public FileStreamSink(Writer writer){
		this.writer = writer;
	}
	
	public boolean canAccept() {
		return true;
	}

	public void close() {
		writer.close();
	}

	public void setVideoCodec(IVideoStreamCodec codec) {
		// nothing to do here...
	}
	
	public void enqueue(Message message) {
		
		Tag tag = new TagImpl();
		
		tag.setDataType(message.getDataType());
		tag.setTimestamp(message.getTimestamp());
		tag.setBodySize(message.getData().limit());
		tag.setBody(message.getData());
		
		try {
			writer.writeTag(tag);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
