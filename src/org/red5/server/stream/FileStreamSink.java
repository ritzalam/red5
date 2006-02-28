package org.red5.server.stream;

import java.io.IOException;

import org.red5.io.flv.Tag;
import org.red5.io.flv.TagImpl;
import org.red5.io.flv.Writer;
import org.red5.server.net.rtmp.message.Message;

public class FileStreamSink extends BaseStreamSink implements IStreamSink {

	private Writer writer;
	
	public FileStreamSink(Writer writer){
		this.writer = writer;
	}
	
	public void close() {
		writer.close();
		super.close();
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
