package org.red5.server.net.rtmp.codec;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.DebugPooledByteBufferAllocator;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.ChunkSize;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.PacketHeader;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.SharedObject;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.VideoData;
import org.red5.server.service.Call;

public class RTMPProtocolEncoder implements SimpleProtocolEncoder, Constants {

	protected static Log log =
        LogFactory.getLog(RTMPProtocolEncoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(RTMPProtocolEncoder.class.getName()+".out");
	
	private Serializer serializer = null;
	private Serializer soSerializer = new SharedObjectSerializer();
	
	public ByteBuffer encode(ProtocolState state, Object message) throws Exception {
			
		//DebugPooledByteBufferAllocator.setCodeSection("encode");
		
		try {

			
			
			final RTMP rtmp = (RTMP) state;
		
			if(message instanceof ByteBuffer){
				if(log.isDebugEnabled())
					log.debug("Sending raw buffer");
				return (ByteBuffer) message; 
			}
	
			final OutPacket packet = (OutPacket) message;
			final PacketHeader header = packet.getDestination();
			final byte channelId = header.getChannelId();
			
			final ByteBuffer data = encodeMessage(packet.getMessage());
			header.setSize(data.limit());
			ByteBuffer headers = encodeHeader(header,rtmp.getLastWriteHeader(channelId));
			rtmp.setLastWriteHeader(channelId, header);
			rtmp.setLastWritePacket(channelId, packet);

			final int chunkSize =  rtmp.getWriteChunkSize();
			int numChunks =  (int) Math.ceil(header.getSize() / (float) chunkSize);
			
			ByteBuffer buf = null;
			final int bufSize = header.getSize() + 12 + (numChunks - 1 * 1);
			buf = ByteBuffer.allocate(bufSize); // FIX ME
			buf.setAutoExpand(false);

			headers.flip();	
			buf.put(headers);
			headers.release();
	
			
			
			// TODO: threadsafe way of doing this reusing the data here, im thinking a lock
			for(int i=0; i<numChunks; i++){
				int readAmount = (data.remaining()>chunkSize) 
					? chunkSize : data.remaining();
				if(log.isDebugEnabled())
					log.debug("putting chunk: "+readAmount);
				BufferUtils.put(buf,data,readAmount);
				if(data.remaining()>0){
					buf.put(RTMPUtils.encodeHeaderByte(HEADER_CONTINUE, header.getChannelId()));
				}
			}
			
			buf.flip();
			
			if(ioLog.isDebugEnabled()){
				ioLog.debug(packet.getDestination());
				ioLog.debug(packet.getMessage());
				ioLog.debug(buf.getHexDump());
			}
			
			// Once we have finished with the data buffer flip it for next use
			data.flip();
			// this will destroy the packet if there are no more refs
			
			packet.getMessage().release();
			
			//DebugPooledByteBufferAllocator.setCodeSection(null);
			return buf;
			
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//DebugPooledByteBufferAllocator.setCodeSection(null);
		return null;
	}

		
	public ByteBuffer encodeHeader(PacketHeader header, PacketHeader lastHeader){
		
		int headerSize = 12;
		
		ByteBuffer buf = ByteBuffer.allocate(headerSize);
		
		byte headerByte = RTMPUtils.encodeHeaderByte(HEADER_NEW, header.getChannelId());
		
		buf.put(headerByte);
	
		// write timer
		RTMPUtils.writeMediumInt(buf, header.getTimer());
		
		// write size
		RTMPUtils.writeMediumInt(buf, header.getSize());
		
		// write datatype
		buf.put(header.getDataType());
		
		// write stream
		RTMPUtils.writeReverseInt(buf, header.getStreamId());
		
		return buf;
	}
	
	public ByteBuffer encodeMessage(Message message){
		if(message.isSealed()){
			return message.getData();
		}
		switch(message.getDataType()){
		case TYPE_CHUNK_SIZE:
			encodeChunkSize((ChunkSize) message);
			break;
		case TYPE_INVOKE:
			encodeInvoke((Invoke) message);
			break;
		case TYPE_NOTIFY:
			if (((Notify) message).getCall() == null)
				// Stream metadata
				encodeStreamMetadata((Notify) message);
			else
				encodeInvoke((Notify) message);
			break;
		case TYPE_PING:
			encodePing((Ping) message);
			break;
		case TYPE_STREAM_BYTES_READ:
			encodeStreamBytesRead((StreamBytesRead) message);
			break;
		case TYPE_AUDIO_DATA:
			encodeAudioData((AudioData) message);
			break;
		case TYPE_VIDEO_DATA:
			encodeVideoData((VideoData) message);
			break;
		case TYPE_SHARED_OBJECT:
			encodeSharedObject((SharedObject) message);
			break;
		}
		if(message.getData() == null){
			message.setData(ByteBuffer.allocate(0));
		}
		message.getData().flip();
		message.setSealed(true);
		return message.getData();
	}

	private void encodeChunkSize(ChunkSize chunkSize) {
		if(chunkSize.getData() == null) chunkSize.setData(ByteBuffer.allocate(4));
		final ByteBuffer data = chunkSize.getData();
		data.putInt(chunkSize.getSize());
	}

	private void encodeSharedObject(SharedObject so) {
	
		final ByteBuffer data = ByteBuffer.allocate(256).setAutoExpand(true);
		java.nio.ByteBuffer strBuf;
		int len;
		
		Output.putString(data, so.getName());
		// SO version
		data.putInt(so.getSoId());
		// Encoding (this always seems to be 2 for persistent shared objects)
		data.putInt(so.isPersistent() ? 2 : 0);
		// unknown field
		data.putInt(0);
		
		Iterator it = so.getEvents().iterator();
		while (it.hasNext()) {
			
			SharedObjectEvent event = (SharedObjectEvent) it.next();
			//log.info("encode: " + event);
			switch (event.getType()) {
			case SO_CLIENT_INITIAL_DATA:
				data.put(event.getType());
				data.putInt(0);
				break;
			
			case SO_CLIENT_UPDATE_ATTRIBUTE:
				// Confirm attribute change requested by client
				strBuf = AMF.CHARSET.encode(event.getKey());
				len = strBuf.limit();
				
				data.put(event.getType());

				// Size is length informations + attribute name 
				data.putInt(len+2);
				
				data.putShort((short) len);
				data.put(strBuf);
				break;
				
			case SO_CLIENT_UPDATE_DATA:
				if (event.getKey() == null) {
					// Update multiple attributes in one request
					Map initialData = (Map) event.getValue();
					
					// Buffer for all initial attributes
					ByteBuffer completeBuffer = ByteBuffer.allocate(128);
					completeBuffer.setAutoExpand(true);
					
					Iterator keys = initialData.keySet().iterator();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						
						ByteBuffer sub = ByteBuffer.allocate(128);
						sub.setAutoExpand(true);
						
						// Store key without leading AMF type flag
						strBuf = AMF.CHARSET.encode(key);
						len = strBuf.limit();
						sub.putShort((short) len);
						sub.put(strBuf);
						
						// Serialize attribute value using regular AMF methods
						Output output = new Output(sub);
						soSerializer.serialize(output, initialData.get(key));
						
						completeBuffer.put(event.getType());
						completeBuffer.putInt(sub.position());
						sub.flip();
						completeBuffer.put(sub);
						sub.release();
					}
					
					completeBuffer.flip();
					data.put(completeBuffer);
					completeBuffer.release();
				} else {
					// Update one attribute
					ByteBuffer sub = ByteBuffer.allocate(128);
					sub.setAutoExpand(true);
					
					// Store key without leading AMF type flag
					strBuf = AMF.CHARSET.encode(event.getKey());
					len = strBuf.limit();
					sub.putShort((short) len);
					sub.put(strBuf);
					
					// Serialize attribute value using regular AMF methods
					Output output = new Output(sub);
					soSerializer.serialize(output, event.getValue());
					
					data.put(event.getType());
					data.putInt(sub.position());
					sub.flip();
					data.put(sub);
					sub.release();
				}
				break;
			
			case SO_CLIENT_DELETE_DATA:
				// Store key without leading AMF type flag
				strBuf = AMF.CHARSET.encode(event.getKey());
				len = strBuf.limit();
				data.put(event.getType());
				
				// Size is length informations + attribute name 
				data.putInt(len+2);
				
				data.putShort((short) len);
				data.put(strBuf);
				break;
				
			case SO_CLIENT_SEND_MESSAGE:
				// Send method name and value
				ByteBuffer sub = ByteBuffer.allocate(128);
				sub.setAutoExpand(true);
				
				// Serialize name of the handler to call...
				Output output = new Output(sub);
				soSerializer.serialize(output, event.getKey());
				// ...and the arguments
				List value = (List) event.getValue();
				it = value.iterator();
				while (it.hasNext()) {
					serializer.serialize(output, it.next());
				}
				
				data.put(event.getType());
				data.putInt(sub.position());
				sub.flip();
				data.put(sub);
				sub.release();
				break;
				
			default:
				log.error("Unknown event " + event.getType());
			}
		}
		so.setData(data);
	}

	public void encodeInvoke(Notify invoke){
		// TODO: tidy up here
		// log.debug("Encode invoke");
		if(invoke.getData()==null) invoke.setData(ByteBuffer.allocate(256).setAutoExpand(true));
		Output output = new Output(invoke.getData());
		
		final IServiceCall call = invoke.getCall();
		final boolean isPending = (call.getStatus()==Call.STATUS_PENDING);
		
		if(!isPending){
			if(log.isDebugEnabled())
				log.debug("Call has been executed, send result");
			serializer.serialize(output, "_result"); // seems right
		} else {
			if(log.isDebugEnabled())
				log.debug("This is a pending call, send request");
			serializer.serialize(output, invoke.getCall().getServiceMethodName()); // seems right
		}
		
		// dont know what this number does, so im just sending it back
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
		//invoke.getData().flip();
	}
	
	public void encodeNotify(Notify notify){

	}
	
	public void encodePing(Ping ping){
		if(ping.getData()==null) ping.setData(ByteBuffer.allocate(16));
		final ByteBuffer out = ping.getData();
		out.putShort(ping.getValue1());
		out.putInt(ping.getValue2());
		if(ping.getValue3()!=Ping.UNDEFINED)
			out.putInt(ping.getValue3());
		if(log.isDebugEnabled())
			log.debug(ping);
	}
	
	public void encodeStreamBytesRead(StreamBytesRead streamBytesRead){
		if(streamBytesRead.getData()==null) streamBytesRead.setData(ByteBuffer.allocate(4));
		final ByteBuffer out = streamBytesRead.getData();
		out.putInt(streamBytesRead.getBytesRead());
	}
	
	public void encodeStreamMetadata(Notify metaData){
		// Just seek to end of stream, we pass the published data to the clients
		if(metaData.getData()==null) metaData.setData(ByteBuffer.allocate(256).setAutoExpand(true));
		final ByteBuffer out = metaData.getData(); 
		out.position(out.limit());
	}

	public void encodeAudioData(AudioData audioData){
		audioData.release();
	}
	
	public void encodeVideoData(VideoData videoData){
		videoData.release();
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

}