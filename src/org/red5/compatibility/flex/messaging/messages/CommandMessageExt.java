package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * An externalizable version of a given CommandMessage. The class alias for this
 * class within flex is "DSC".
 * 
 * @author Paul Gregoire
 */
public class CommandMessageExt extends CommandMessage implements IExternalizable {

	private static final long serialVersionUID = -5371460213241777011L;
	
	private CommandMessage message;

	public CommandMessageExt() {
	}

	public CommandMessageExt(CommandMessage message) {
		this.message = message;
	}
	
	@Override
	public void writeExternal(IDataOutput out) {
		//if (this.message != null)
		//this.message.writeExternal(output);
		//else
		//super.writeExternal(output);		
		if (message != null) {
			short flags = 0;
			if (this.operation != 0) {
				flags = (short) (flags | OPERATION_FLAG);
			}
			out.writeByte((byte) flags);
			if (this.operation != 0) {
				out.writeInt(operation);
			}
		}
	}

}
