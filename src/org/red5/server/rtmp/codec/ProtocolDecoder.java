package org.red5.server.rtmp.codec;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.protocol.ProtocolDecoderOutput;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.red5.server.io.BufferUtils;
import org.red5.server.io.Deserializer;
import org.red5.server.io.amf.Input;
import org.red5.server.rtmp.Channel;
import org.red5.server.rtmp.Connection;
import org.red5.server.rtmp.RTMPUtils;
import org.red5.server.rtmp.message.AudioData;
import org.red5.server.rtmp.message.Constants;
import org.red5.server.rtmp.message.Handshake;
import org.red5.server.rtmp.message.InPacket;
import org.red5.server.rtmp.message.Invoke;
import org.red5.server.rtmp.message.Message;
import org.red5.server.rtmp.message.Notify;
import org.red5.server.rtmp.message.PacketHeader;
import org.red5.server.rtmp.message.Ping;
import org.red5.server.rtmp.message.SharedObject;
import org.red5.server.rtmp.message.SharedObjectEvent;
import org.red5.server.rtmp.message.StreamBytesRead;
import org.red5.server.rtmp.message.Unknown;
import org.red5.server.rtmp.message.VideoData;
import org.red5.server.service.Call;

public class ProtocolDecoder implements Constants, org.apache.mina.protocol.ProtocolDecoder {

	protected static Log log =
        LogFactory.getLog(ProtocolDecoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(ProtocolDecoder.class.getName()+".in");
	
	private Deserializer deserializer = null;

	public ProtocolDecoder(){
		
	}
	
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}
	
    public void decode( ProtocolSession session, ByteBuffer in,
            ProtocolDecoderOutput out ) throws ProtocolViolationException {
		
		ByteBuffer buf = (ByteBuffer) session.getAttribute("buffer");
		if(buf==null){
			log.warn("New buffer");
			buf = ByteBuffer.allocate(1024);
			buf.setAutoExpand(true);
			session.setAttribute("buffer",buf);
		}
		buf.put(in);
		buf.flip();
		
		try
		{
		for( ;; )
		{
		    int oldPos = buf.position();
		    boolean decoded = doDecode( session, buf, out );
		    if( decoded )
		    {
		        if( buf.position() == oldPos )
		        {
		            throw new IllegalStateException(
		                    "doDecode() can't return true when buffer is not consumed." );
		        }
		        
		        if( !buf.hasRemaining() )
		        {
		            break;
		        }
		    }
		    else
		    {
		        break;
		    }
		}
		}
		catch(ProtocolViolationException pvx){
			log.error("Error",pvx);
		}
		catch(Exception ex){
			log.error("Error",ex);
		}
		finally {
			// is this needed?
			buf.compact();
		}
	}

	
	protected boolean doDecode(ProtocolSession session, ByteBuffer in,
			 ProtocolDecoderOutput out) throws ProtocolViolationException {
		
		try {
			
			if(in.remaining() < 1) {
				log.debug("Empty read, buffering");
				return false;
			}
			
			final int startPosition = in.position();
			final Connection conn = (Connection) session.getAttachment();
			
			if(conn.getMode()==Connection.MODE_SERVER){
			
				if(conn.getState()==Connection.STATE_CONNECT){
					if(in.remaining() < HANDSHAKE_SIZE + 1){ 
						log.warn("Handshake init too small, buffering");
						log.warn("remaining: "+in.remaining());
						return false;
					}
					in.get();
					conn.setClientTimer(in.getInt());
					log.info("CLIENT UPTIME ?: "+conn.getClientTimer());
					in.position(in.position()-5);
					ByteBuffer hs = ByteBuffer.allocate(HANDSHAKE_SIZE);
					hs.setAutoExpand(true);
					in.get(); // skip the header byte
					int limit = in.limit();
					in.limit(in.position() + HANDSHAKE_SIZE);
					hs.put(in).flip();
					out.write(hs);
					conn.setState(Connection.STATE_HANDSHAKE);
					in.limit(limit);
					return true;
				} 
				
				if(conn.getState()==Connection.STATE_HANDSHAKE){
					log.debug("Handshake reply");
					if(in.remaining() < HANDSHAKE_SIZE){ 
						log.warn("Handshake reply too small, buffering");
						return false;
					}
					in.skip(HANDSHAKE_SIZE);
					conn.setState(Connection.STATE_CONNECTED);
					return true;
				}
				
			} else {
				
				// this is client mode. 
				if(conn.getState()==Connection.STATE_CONNECT){
					if(in.remaining() < (2*HANDSHAKE_SIZE)+1){ 
						log.warn("Handshake init too small, buffering");
						return false;
					}
					ByteBuffer hs = ByteBuffer.allocate(HANDSHAKE_SIZE);
					hs.setAutoExpand(true);
					hs.put(in).flip();
					out.write(hs);
					conn.setState(Connection.STATE_CONNECTED);
					return true;
				} 
				
				
			}
			
			final byte headerByte = in.get();
			final byte channelId = RTMPUtils.decodeChannelId(headerByte);
			
			if(channelId<0)
				throw new ProtocolViolationException("Bad channel id");
			
			// Get the channel, and header size
			final Channel channel = conn.getChannel(channelId);
			final byte headerSize = (byte) RTMPUtils.decodeHeaderSize(headerByte);
			int headerLength = RTMPUtils.getHeaderLength(headerSize);
			
			if(headerLength > in.remaining()) {
				log.debug("Header too small, buffering");
				in.position(startPosition);
				return false;
			}
			
			// Read the header
			PacketHeader header = null;
			in.position(in.position()-1);
			
			header = decodeHeader(in,channel.getLastReadHeader());
			
			if(header==null){
				log.warn("Header is null");
				throw new ProtocolViolationException("Header is null, check for error");
			}
				
			log.debug(header);
			
			channel.setLastReadHeader(header);
			InPacket packet = channel.getInPacket();
			
			if(packet==null){
				packet = newPacket(header);
				channel.setInPacket(packet);
			}
			
			final ByteBuffer buf = packet.getMessage().getData();
			int readRemaining = header.getSize() - buf.position();
			//readRemaining += Math.floor(header.getSize()/128);
			
			int chunkSize = header.getChunkSize();
			final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
			
			//log.debug("Read amount: "+readAmount);
			
			if(in.remaining() < readAmount) {
				if(log.isDebugEnabled())
					log.debug("Chunk too small, buffering ("+in.remaining()+","+readAmount);
				// log.debug("Remaining: "+in.remaining());
				in.position(startPosition);
				return false;
			}
			
			//log.debug("in: "+in.remaining()+" read: "+readAmount+" pos: "+buf.position());
			
			try {
				BufferUtils.put(buf, in, readAmount);
			} catch (RuntimeException e) {
				log.error("Error",e);
				throw new ProtocolViolationException("Error copying buffer");
			}
			
			if(buf.position() >= header.getSize()){
				if(log.isDebugEnabled())
					log.debug("Finished read, decode packet");
				buf.flip();
				decodeMessage(packet.getMessage());
				packet.getMessage().setTimestamp(packet.getSource().getTimer());
				ioLog.debug(packet.getSource());
				ioLog.debug(packet.getMessage());
				out.write(packet);
				channel.setInPacket(null);
			} 
			return true;

		} catch (RuntimeException e){
			log.error("Error", e);
			throw new ProtocolViolationException("Error copying buffer");
		}
		
	}
	
