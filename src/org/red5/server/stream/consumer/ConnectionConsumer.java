package org.red5.server.stream.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IConnection;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;

public class ConnectionConsumer implements IPushableConsumer,
		IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(ConnectionConsumer.class);
	
	public static final String KEY = ConnectionConsumer.class.getName();
	
	private RTMPConnection conn;
	private Channel video;
	private Channel audio;
	private Channel data;
	
	public ConnectionConsumer(RTMPConnection conn, byte videoChannel, byte audioChannel, byte dataChannel) {
		this.conn = conn;
		this.video = conn.getChannel(videoChannel);
		this.audio = conn.getChannel(audioChannel);
		this.data = conn.getChannel(dataChannel);
	}
	
	public void pushMessage(IPipe pipe, IMessage message) {
		if (message instanceof StatusMessage) {
			StatusMessage statusMsg = (StatusMessage) message;
			data.sendStatus(statusMsg.getBody());
		}
		else if (message instanceof RTMPMessage) {
			RTMPMessage rtmpMsg = (RTMPMessage) message;
			Message msg = rtmpMsg.getBody();
//			log.debug("ts :" + msg.getTimestamp());
			switch(msg.getDataType()){
			case Constants.TYPE_VIDEO_DATA:
			case Constants.TYPE_STREAM_METADATA:
				video.write(msg);
				break;
			case Constants.TYPE_AUDIO_DATA:
				audio.write(msg);
				break;
			case Constants.TYPE_PING:
				conn.ping((Ping) msg);
				break;
			default:
				data.write(msg);
			break;
			}
		}
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub
		
	}

}
