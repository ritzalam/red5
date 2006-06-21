package org.red5.server.net.rtmp_refactor.codec;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp_refactor.event.AudioData;
import org.red5.server.net.rtmp_refactor.event.ChunkSize;
import org.red5.server.net.rtmp_refactor.event.Invoke;
import org.red5.server.net.rtmp_refactor.event.Notify;
import org.red5.server.net.rtmp_refactor.event.Ping;
import org.red5.server.net.rtmp_refactor.event.StreamBytesRead;
import org.red5.server.net.rtmp_refactor.event.Unknown;
import org.red5.server.net.rtmp_refactor.event.VideoData;
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