	public PacketHeader decodeHeader(ByteBuffer in, PacketHeader lastHeader){
		
		final byte headerByte = in.get();
		final byte channelId = RTMPUtils.decodeChannelId(headerByte);
		final byte headerSize = (byte) RTMPUtils.decodeHeaderSize(headerByte);
		PacketHeader header = new PacketHeader();
		header.setChannelId(channelId);
		
		switch(headerSize){
		
		case HEADER_NEW:
			//log.debug("0: Full headers");	
			//log.debug(in.getHexDump());
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setSize(RTMPUtils.readMediumInt(in));
			header.setDataType(in.get());
			header.setStreamId(RTMPUtils.readReverseInt(in));
			break;
			
		case HEADER_SAME_SOURCE:			
			//log.debug("1: Same source as last time");
			//log.debug(in.getHexDump());
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setSize(RTMPUtils.readMediumInt(in));
			header.setDataType(in.get());
			header.setStreamId(lastHeader.getStreamId());
			break;
			
		case HEADER_TIMER_CHANGE:
			//log.debug("2: Only the timer changed");
			//log.debug(in.getHexDump());
			header.setTimer(RTMPUtils.readUnsignedMediumInt(in));
			header.setSize(lastHeader.getSize());
			header.setDataType(lastHeader.getDataType());
			header.setStreamId(lastHeader.getStreamId());
			break;
			
		case HEADER_CONTINUE:
			//log.debug("3: Continue");
			header = lastHeader;
			break;
			
		default:
			log.error("Unexpected header size: "+headerSize);
			return null;
		
		}
		return header;
	}
	
