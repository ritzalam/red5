package org.red5.server.api.stream.support;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import org.red5.server.api.IBandwidthConfigure;
import org.springframework.core.style.ToStringCreator;

/**
 * This class is the only IBandwidthConfigure implementation in 0.5. It's a kind
 * of ValueObject (item with a set of values that just stores data) that is used
 * to configure Red5 application bandwidth settings.
 * 
 * This class helps you to configure maximum burst amount, level of bandwidth
 * from server to client and vice versa, set bandwidth amount for audio and
 * video separately and so forth.
 * 
 * Say if you need to limit bandwidth for each connection of create a copy of a
 * stream with lower quality you use instance of this class to set bandwidth
 * parameters.
 */
public class SimpleBandwidthConfigure implements IBandwidthConfigure {
	private long audioBandwidth;

	private long videoBandwidth;

	private long overallBandwidth = -1;

	private long upstreamBandwidth = -1;

	private long downstreamBandwidth = -1;

	private long burst = 0;

	private long maxBurst = 0;

	public SimpleBandwidthConfigure() {

	}

	public SimpleBandwidthConfigure(IBandwidthConfigure config) {
		this.audioBandwidth = config.getAudioBandwidth();
		this.videoBandwidth = config.getVideoBandwidth();
		this.overallBandwidth = config.getOverallBandwidth();
		this.upstreamBandwidth = config.getUpstreamBandwidth();
		this.downstreamBandwidth = config.getDownstreamBandwidth();
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

	public long getUpstreamBandwidth() {
		return upstreamBandwidth;
	}

	public void setUpstreamBandwidth(long upstreamBandwidth) {
		this.upstreamBandwidth = upstreamBandwidth;
	}

	public long getDownstreamBandwidth() {
		return downstreamBandwidth;
	}

	public void setDownstreamBandwidth(long downstreamBandwidth) {
		this.downstreamBandwidth = downstreamBandwidth;
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

	@Override
	public String toString() {
		return new ToStringCreator(this).append("ALL", getOverallBandwidth())
				.append("BURST", getBurst()).append("MAX", getMaxBurst())
				.toString();
	}

	@Override
	public IBandwidthConfigure clone() {
		IBandwidthConfigure clone = new SimpleBandwidthConfigure();
		clone.setOverallBandwidth(getOverallBandwidth());
		clone.setAudioBandwidth(getAudioBandwidth());
		clone.setMaxBurst(getMaxBurst());
		clone.setVideoBandwidth(getVideoBandwidth());
		clone.setBurst(getBurst());
		return clone;
	}

}
