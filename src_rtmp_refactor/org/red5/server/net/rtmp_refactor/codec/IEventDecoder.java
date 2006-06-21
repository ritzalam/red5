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

public interface IEventDecoder {

	public abstract Unknown decodeUnknown(ByteBuffer in);

	public abstract ChunkSize decodeChunkSize(ByteBuffer in);

	public abstract ISharedObjectMessage decodeSharedObject(ByteBuffer in);

	public abstract Notify decodeNotify(ByteBuffer in);

	public abstract Invoke decodeInvoke(ByteBuffer in);

	public abstract Ping decodePing(ByteBuffer in);

	public abstract StreamBytesRead decodeStreamBytesRead(ByteBuffer in);

	public abstract AudioData decodeAudioData(ByteBuffer in);

	public abstract VideoData decodeVideoData(ByteBuffer in);

}