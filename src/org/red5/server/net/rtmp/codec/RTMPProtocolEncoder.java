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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.StreamBytesRead;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.SharedObjectTypeMapping;
import org.red5.server.service.Call;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.ISharedObjectMessage;

public class RTMPProtocolEncoder implements SimpleProtocolEncoder, Constants, IEventEncoder {

	protected static Log log =
        LogFactory.getLog(RTMPProtocolEncoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(RTMPProtocolEncoder.class.getName()+".out");
	
	private Serializer serializer = null;
	
	public ByteBuffer encode(ProtocolState state, Object message) throws Exception {
		try {
			final RTMP rtmp = (RTMP) state;
			if(message instanceof ByteBuffer) return (ByteBuffer) message; 
		    else return encodePacket(rtmp, (Packet) message);
		} catch (RuntimeException e) {
			log.error("Error encoding object: ",e);
		}
		return null;
	}

	public ByteBuffer encodePacket(RTMP rtmp, Packet packet){

		final Header header = packet.getHeader();
		final byte channelId = header.getChannelId();
		final IRTMPEvent message = packet.getMessage();
		ByteBuffer data;
		
		try {
			data = encodeMessage(header, message);
		} finally {
			message.release();
		}
		
		if (data.position() != 0)
			data.flip();
		else
			data.rewind();
		header.setSize(data.limit());
		
		final ByteBuffer headers = encodeHeader(header,rtmp.getLastWriteHeader(channelId));
		
		rtmp.setLastWriteHeader(channelId, header);
		rtmp.setLastWritePacket(channelId, packet);
		
		final int chunkSize = rtmp.getWriteChunkSize();
		final int numChunks = (int) Math.ceil(header.getSize() / (float) chunkSize);
		final int bufSize = header.getSize() + headers.limit() + (numChunks - 1 * 1);
		final ByteBuffer out = ByteBuffer.allocate(bufSize);
		
		headers.flip();	
		out.put(headers);
		headers.release();
		
		if(numChunks == 1){
			// we can do it with a single copy
			BufferUtils.put(out,data,out.remaining());
		} else {
			for(int i=0; i<numChunks-1; i++){
				BufferUtils.put(out,data,chunkSize);
				out.put(RTMPUtils.encodeHeaderByte(HEADER_CONTINUE, header.getChannelId()));
			}
			BufferUtils.put(out,data,out.remaining());
		}
		
		data.release();
		out.flip();

		return out;
	}
	
	public ByteBuffer encodeHeader(Header header, Header lastHeader){
		
		byte headerType = HEADER_NEW;
		if(lastHeader==null || header.getStreamId() != lastHeader.getStreamId() || !header.isTimerRelative()){
			headerType = HEADER_NEW;
		} else if(header.getSize() != lastHeader.getSize() || header.getDataType() != lastHeader.getDataType()){
			headerType = HEADER_SAME_SOURCE;
		} else if(header.getTimer() != lastHeader.getTimer()){
			headerType = HEADER_TIMER_CHANGE;
		} else
			headerType = HEADER_CONTINUE;
		
		final ByteBuffer buf = ByteBuffer.allocate(RTMPUtils.getHeaderLength(headerType));
		final byte headerByte = RTMPUtils.encodeHeaderByte(headerType, header.getChannelId());
		
		buf.put(headerByte);
		
		switch(headerType){
		
		case HEADER_NEW:
			log.info("absolute header timer: " + header.getTimer());
			RTMPUtils.writeMediumInt(buf, header.getTimer());
			RTMPUtils.writeMediumInt(buf, header.getSize());
			buf.put(header.getDataType());
			RTMPUtils.writeReverseInt(buf, header.getStreamId());
			break;
			
		case HEADER_SAME_SOURCE:
			RTMPUtils.writeMediumInt(buf, header.getTimer());
			RTMPUtils.writeMediumInt(buf, header.getSize());
			buf.put(header.getDataType());
			break;
			
		case HEADER_TIMER_CHANGE:
			RTMPUtils.writeMediumInt(buf, header.getTimer());
			break;
			
		case HEADER_CONTINUE:
			break;
		
		}
		return buf;
	}
	
	public ByteBuffer encodeMessage(Header header, IRTMPEvent message){
		switch(header.getDataType()){
		case TYPE_CHUNK_SIZE:
			return encodeChunkSize((ChunkSize) message);
		case TYPE_INVOKE:
			return encodeInvoke((Invoke) message);
		case TYPE_NOTIFY:
			if (((Notify) message).getCall() == null)
				return encodeStreamMetadata((Notify) message);
			else 
				return encodeNotify((Notify) message);
		case TYPE_PING:
			return encodePing((Ping) message);
		case TYPE_STREAM_BYTES_READ:
			return encodeStreamBytesRead((StreamBytesRead) message);
		case TYPE_AUDIO_DATA:
			return encodeAudioData((AudioData) message);
		case TYPE_VIDEO_DATA:
			return encodeVideoData((VideoData) message);
		case TYPE_SHARED_OBJECT:
			return encodeSharedObject((ISharedObjectMessage) message);
		default: return null;
		}
	}

	public ByteBuffer encodeChunkSize(ChunkSize chunkSize) {
		final ByteBuffer out = ByteBuffer.allocate(4);
		out.putInt(chunkSize.getSize());
		return out;
	}

	public ByteBuffer encodeSharedObject(ISharedObjectMessage so) {
		
		final ByteBuffer out = ByteBuffer.allocate(128);
		out.setAutoExpand(true);
		
		Output.putString(out, so.getName());
		// SO version
		out.putInt(so.getVersion());
		// Encoding (this always seems to be 2 for persistent shared objects)
		out.putInt(so.isPersistent() ? 2 : 0);
		// unknown field
		out.putInt(0);
		
		int mark, len = 0;
		
		final Iterator iter = so.getEvents().iterator();
	    while(iter.hasNext()){
			
			ISharedObjectEvent event = (ISharedObjectEvent) iter.next();
			byte type = SharedObjectTypeMapping.toByte(event.getType());

			switch (event.getType()) {
			case CLIENT_INITIAL_DATA:
			case CLIENT_CLEAR_DATA:
				out.put(type);
				out.putInt(0);
				break;
			
			case CLIENT_DELETE_DATA:
			case CLIENT_UPDATE_ATTRIBUTE:
				out.put(type);
				mark = out.position();
				out.skip(4); // we will be back
				Output.putString(out,event.getKey());
				len = out.position() - mark - 4;
				out.putInt(mark,len);
				break;
				
			case CLIENT_UPDATE_DATA:
				if (event.getKey() == null) {
					// Update multiple attributes in one request
					Map initialData = (Map) event.getValue();
					
					Iterator keys = initialData.keySet().iterator();
					while (keys.hasNext()) {
						
						out.put(type);
						mark = out.position();
						out.skip(4); // we will be back
						
						String key = (String) keys.next();		
						Output.putString(out,key);
						final Output output = new Output(out);
						serializer.serialize(output, initialData.get(key));
						
						len = out.position() - mark - 4;
						out.putInt(mark,len);
					}
				} else {
					out.put(type);
					mark = out.position();
					out.skip(4); // we will be back
					
					Output.putString(out,event.getKey());
					final Output output = new Output(out);
					serializer.serialize(output,event.getValue());

					len = out.position() - mark - 4;
					out.putInt(mark,len);
				}
				break;
							
			case CLIENT_SEND_MESSAGE:
			case SERVER_SEND_MESSAGE:
				// Send method name and value
				out.put(type);
				mark = out.position();
				out.skip(4);
				// Serialize name of the handler to call...
				final Output output = new Output(out);
				serializer.serialize(output, event.getKey());
				// ...and the arguments
				for(Object arg : (List) event.getValue()){
					serializer.serialize(output, arg);
				}
				len = out.position() - mark - 4;
				//log.debug(len);
				out.putInt(mark,len);			
				//log.info(out.getHexDump());
				break;
				
			default:
				//log.error("Unknown event " + event.getType());
	            // XXX: come back here, need to make this work in server or client mode
			    // talk to joachim about this part.
				out.put(type);
				mark = out.position();
				//out.putInt(0);
				out.skip(4); // we will be back
				Output.putString(out,event.getKey());
				final Output output2 = new Output(out);
				serializer.serialize(output2, event.getValue());
				len = out.position() - mark - 4;
				out.putInt(mark,len);
				break;
				
			
			}
		}
		return out;
	}

	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodeNotify(org.red5.server.net.rtmp.event.Notify)
	 */
	public ByteBuffer encodeNotify(Notify notify){
		return encodeNotifyOrInvoke(notify);
	}
	
	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodeInvoke(org.red5.server.net.rtmp.event.Invoke)
	 */
	public ByteBuffer encodeInvoke(Invoke invoke){
		return encodeNotifyOrInvoke(invoke);
	}
	
	protected ByteBuffer encodeNotifyOrInvoke(Notify invoke){
		// TODO: tidy up here
		// log.debug("Encode invoke");
		
		ByteBuffer out = ByteBuffer.allocate(1024);
		out.setAutoExpand(true);
		Output output = new Output(out);
		
		final IServiceCall call = invoke.getCall();
		final boolean isPending = (call.getStatus()==Call.STATUS_PENDING);
		
		if(!isPending){
			if(log.isDebugEnabled())
				log.debug("Call has been executed, send result");
			serializer.serialize(output, "_result"); // seems right
		} else {
			if(log.isDebugEnabled())
				log.debug("This is a pending call, send request");
			final String action = (call.getServiceName()==null) ?
					call.getServiceMethodName() : call.getServiceName() + "." + call.getServiceMethodName();
			serializer.serialize(output, action); // seems right
		}
		serializer.serialize(output, new Integer(invoke.getInvokeId())); 
		serializer.serialize(output, null);
		if (!isPending && (invoke instanceof Invoke)){
			IPendingServiceCall pendingCall = (IPendingServiceCall) call;
			if(log.isDebugEnabled())
				log.debug("Writing result: "+pendingCall.getResult());
			serializer.serialize(output, pendingCall.getResult());
		} else {
			if(log.isDebugEnabled())
				log.debug("Writing params");
			final Object[] args = invoke.getCall().getArguments();
			if(args!=null){
				for (int i = 0; i < args.length; i++) {
					serializer.serialize(output, args[i]);
				}
			}
		}		
		return out;
	}
	
	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodePing(org.red5.server.net.rtmp.event.Ping)
	 */
	public ByteBuffer encodePing(Ping ping){
		final int len =  (ping.getValue3()==Ping.UNDEFINED) ? 6 : 8;
		final ByteBuffer out = ByteBuffer.allocate(len);
		out.putShort(ping.getValue1());
		out.putInt(ping.getValue2());
		if(ping.getValue3()!=Ping.UNDEFINED)
			out.putInt(ping.getValue3());
		return out;
	}
	
	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodeStreamBytesRead(org.red5.server.net.rtmp.event.StreamBytesRead)
	 */
	public ByteBuffer encodeStreamBytesRead(StreamBytesRead streamBytesRead){
		final ByteBuffer out = ByteBuffer.allocate(4);
		out.putInt(streamBytesRead.getBytesRead());
		return out;
	}

	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodeAudioData(org.red5.server.net.rtmp.event.AudioData)
	 */
	public ByteBuffer encodeAudioData(AudioData audioData){
		return audioData.getData().asReadOnlyBuffer();
	}
	
	/* (non-Javadoc)
	 * @see org.red5.server.net.rtmp.codec.IEventEncoder#encodeVideoData(org.red5.server.net.rtmp.event.VideoData)
	 */
	public ByteBuffer encodeVideoData(VideoData videoData){
		return videoData.getData().asReadOnlyBuffer();
	}
	
	public ByteBuffer encodeUnknown(Unknown unknown){
		return unknown.getData().asReadOnlyBuffer();
	}
	
	public ByteBuffer encodeStreamMetadata(Notify metaData){
		return metaData.getData().asReadOnlyBuffer();
	}

	public void setSerializer(org.red5.io.object.Serializer serializer) {
		this.serializer = serializer;
	}

	
}
