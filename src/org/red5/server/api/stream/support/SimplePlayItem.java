package org.red5.server.api.stream.support;

import org.red5.server.api.stream.IPlayItem;
import org.red5.server.messaging.IMessageInput;

public class SimplePlayItem implements IPlayItem {
	private long length;
	private String name;
	private long start;
	private IMessageInput msgInput;
	
	public long getLength() {
		return length;
	}

	public IMessageInput getMessageInput() {
		return msgInput;
	}

	public String getName() {
		return name;
	}

	public long getStart() {
		return start;
	}

	public IMessageInput getMsgInput() {
		return msgInput;
	}

	public void setMsgInput(IMessageInput msgInput) {
		this.msgInput = msgInput;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStart(long start) {
		this.start = start;
	}

}
