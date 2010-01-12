package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.ByteArray;
import org.red5.io.amf3.IDataInput;
import org.red5.io.utils.RandomGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/**
 * Command message as sent by the <code>mx:RemoteObject</code> tag.
 * 
 * @see <a href="http://osflash.org/documentation/amf3">osflash documentation (external)</a>
 * @see <a href="http://livedocs.adobe.com/flex/2/langref/mx/rpc/remoting/mxml/RemoteObject.html">Adobe Livedocs (external)</a>
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public class CommandMessage extends AsyncMessage {

	private static final long serialVersionUID = 8805045741686625945L;

	protected static byte OPERATION_FLAG = 1;
	
	public String messageRefType;

	/** Command id to execute. */
	public int operation = Constants.UNKNOWN_OPERATION;

	/** {@inheritDoc} */
	protected void addParameters(StringBuilder result) {
		super.addParameters(result);
		result.append(",messageRefType=");
		result.append(messageRefType);
		result.append(",operation=");
		result.append(operation);
	}

	public int getOperation() {
		return operation;
	}

	public void setOperation(int operation) {
		this.operation = operation;
	}

	static Logger log = LoggerFactory.getLogger(CommandMessage.class);	
	
	@Override
	public void readExternal(IDataInput in) {
		super.readExternal(in);
		short[] flagsArray = readFlags(in);
		for (int i = 0; i < flagsArray.length; ++i) {
			short flags = flagsArray[i];
			log.debug("Unsigned byte: {}", flags);
			short reservedPosition = 0;
			if (i == 0) {
				if ((flags & OPERATION_FLAG) != 0) {
					Object obj = in.readObject();
					log.debug("Operation object: {} name: {}", obj, obj.getClass().getName());
					this.operation = ((Number) obj).intValue();
				}
				reservedPosition = 1;
			}
			if (flags >> reservedPosition == 0) {
				continue;
			}
			for (short j = reservedPosition; j < 6; j = (short) (j + 1)) {
				if ((flags >> j & 0x1) == 0) {
					continue;
				}
				Object obj = in.readObject();
				log.debug("Object2: {} name: {}", obj, obj.getClass().getName());
				if (obj instanceof ByteArray) {
					ByteArray ba = (ByteArray) obj;
					byte[] arr = new byte[ba.length()];
					ba.readBytes(arr);
					log.debug("Array length: {} Data: {}", arr.length, RandomGUID.fromByteArray(arr));
				}
			}
		}
		log.debug("Operation: {}", operation);
	}

}
