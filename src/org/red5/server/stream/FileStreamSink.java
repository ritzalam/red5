package org.red5.server.stream;

import java.io.IOException;

import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.net.rtmp.message.Message;

public class FileStreamSink extends SubscriberStream implements IEventDispatcher {

	private ITagWriter writer;
	
	public FileStreamSink(IScope scope, ITagWriter writer){
		super(scope, null);
		this.writer = writer;
	}
	
	public void close() {
		writer.close();
		super.close();
	}

	public void dispatchEvent(Object obj) {
		if (!(obj instanceof Message))
			return;
		
		final Message message = (Message) obj;
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
		position = message.getTimestamp();
	}
	
	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
			(event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		dispatchEvent(event.getObject());
	}
}
