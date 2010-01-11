package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * An externalizable version of a given AcknowledgeMessage. The class alias for this
 * class within flex is "DSK".
 * 
 * @author Paul Gregoire
 */
public class AcknowledgeMessageExt extends AcknowledgeMessage implements IExternalizable {

	private static final long serialVersionUID = -8764729006642310394L;

	private AcknowledgeMessage message;

	public AcknowledgeMessageExt() {
	}

	public AcknowledgeMessageExt(AcknowledgeMessage message) {
		this.setMessage(message);
	}

	public void setMessage(AcknowledgeMessage message) {
		this.message = message;
	}

	public AcknowledgeMessage getMessage() {
		return message;
	}	

	@Override
	public void writeExternal(IDataOutput output) {
		//if (this.message != null)
		//this.message.writeExternal(output);
		//else
		//super.writeExternal(output);
		output.writeByte((byte) 0);
	}

}
