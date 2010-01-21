package org.red5.compatibility.flex.messaging.messages;

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

import java.io.Serializable;
import java.util.Map;

import org.red5.io.amf3.ByteArray;
import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.utils.ObjectMap;
import org.red5.io.utils.RandomGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all Flex compatibility messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
@SuppressWarnings("unchecked")
public class AbstractMessage implements Message, Serializable {

	private static final long serialVersionUID = -834697863344344313L;

	public long timestamp;

	public Map<String, Object> headers = new ObjectMap<String, Object>();

	public Object body;

	public String messageId;

	protected byte[] messageIdBytes;

	public long timeToLive;

	public String clientId;

	protected byte[] clientIdBytes;

	public String destination;

	/**
	 * Initialize default message fields.
	 */
	public AbstractMessage() {
		timestamp = System.currentTimeMillis();
		messageId = new RandomGUID().toString();
	}

	/**
	 * Add message properties to string.
	 * 
	 * @param result <code>StringBuilder</code> to add properties to
	 */
	protected void addParameters(StringBuilder result) {
		result.append("ts=");
		result.append(timestamp);
		result.append(",headers=");
		result.append(headers);
		result.append(",body=");
		result.append(body);
		result.append(",messageId=");
		result.append(messageId);
		result.append(",timeToLive=");
		result.append(timeToLive);
		result.append(",clientId=");
		result.append(clientId);
		result.append(",destination=");
		result.append(destination);
	}

	@Override
	public Object getBody() {
		return body;
	}

	@Override
	public String getClientId() {
		return clientId;
	}

	@Override
	public String getDestination() {
		return destination;
	}

	@Override
	public Object getHeader(String name) {
		return headers.get(name);
	}

	@Override
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public long getTimeToLive() {
		return timeToLive;
	}

	@Override
	public boolean headerExists(String name) {
		return headers.containsKey(name);
	}

	@Override
	public void setBody(Object value) {
		body = value;
	}

	@Override
	public void setClientId(String value) {
		clientId = value;
	}

	@Override
	public void setDestination(String value) {
		destination = value;
	}

	@Override
	public void setHeader(String name, Object value) {
		headers.put(name, value);
	}

	@Override
	public void setHeaders(Map<String, Object> value) {
		if (!headers.isEmpty()) {
			headers.clear();
		}
		headers.putAll(value);
	}

	@Override
	public void setMessageId(String value) {
		messageId = value;
	}

	@Override
	public void setTimestamp(long value) {
		timestamp = value;
	}

	@Override
	public void setTimeToLive(long value) {
		timeToLive = value;
	}

