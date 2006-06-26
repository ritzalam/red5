package org.red5.server.api.stream.support;

import org.red5.server.api.IBandwidthConfigure;

public class SimpleBandwidthConfigure implements IBandwidthConfigure {
	private long audioBandwidth;
	private long videoBandwidth;
	private long overallBandwidth = -1;
	private long burst = 0;
	private long maxBurst = 0;
	
	public SimpleBandwidthConfigure() {
		
	}
	
	public SimpleBandwidthConfigure(IBandwidthConfigure config) {
		this.audioBandwidth = config.getAudioBandwidth();
		this.videoBandwidth = config.getVideoBandwidth();
		this.overallBandwidth = config.getOverallBandwidth();
	}
	
	public long getAudioBandwidth() {
		return audioBandwidth;
	}
	
	public void setAudioBandwidth(long audioBandwidth) {
		this.audioBandwidth = audioBandwidth;
	}
	
	public long getVideoBandwidth() {
		return videoBandwidth;
	}
	
	public void setVideoBandwidth(long videoBandwidth) {
		this.videoBandwidth = videoBandwidth;
	}

	public long getOverallBandwidth() {
		return overallBandwidth;
	}

	public void setOverallBandwidth(long overallBandwidth) {
		this.overallBandwidth = overallBandwidth;
	}

	public long getBurst() {
		return burst;
	}

	public void setBurst(long burst) {
		this.burst = burst;
	}

	public long getMaxBurst() {
		return maxBurst;
	}

	public void setMaxBurst(long maxBurst) {
		this.maxBurst = maxBurst;
	}	
	
}