	public InPacket newPacket(PacketHeader header){
		final InPacket packet = new InPacket();
		packet.setSource(header);
		switch(header.getDataType()){
		case TYPE_HANDSHAKE:
			packet.setMessage(new Handshake());
			break;
		case TYPE_INVOKE:
			packet.setMessage(new Invoke());
			break;
		case TYPE_NOTIFY:
			packet.setMessage(new Notify());
			break;
		case TYPE_PING:
			packet.setMessage(new Ping());
			break;
		case TYPE_STREAM_BYTES_READ:
			packet.setMessage(new StreamBytesRead());
			break;
		case TYPE_AUDIO_DATA:
			packet.setMessage(new AudioData());
			break;
		case TYPE_VIDEO_DATA:
			packet.setMessage(new VideoData());
			break;
		case TYPE_SHARED_OBJECT:
			packet.setMessage(new SharedObject());
			break;
		default:
			packet.setMessage(new Unknown(header.getDataType()));
			break;
		}
		return packet;
	}
	
	public void decodeMessage(Message message) {
		switch(message.getDataType()){
		case TYPE_HANDSHAKE:
			// none needed
			break;
		case TYPE_INVOKE:
			decodeInvoke((Invoke) message);
			break;
		case TYPE_NOTIFY:
			decodeNotify((Notify) message);
			break;
		case TYPE_PING:
			decodePing((Ping) message);
			break;
		case TYPE_STREAM_BYTES_READ:
			decodeStreamBytesRead((StreamBytesRead) message);
			break;
		case TYPE_AUDIO_DATA:
			decodeAudioData((AudioData) message);
			break;
		case TYPE_VIDEO_DATA:
			decodeVideoData((VideoData) message);
			break;
		case TYPE_SHARED_OBJECT:
			decodeSharedObject((SharedObject) message);
			break;
		}
	}
	
	public void decodeSharedObject(SharedObject so) {
		
		log.info("> "+so.getData().getHexDump());
		
		final ByteBuffer data = so.getData();

		Input input = new Input(data);
		so.setName(input.getString(data));
		data.skip(12);
		while(data.hasRemaining()){
			byte type = data.get();
			log.info("type: "+type);
			SharedObjectEvent event = new SharedObjectEvent(type,null,null);
			int length = data.getInt();
			if(length > 0){
				event.setKey(input.getString(data));
				if(length > event.getKey().length()+2){
					event.setValue(deserializer.deserialize(input));
				}
			}
			so.addEvent(event);
		}
		log.info(so);
	}

	public void decodeInvoke(Invoke invoke){
		
		Input input = new Input(invoke.getData());
	
		String action = (String) deserializer.deserialize(input);
		
		if(log.isDebugEnabled())
			log.debug("Action "+action);
		
		int invokeId = ((Number) deserializer.deserialize(input)).intValue();
		invoke.setInvokeId(invokeId);
				
		Object[] params = new Object[]{};

		if(invoke.getData().hasRemaining()){
			ArrayList paramList = new ArrayList();
			
			// Before the actual parameters we sometimes (connect) get a map
			// of parameters, this is usually null, but if set should be passed
			// to the connection object. 
			final Map connParams = (Map) deserializer.deserialize(input);
			invoke.setConnectionParams(connParams);
			
			while(invoke.getData().hasRemaining()){
				paramList.add(deserializer.deserialize(input));
			}
			params = paramList.toArray();
			if(log.isDebugEnabled()){
				log.debug("Num params: "+paramList.size()); 
				for(int i=0; i<params.length; i++){
					log.debug(" > "+i+": "+params[i]);
				}
			}
		} 
		
		final int dotIndex = action.indexOf(".");
		String serviceName = (dotIndex==-1) ? null : action.substring(0,dotIndex);
		String serviceMethod = (dotIndex==-1) ? action : action.substring(dotIndex+1, action.length());
		
		Call call = new Call(serviceName,serviceMethod,params);
		
		invoke.setCall(call);
	}
	
	public void decodeNotify(Notify notify){

	}
	
	public void decodePing(Ping ping){
		final ByteBuffer in = ping.getData();
		ping.setValue1(in.getShort());
		ping.setValue2(in.getInt());
		if(in.remaining() > 0) ping.setValue3(in.getInt());
		if(log.isDebugEnabled())
			log.debug(ping);
	}
	
	public void decodeStreamBytesRead(StreamBytesRead streamBytesRead){
		final ByteBuffer in = streamBytesRead.getData();
		streamBytesRead.setBytesRead(in.getInt());
	}
	
	public void decodeAudioData(AudioData audioData){
		audioData.setSealed(true);
	}
	
	public void decodeVideoData(VideoData videoData){
		videoData.setSealed(true);
	}
	
}
