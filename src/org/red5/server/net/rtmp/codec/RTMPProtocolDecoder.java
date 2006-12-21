package org.red5.server.net.rtmp.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.utils.BufferUtils;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.net.protocol.ProtocolException;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.FlexMessage;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.SharedObjectTypeMapping;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.ISharedObjectMessage;
import org.red5.server.so.SharedObjectMessage;

public class RTMPProtocolDecoder implements Constants, SimpleProtocolDecoder,
		IEventDecoder {

	protected static Log log = LogFactory.getLog(RTMPProtocolDecoder.class
			.getName());

	protected static Log ioLog = LogFactory.getLog(RTMPProtocolDecoder.class
			.getName()
			+ ".in");

	private Deserializer deserializer;

	public RTMPProtocolDecoder() {

	}

	private Input getInput(ByteBuffer buffer) {
		IConnection conn = Red5.getConnectionLocal();
		if (conn != null && conn.getEncoding() == Encoding.AMF3) {
			return new org.red5.io.amf3.Input(buffer);
		} else {
			return new org.red5.io.amf.Input(buffer);
		}
	}

	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	public List decodeBuffer(ProtocolState state, ByteBuffer buffer) {

		final List<Object> result = new LinkedList<Object>();

		try {
			while (true) {

				final int remaining = buffer.remaining();
				if (state.canStartDecoding(remaining)) {
					state.startDecoding();
				} else {
					break;
				}

				final Object decodedObject = decode(state, buffer);

				if (state.hasDecodedObject()) {
					result.add(decodedObject);
				} else if (state.canContinueDecoding()) {
					continue;
				} else {
					break;
				}

				if (!buffer.hasRemaining()) {
					break;
				}
			}
		} catch (ProtocolException pvx) {
			log.error("Error decoding buffer", pvx);
		} catch (Exception ex) {
			log.error("Error decoding buffer", ex);
		} finally {
			buffer.compact();
		}
		return result;
	}

	public Object decode(ProtocolState state, ByteBuffer in)
			throws ProtocolException {
		int start = in.position();
		try {
			final RTMP rtmp = (RTMP) state;
			switch (rtmp.getState()) {
				case RTMP.STATE_CONNECTED:
					return decodePacket(rtmp, in);
				case RTMP.STATE_ERROR:
					// attempt to correct error 
				case RTMP.STATE_CONNECT:
				case RTMP.STATE_HANDSHAKE:
					return decodeHandshake(rtmp, in);
				default:
					return null;
			}
		} catch (RuntimeException e) {
			log.error("Error in packet at " + start, e);
			throw new ProtocolException("Error during decoding");
		}
	}

	public ByteBuffer decodeHandshake(RTMP rtmp, ByteBuffer in) {

		final int remaining = in.remaining();

		if (rtmp.getMode() == RTMP.MODE_SERVER) {

			if (rtmp.getState() == RTMP.STATE_CONNECT) {

				if (remaining < HANDSHAKE_SIZE + 1) {
					if (log.isDebugEnabled()) {
						log.debug("Handshake init too small, buffering. remaining: " + remaining);
					}
					rtmp.bufferDecoding(HANDSHAKE_SIZE + 1);
					return null;
				} else {
					final ByteBuffer hs = ByteBuffer.allocate(HANDSHAKE_SIZE);
					in.get(); // skip the header byte
					BufferUtils.put(hs, in, HANDSHAKE_SIZE);
					hs.flip();
					rtmp.setState(RTMP.STATE_HANDSHAKE);
					return hs;
				}
			}

			if (rtmp.getState() == RTMP.STATE_HANDSHAKE) {
				log.debug("Handshake reply");
				if (remaining < HANDSHAKE_SIZE) {
					if (log.isDebugEnabled()) {
						log.debug("Handshake reply too small, buffering. remaining: " + remaining);
					}
					rtmp.bufferDecoding(HANDSHAKE_SIZE);
					return null;
				} else {
					in.skip(HANDSHAKE_SIZE);
					rtmp.setState(RTMP.STATE_CONNECTED);
					rtmp.continueDecoding();
					return null;
				}
			}

		} else {
			// else, this is client mode. 
			if (rtmp.getState() == RTMP.STATE_CONNECT) {
				final int size = (2 * HANDSHAKE_SIZE) + 1;
				if (remaining < size) {
					if (log.isDebugEnabled()) {
						log.debug("Handshake init too small, buffering. remaining: " + remaining);
					}
					rtmp.bufferDecoding(size);
					return null;
				} else {
					final ByteBuffer hs = ByteBuffer.allocate(size);
					BufferUtils.put(hs, in, size);
					hs.flip();
					rtmp.setState(RTMP.STATE_CONNECTED);
					return hs;
				}
			}
		}
		return null;
	}

	public Packet decodePacket(RTMP rtmp, ByteBuffer in) {

		final int remaining = in.remaining();

		// We need at least one byte
		if (remaining < 1) {
			rtmp.bufferDecoding(1);
			return null;
		}

		final int position = in.position();
		final byte headerByte = in.get();
		final byte channelId = RTMPUtils.decodeChannelId(headerByte);

		if (channelId < 0) {
			throw new ProtocolException("Bad channel id: " + channelId);
		}

		// Get the header size and length
		final byte headerSize = RTMPUtils.decodeHeaderSize(headerByte);
		int headerLength = RTMPUtils.getHeaderLength(headerSize);

		if (headerLength > remaining) {
			if (log.isDebugEnabled()) {
				log.debug("Header too small, buffering. remaining: " + remaining);
			}
			in.position(position);
			rtmp.bufferDecoding(headerLength);
			return null;
		}

		// Move the position back to the start
		in.position(position);

		final Header header = decodeHeader(in, rtmp
				.getLastReadHeader(channelId));

		if (header == null) {
			throw new ProtocolException("Header is null, check for error");
		}

		// Save the header
		rtmp.setLastReadHeader(channelId, header);

		// Check to see if this is a new packets or continue decoding an
		// existing one.
		Packet packet = rtmp.getLastReadPacket(channelId);

		if (packet == null) {
			packet = new Packet(header);
			rtmp.setLastReadPacket(channelId, packet);
		}

		final ByteBuffer buf = packet.getData();
		final int addSize = (header.getTimer() == 0xffffff ? 4 : 0);
		final int readRemaining = header.getSize() + addSize - buf.position();
		final int chunkSize = rtmp.getReadChunkSize();
		final int readAmount = (readRemaining > chunkSize) ? chunkSize
				: readRemaining;

		if (in.remaining() < readAmount) {
			if (log.isDebugEnabled()) {
				log.debug("Chunk too small, buffering (" + in.remaining() + ','
						+ readAmount);
			}
			// skip the position back to the start
			in.position(position);
			rtmp.bufferDecoding(headerSize + readAmount);
			return null;
		}

		BufferUtils.put(buf, in, readAmount);

		if (buf.position() < header.getSize() + addSize) {
			rtmp.continueDecoding();
			return null;
		}

		buf.flip();

		final IRTMPEvent message = decodeMessage(rtmp, packet.getHeader(), buf);
		packet.setMessage(message);

		if (message instanceof ChunkSize) {
			ChunkSize chunkSizeMsg = (ChunkSize) message;
			rtmp.setReadChunkSize(chunkSizeMsg.getSize());
		}
		rtmp.setLastReadPacket(channelId, null);
		return packet;

	}

	public Header decodeHeader(ByteBuffer in, Header lastHeader) {

		final byte headerByte = in.get();
		final byte channelId = RTMPUtils.decodeChannelId(headerByte);
		final byte headerSize = RTMPUtils.decodeHeaderSize(headerByte);
		Header header = new Header();
		header.setChannelId(channelId);
		header.setTimerRelative(headerSize != HEADER_NEW);

		switch (headerSize) {

			case HEADER_NEW:
				header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
				header.setSize(RTMPUtils.readMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(RTMPUtils.readReverseInt(in));
				break;

			case HEADER_SAME_SOURCE:
				header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
				header.setSize(RTMPUtils.readMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(lastHeader.getStreamId());
				break;

			case HEADER_TIMER_CHANGE:
				header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				break;

			case HEADER_CONTINUE:
				header.setTimer(lastHeader.getTimer());
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				break;

			default:
				log.error("Unexpected header size: " + headerSize);
				return null;

		}
		return header;
	}

	public IRTMPEvent decodeMessage(RTMP rtmp, Header header, ByteBuffer in) {
		IRTMPEvent message = null;
		if (header.getTimer() == 0xffffff) {
			// Skip first four bytes
			int unknown = in.getInt();
			if (log.isDebugEnabled()) {
				log.debug("Unknown 4 bytes: " + unknown);
			}
		}

		switch (header.getDataType()) {
			case TYPE_CHUNK_SIZE:
				message = decodeChunkSize(in);
				break;
			case TYPE_INVOKE:
				message = decodeInvoke(in);
				break;
			case TYPE_NOTIFY:
				message = decodeNotify(in, header, rtmp);
				break;
			case TYPE_PING:
				message = decodePing(in);
				break;
			case TYPE_BYTES_READ:
				message = decodeBytesRead(in);
				break;
			case TYPE_AUDIO_DATA:
				message = decodeAudioData(in);
				break;
			case TYPE_VIDEO_DATA:
				message = decodeVideoData(in);
				break;
			case TYPE_SHARED_OBJECT:
				message = decodeSharedObject(in);
				break;
			case TYPE_SERVER_BANDWIDTH:
				message = decodeServerBW(in);
				break;
			case TYPE_CLIENT_BANDWIDTH:
				message = decodeClientBW(in);
				break;
			case TYPE_FLEX_MESSAGE:
				message = decodeFlexMessage(in);
				break;
			default:
				message = decodeUnknown(header.getDataType(), in);
				break;
		}
		message.setHeader(header);
		message.setTimestamp(header.getTimer());
		return message;
	}

	private IRTMPEvent decodeServerBW(ByteBuffer in) {
		return new ServerBW(in.getInt());
	}

	private IRTMPEvent decodeClientBW(ByteBuffer in) {
		return new ClientBW(in.getInt(), in.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeUnknown(org.apache.mina.common.ByteBuffer)
	 */
	public Unknown decodeUnknown(byte dataType, ByteBuffer in) {
		return new Unknown(dataType, in.asReadOnlyBuffer());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeChunkSize(org.apache.mina.common.ByteBuffer)
	 */
	public ChunkSize decodeChunkSize(ByteBuffer in) {
		return new ChunkSize(in.getInt());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeSharedObject(org.apache.mina.common.ByteBuffer)
	 */
	public ISharedObjectMessage decodeSharedObject(ByteBuffer in) {

		final Input input = getInput(in);
		String name = input.getString();
		// Read version of SO to modify
		int version = in.getInt();
		// Read persistence informations
		boolean persistent = in.getInt() == 2;
		// Skip unknown bytes
		in.skip(4);

		final SharedObjectMessage so = new SharedObjectMessage(null, name,
				version, persistent);

		// Parse request body
		while (in.hasRemaining()) {

			final ISharedObjectEvent.Type type = SharedObjectTypeMapping
					.toType(in.get());
			String key = null;
			Object value = null;

			//if(log.isDebugEnabled()) 
			//	log.debug("type: "+SharedObjectTypeMapping.toString(type));

			//SharedObjectEvent event = new SharedObjectEvent(,null,null);
			final int length = in.getInt();
			if (type == ISharedObjectEvent.Type.CLIENT_STATUS) {
				// Status code
				key = input.getString();
				// Status level
				value = input.getString();
			} else if (type == ISharedObjectEvent.Type.CLIENT_UPDATE_DATA) {
				key = null;
				// Map containing new attribute values
				final Map<String, Object> map = new HashMap<String, Object>();
				final int start = in.position();
				while (in.position() - start < length) {
					String tmp = input.getString();
					map.put(tmp, deserializer.deserialize(input));
				}
				value = map;
			} else if (type != ISharedObjectEvent.Type.SERVER_SEND_MESSAGE
					&& type != ISharedObjectEvent.Type.CLIENT_SEND_MESSAGE) {
				if (length > 0) {
					key = input.getString();
					if (length > key.length() + 2) {
						value = deserializer.deserialize(input);
					}
				}
			} else {
				final int start = in.position();
				// the "send" event seems to encode the handler name
				// as complete AMF string including the string type byte
				key = (String) deserializer.deserialize(input);

				// read parameters
				final List<Object> list = new LinkedList<Object>();
				while (in.position() - start < length) {
					Object tmp = deserializer.deserialize(input);
					list.add(tmp);
				}
				value = list;
			}
			so.addEvent(type, key, value);
		}
		return so;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeNotify(org.apache.mina.common.ByteBuffer)
	 */
	public Notify decodeNotify(ByteBuffer in) {
		return decodeNotify(in, null, null);
	}

	public Notify decodeNotify(ByteBuffer in, Header header, RTMP rtmp) {
		return decodeNotifyOrInvoke(new Notify(), in, header, rtmp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeInvoke(org.apache.mina.common.ByteBuffer)
	 */
	public Invoke decodeInvoke(ByteBuffer in) {
		return (Invoke) decodeNotifyOrInvoke(new Invoke(), in, null, null);
	}

	/**
	 * Checks if the passed action is a reserved stream method.
	 * 
	 * @param action
	 * @return
	 */
	private boolean isStreamCommand(String action) {
		return (ACTION_CREATE_STREAM.equals(action)
				|| ACTION_DELETE_STREAM.equals(action)
				|| ACTION_PUBLISH.equals(action) || ACTION_PLAY.equals(action)
				|| ACTION_SEEK.equals(action) || ACTION_PAUSE.equals(action)
				|| ACTION_CLOSE_STREAM.equals(action)
				|| ACTION_RECEIVE_VIDEO.equals(action) || ACTION_RECEIVE_AUDIO
				.equals(action));
	}

	protected Notify decodeNotifyOrInvoke(Notify notify, ByteBuffer in,
			Header header, RTMP rtmp) {
		// TODO: we should use different code depending on server or client mode
		int start = in.position();
		Input input = getInput(in);

		String action = (String) deserializer.deserialize(input);

		if (!(notify instanceof Invoke) && rtmp != null
				&& rtmp.getMode() == RTMP.MODE_SERVER && header != null
				&& header.getStreamId() != 0 && !isStreamCommand(action)) {
			// Don't decode "NetStream.send" requests
			in.position(start);
			notify.setData(in.asReadOnlyBuffer());
			return notify;
		}

		if (log.isDebugEnabled()) {
			log.debug("Action " + action);
		}

		if (header == null || header.getStreamId() == 0) {
			int invokeId = ((Number) deserializer.deserialize(input))
					.intValue();
			notify.setInvokeId(invokeId);
		}

		Object[] params = new Object[] {};

		if (in.hasRemaining()) {
			ArrayList paramList = new ArrayList();

			final Object obj = deserializer.deserialize(input);

			if (obj instanceof Map) {
				// Before the actual parameters we sometimes (connect) get a map
				// of parameters, this is usually null, but if set should be
				// passed
				// to the connection object. 
				final Map connParams = (Map) obj;
				notify.setConnectionParams(connParams);
			} else if (obj != null) {
				paramList.add(obj);
			}

			while (in.hasRemaining()) {
				paramList.add(deserializer.deserialize(input));
			}
			params = paramList.toArray();
			if (log.isDebugEnabled()) {
				log.debug("Num params: " + paramList.size());
				for (int i = 0; i < params.length; i++) {
					log.debug(" > " + i + ": " + params[i]);
				}
			}
		}

		final int dotIndex = action.lastIndexOf('.');
		String serviceName = (dotIndex == -1) ? null : action.substring(0,
				dotIndex);
		String serviceMethod = (dotIndex == -1) ? action : action.substring(
				dotIndex + 1, action.length());

		if (notify instanceof Invoke) {
			PendingCall call = new PendingCall(serviceName, serviceMethod,
					params);
			((Invoke) notify).setCall(call);
		} else {
			Call call = new Call(serviceName, serviceMethod, params);
			notify.setCall(call);
		}

		return notify;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodePing(org.apache.mina.common.ByteBuffer)
	 */
	public Ping decodePing(ByteBuffer in) {
		final Ping ping = new Ping();
		ping.setDebug(in.getHexDump());
		ping.setValue1(in.getShort());
		ping.setValue2(in.getInt());
		if (in.hasRemaining()) {
			ping.setValue3(in.getInt());
		}
		if (in.hasRemaining()) {
			ping.setValue4(in.getInt());
		}
		return ping;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeStreamBytesRead(org.apache.mina.common.ByteBuffer)
	 */
	public BytesRead decodeBytesRead(ByteBuffer in) {
		return new BytesRead(in.getInt());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeAudioData(org.apache.mina.common.ByteBuffer)
	 */
	public AudioData decodeAudioData(ByteBuffer in) {
		return new AudioData(in.asReadOnlyBuffer());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.server.net.rtmp.codec.IEventDecoder#decodeVideoData(org.apache.mina.common.ByteBuffer)
	 */
	public VideoData decodeVideoData(ByteBuffer in) {
		return new VideoData(in.asReadOnlyBuffer());
	}

	public FlexMessage decodeFlexMessage(ByteBuffer in) {
		// Unknown byte, always 0?
		in.skip(1);
		Input input = new org.red5.io.amf.Input(in);
		String action = (String) deserializer.deserialize(input);
		int invokeId = ((Number) deserializer.deserialize(input)).intValue();
		FlexMessage msg = new FlexMessage();
		msg.setInvokeId(invokeId);
		Object[] params = new Object[] {};

		if (in.hasRemaining()) {
			ArrayList paramList = new ArrayList();

			final Object obj = deserializer.deserialize(input);
			if (obj != null) {
				paramList.add(obj);
			}
			
			if (in.hasRemaining()) {
				// Check for AMF3 encoding of parameters
				byte tmp = in.get();
				in.position(in.position()-1);
				if (tmp == AMF.TYPE_AMF3_OBJECT) {
					// All further parameters are encoded using AMF3
					input = new org.red5.io.amf3.Input(in);
				}
				while (in.hasRemaining()) {
					paramList.add(deserializer.deserialize(input));
				}
			}
			params = paramList.toArray();
			if (log.isDebugEnabled()) {
				log.debug("Num params: " + paramList.size());
				for (int i = 0; i < params.length; i++) {
					log.debug(" > " + i + ": " + params[i]);
				}
			}
		}

		final int dotIndex = action.lastIndexOf('.');
		String serviceName = (dotIndex == -1) ? null : action.substring(0,
				dotIndex);
		String serviceMethod = (dotIndex == -1) ? action : action.substring(
				dotIndex + 1, action.length());

		PendingCall call = new PendingCall(serviceName, serviceMethod, params);
		msg.setCall(call);
		return msg;
	}
	
}
