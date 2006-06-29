package org.red5.server.stream.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.StreamBytesRead;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
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
			IRTMPEvent msg = rtmpMsg.getBody();
//			log.debug("ts :" + msg.getTimestamp());
			switch(msg.getDataType()){
			case Constants.TYPE_STREAM_METADATA:
				Notify notify = new Notify(((Notify) msg).getData().asReadOnlyBuffer());
				notify.setHeader(msg.getHeader());
				notify.setTimestamp(msg.getTimestamp());
				video.write(notify);
				break;
			case Constants.TYPE_VIDEO_DATA:
				VideoData videoData = new VideoData(((VideoData) msg).getData().asReadOnlyBuffer());
				videoData.setHeader(msg.getHeader());
				videoData.setTimestamp(msg.getTimestamp());
				video.write(videoData);
				break;
			case Constants.TYPE_AUDIO_DATA:
				AudioData audioData = new AudioData(((AudioData) msg).getData().asReadOnlyBuffer());
				audioData.setHeader(msg.getHeader());
				audioData.setTimestamp(msg.getTimestamp());
				audio.write(audioData);
				break;
			case Constants.TYPE_PING:
				conn.ping((Ping) msg);
				break;
			case Constants.TYPE_STREAM_BYTES_READ:
				video.write((StreamBytesRead) msg);
				break;
			default:
				data.write(msg);
			break;
			}
		}
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		// TODO close channels on pipe disconnect
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub
		
	}

}
