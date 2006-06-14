package org.red5.server.api.stream;

import org.red5.server.messaging.IFilter;

public interface IBandwidthConfigure {
	void setAudioBandwidth(long bw);
	long getAudioBandwidth();
	void setVideoBandwidth(long bw);
	long getVideoBandwidth();
	void setDataBandwidth(long bw);
	long getDataBandwidth();
	void setCustomBandwidthController(IFilter bwc);
	IFilter getCustomBandwidthController();
}
