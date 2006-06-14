package org.red5.server.api.stream;

public interface IStreamCodecInfo {
	boolean hasAudio();
	boolean hasVideo();
	String getAudioCodecName();
	String getVideoCodecName();
}
