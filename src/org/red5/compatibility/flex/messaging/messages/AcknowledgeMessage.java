package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
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
 * Flex compatibility message that is returned to the client.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com) 
 */
public class AcknowledgeMessage extends AsyncMessage {

	private static final long serialVersionUID = 228072709981643313L;

	static Logger log = LoggerFactory.getLogger(AcknowledgeMessage.class);

	@Override
	public void readExternal(IDataInput in) {
		super.readExternal(in);
		short[] flagsArray = readFlags(in);
		for (int i = 0; i < flagsArray.length; ++i) {
			short flags = flagsArray[i];
			short reservedPosition = 0;
			if (flags >> reservedPosition == 0) {
				continue;
			}
			for (short j = reservedPosition; j < 6; j = (short) (j + 1)) {
				if ((flags >> j & 0x1) == 0) {
					continue;
				}
				in.readObject();
			}
		}
	}

	@Override
	public void writeExternal(IDataOutput output) {
		super.writeExternal(output);
		output.writeByte((byte) 0);
	}
	
}
