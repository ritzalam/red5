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

public interface IEventDecoder {

	public abstract Unknown decodeUnknown(byte dataType, ByteBuffer in);

	public abstract ChunkSize decodeChunkSize(ByteBuffer in);

	public abstract ISharedObjectMessage decodeSharedObject(ByteBuffer in);

	public abstract Notify decodeNotify(ByteBuffer in);

	public abstract Invoke decodeInvoke(ByteBuffer in);

	public abstract Ping decodePing(ByteBuffer in);

	public abstract StreamBytesRead decodeStreamBytesRead(ByteBuffer in);

	public abstract AudioData decodeAudioData(ByteBuffer in);

	public abstract VideoData decodeVideoData(ByteBuffer in);

}