	/**
	 * Return string representation of the message.
	 * 
	 * @return value
	 */
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getName());
		result.append("(");
		addParameters(result);
		result.append(")");
		return result.toString();
	}

	static Logger log = LoggerFactory.getLogger(AbstractMessage.class);

	protected short[] readFlags(IDataInput input) {
		boolean hasNextFlag = true;
		short[] flagsArray = new short[2];
		int i = 0;
		while (hasNextFlag) {
			short flags = (short) input.readUnsignedByte();
			log.debug("Unsigned byte: {}", flags);
			if (i == flagsArray.length) {
				short[] tempArray = new short[i * 2];
				System.arraycopy(flagsArray, 0, tempArray, 0, flagsArray.length);
				flagsArray = tempArray;
			}
			flagsArray[i] = flags;
			if ((flags & 0x80) != 0) {
				hasNextFlag = true;
			} else {
				hasNextFlag = false;
			}
			++i;
		}
		log.debug("Flag count: {}", flagsArray.length);
		return flagsArray;
	}

	@SuppressWarnings("rawtypes")
	public void readExternal(IDataInput input) {
		log.debug("AbstractMessage - Read external");
		short[] flagsArray = readFlags(input);
		for (int i = 0; i < flagsArray.length; ++i) {
			short flags = flagsArray[i];
			short reservedPosition = 0;
			if (i == 0) {
				if ((flags & 0x1) != 0) {
					Object obj = input.readObject();
					log.debug("Body object: {} name: {}", obj, obj.getClass().getName());

					body = obj;
				}
				if ((flags & 0x2) != 0) {
					Object obj = input.readUTF();
					log.debug("Client id object: {} name: {}", obj, obj.getClass().getName());

					clientId = ((String) obj);
				}
				if ((flags & 0x4) != 0) {
					Object obj = input.readUTF();
					log.debug("Destination object: {} name: {}", obj, obj.getClass().getName());

					destination = ((String) obj);
				}
				if ((flags & 0x8) != 0) {
					Object obj = input.readObject();
					log.debug("Headers object: {} name: {}", obj, obj.getClass().getName());

					headers = ((ObjectMap) obj);
				}
				if ((flags & 0x10) != 0) {
					Object obj = input.readUTF();
					log.debug("Message id object: {} name: {}", obj, obj.getClass().getName());

					messageId = ((String) obj);
				}
				if ((flags & 0x20) != 0) {
					Object obj = input.readObject();
					log.debug("Timestamp object: {} name: {}", obj, obj.getClass().getName());

					timestamp = ((Number) obj).longValue();
				}
				if ((flags & 0x40) != 0) {
					Object obj = input.readObject();
					log.debug("TTL object: {} name: {}", obj, obj.getClass().getName());
					timeToLive = ((Number) obj).longValue();
				}
				reservedPosition = 7;
			} else if (i == 1) {
				if ((flags & 0x1) != 0) {
					Object obj = input.readObject();
					log.debug("Client id (bytes) object: {} name: {}", obj, obj.getClass().getName());
					if (obj instanceof ByteArray) {
						ByteArray ba = (ByteArray) obj;
						clientIdBytes = new byte[ba.length()];
						ba.readBytes(clientIdBytes);
						clientId = RandomGUID.fromByteArray(clientIdBytes);
					}
				}
				if ((flags & 0x2) != 0) {
					Object obj = input.readObject();
					log.debug("Message id (bytes) object: {} name: {}", obj, obj.getClass().getName());
					if (obj instanceof ByteArray) {
						ByteArray ba = (ByteArray) obj;
						messageIdBytes = new byte[ba.length()];
						ba.readBytes(messageIdBytes);
						messageId = RandomGUID.fromByteArray(messageIdBytes);
					}
				}
				reservedPosition = 2;
			}
			if (flags >> reservedPosition == 0) {
				continue;
			}
			for (short j = reservedPosition; j < 6; j = (short) (j + 1)) {
				if ((flags >> j & 0x1) == 0) {
					continue;
				}
				input.readObject();
			}
		}
	}

	public void writeExternal(IDataOutput output) {
		short flags = 0;

		if (clientIdBytes == null) {
			if (clientId == null) {
				clientIdBytes = null;
			} else {
				clientIdBytes = RandomGUID.toByteArray(clientId);
			}
		}
		if (messageIdBytes == null) {
			if (messageId == null) {
				messageIdBytes = null;
			} else {
				messageIdBytes = RandomGUID.toByteArray(messageId);
			}
		}
		if (body != null) {
			flags = (short) (flags | 0x1);
		}
		if ((clientId != null) && (clientIdBytes == null)) {
			flags = (short) (flags | 0x2);
		}
		if (destination != null) {
			flags = (short) (flags | 0x4);
		}
		if (headers != null) {
			flags = (short) (flags | 0x8);
		}
		if ((messageId != null) && (messageIdBytes == null)) {
			flags = (short) (flags | 0x10);
		}
		if (timestamp != 0L) {
			flags = (short) (flags | 0x20);
		}
		if (timeToLive != 0L) {
			flags = (short) (flags | 0x40);
		}
		if ((clientIdBytes != null) || (messageIdBytes != null)) {
			flags = (short) (flags | 0x80);
		}
		output.writeByte((byte) flags);

		flags = 0;

		if (clientIdBytes != null) {
			flags = (short) (flags | 0x1);
		}
		if (messageIdBytes != null) {
			flags = (short) (flags | 0x2);
		}
		if (flags != 0) {
			output.writeByte((byte) flags);
		}
		if (body != null) {
			output.writeObject(body);
		}
		if ((clientId != null) && (clientIdBytes == null)) {
			output.writeUTF(clientId);
		}
		if (destination != null) {
			output.writeUTF(destination);
		}
		if (headers != null) {
			output.writeObject(headers);
		}
		if ((messageId != null) && (messageIdBytes == null)) {
			output.writeUTF(this.messageId);
		}
		if (timestamp != 0L) {
			output.writeObject(Long.valueOf(timestamp));
		}
		if (this.timeToLive != 0L) {
			output.writeObject(Long.valueOf(timeToLive));
		}
		if (this.clientIdBytes != null) {
			output.writeObject(clientIdBytes);
		}
		if (this.messageIdBytes != null) {
			output.writeObject(messageIdBytes);
		}
	}

}
