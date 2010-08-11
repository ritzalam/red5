package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * An externalizable version of a given AsyncMessage. The class alias for this
 * class within flex is "DSA".
 * 
 * @author Paul Gregoire (mondain@gmail.com) 
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
		if (this.message != null) {
			this.message.writeExternal(output);
    	} else {
    		super.writeExternal(output);
    	}
	}	
	
}
