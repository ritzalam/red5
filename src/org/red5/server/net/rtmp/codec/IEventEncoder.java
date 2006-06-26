package org.red5.server.net.rtmp.codec;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.StreamBytesRead;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.so.ISharedObjectMessage;

public interface IEventEncoder {

	public abstract ByteBuffer encodeNotify(Notify notify);

	public abstract ByteBuffer encodeInvoke(Invoke invoke);

	public abstract ByteBuffer encodePing(Ping ping);

	public abstract ByteBuffer encodeStreamBytesRead(
			StreamBytesRead streamBytesRead);

	public abstract ByteBuffer encodeAudioData(AudioData audioData);

	public abstract ByteBuffer encodeVideoData(VideoData videoData);
	
	public abstract ByteBuffer encodeUnknown(Unknown unknown);

	public abstract ByteBuffer encodeChunkSize(ChunkSize chunkSize);
	
	public abstract ByteBuffer encodeSharedObject(ISharedObjectMessage so);
	
}