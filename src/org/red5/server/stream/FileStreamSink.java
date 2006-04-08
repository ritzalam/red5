package org.red5.server.stream;

import java.io.IOException;

import org.red5.io.flv.ITag;
import org.red5.io.flv.IWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.net.rtmp.message.Message;

public class FileStreamSink extends BaseStreamSink implements IStreamSink {

	private IWriter writer;
	
	public FileStreamSink(IWriter writer){
		this.writer = writer;
	}
	
	public void close() {
		writer.close();
		super.close();
	}

	public void enqueue(Message message) {
		
		ITag tag = new Tag();
		
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
