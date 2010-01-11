package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;
import org.red5.io.utils.RandomGUID;

/**
 * An externalizable version of a given AsyncMessage. The class alias for this
 * class within flex is "DSA".
 * 
 * @author Paul Gregoire
 */
public class AsyncMessageExt extends AsyncMessage implements IExternalizable {

	private static final long serialVersionUID = -5371460213241777011L;

	private AsyncMessage message;

	public AsyncMessageExt() {
	}

	public AsyncMessageExt(AsyncMessage message) {
		this.setMessage(message);
	}

	public void setMessage(AsyncMessage message) {
		this.message = message;
	}

	public AsyncMessage getMessage() {
		return message;
	}

	@Override
	public void writeExternal(IDataOutput output) {
		//if (this.message != null)
		//this.message.writeExternal(output);
		//else
		//super.writeExternal(output);
		if (this.correlationIdBytes == null) {
			this.correlationIdBytes = RandomGUID.toByteArray(this.correlationId);
		}
		short flags = 0;
		if ((this.correlationId != null) && (this.correlationIdBytes == null)) {
			flags = (short) (flags | CORRELATION_ID_FLAG);
		}
		if (this.correlationIdBytes != null) {
			flags = (short) (flags | CORRELATION_ID_BYTES_FLAG);
		}
		output.writeByte((byte) flags);
		if ((this.correlationId != null) && (this.correlationIdBytes == null)) {
			output.writeObject(this.correlationId);
		}
		if (this.correlationIdBytes != null) {
			output.writeObject(this.correlationIdBytes);
		}
	}
	
	
}
