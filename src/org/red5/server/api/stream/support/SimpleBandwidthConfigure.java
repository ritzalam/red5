package org.red5.server.api.stream.support;

import org.red5.server.api.stream.IBandwidthConfigure;
import org.red5.server.messaging.IFilter;

public class SimpleBandwidthConfigure implements IBandwidthConfigure {
	private long audioBandwidth;
	private long videoBandwidth;
	private long dataBandwidth;
	private IFilter customBandwidthController;
	
	public long getAudioBandwidth() {
		return audioBandwidth;
	}
	
	public void setAudioBandwidth(long audioBandwidth) {
		this.audioBandwidth = audioBandwidth;
	}
	
	public IFilter getCustomBandwidthController() {
		return customBandwidthController;
	}
	
	public void setCustomBandwidthController(IFilter customBandwidthController) {
		this.customBandwidthController = customBandwidthController;
	}
	
	public long getDataBandwidth() {
		return dataBandwidth;
	}
	
	public void setDataBandwidth(long dataBandwidth) {
		this.dataBandwidth = dataBandwidth;
	}
	
	public long getVideoBandwidth() {
		return videoBandwidth;
	}
	
	public void setVideoBandwidth(long videoBandwidth) {
		this.videoBandwidth = videoBandwidth;
	}	
}
