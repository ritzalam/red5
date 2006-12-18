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
 * This class is the only IBandwidthConfigure implementation provided in 0.6. It's a kind
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
    /**
     *  Audio bandwidth limit
     */
    private long audioBandwidth;

    /**
     *  Video bandwidth limit
     */
    private long videoBandwidth;

    /**
     *  Overall bandwidth limit
     */
    private long overallBandwidth = -1;

    /**
     *  Upstream (from client to server) banwidth limit
     */
    private long upstreamBandwidth = -1;

    /**
     *  Downstream (from client to server) banwidth limit
     */
    private long downstreamBandwidth = -1;

    /**
     *  Burst value
     */
    private long burst = 0;

    /**
     *  Max burst value
     */
    private long maxBurst = 0;

    /**
     *
     */
    public SimpleBandwidthConfigure() {

	}

    /**
     * Create SimpleBandwidthConfigure from bandwidth configuration context
     * @param config
     */
    public SimpleBandwidthConfigure(IBandwidthConfigure config) {
		this.audioBandwidth = config.getAudioBandwidth();
		this.videoBandwidth = config.getVideoBandwidth();
		this.overallBandwidth = config.getOverallBandwidth();
		this.upstreamBandwidth = config.getUpstreamBandwidth();
		this.downstreamBandwidth = config.getDownstreamBandwidth();
	}

    /**
     * Getter for audio bandwidth limit
     * @return            Audio bandwidth limit
     */
    public long getAudioBandwidth() {
		return audioBandwidth;
	}

    /**
     *
     * @param audioBandwidth
     */
	public void setAudioBandwidth(long audioBandwidth) {
		this.audioBandwidth = audioBandwidth;
	}

    /**
     *
     * @return
     */
	public long getVideoBandwidth() {
		return videoBandwidth;
	}

    /**
     *
     * @param videoBandwidth
     */
	public void setVideoBandwidth(long videoBandwidth) {
		this.videoBandwidth = videoBandwidth;
	}

    /**
     *
     * @return
     */
	public long getOverallBandwidth() {
		return overallBandwidth;
	}

    /**
     *
     * @param overallBandwidth
     */
	public void setOverallBandwidth(long overallBandwidth) {
		this.overallBandwidth = overallBandwidth;
	}

    /**
     *
     * @return
     */
	public long getUpstreamBandwidth() {
		return upstreamBandwidth;
	}

    /**
     *
     * @param upstreamBandwidth
     */
	public void setUpstreamBandwidth(long upstreamBandwidth) {
		this.upstreamBandwidth = upstreamBandwidth;
	}

    /**
     *
     * @return
     */
	public long getDownstreamBandwidth() {
		return downstreamBandwidth;
	}

    /**
     *
     * @param downstreamBandwidth
     */
	public void setDownstreamBandwidth(long downstreamBandwidth) {
		this.downstreamBandwidth = downstreamBandwidth;
	}

    /**
     *
     * @return
     */
	public long getBurst() {
		return burst;
	}

    /**
     *
     * @param burst
     */
	public void setBurst(long burst) {
		this.burst = burst;
	}

    /**
     *
     * @return
     */
	public long getMaxBurst() {
		return maxBurst;
	}

    /**
     *
     * @param maxBurst
     */
	public void setMaxBurst(long maxBurst) {
		this.maxBurst = maxBurst;
	}

    /**
     *
     * @return
     */
    @Override
	public String toString() {
		return new ToStringCreator(this).append("ALL", getOverallBandwidth())
				.append("BURST", getBurst()).append("MAX", getMaxBurst())
				.toString();
	}

    /**
     * Clone bandwidth configuration object
     * @return            Clone of current bandwidth configuration
     */
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
