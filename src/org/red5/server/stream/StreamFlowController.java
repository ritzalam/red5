package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IBWControllable;
import org.red5.server.api.stream.support.SimpleBandwidthConfigure;

/**
 * TODO Make stream flow controller independent from Bandwidth Controller
 * Controller for stream flow. Adapts flow bandwidth to given configuration.
 */
public class StreamFlowController {
	public static final String KEY = "StreamFlowController";
    /**
     * Logger
     */
	private static final Log log = LogFactory
			.getLog(StreamFlowController.class);
    /**
     * Fixed change value (4 kb)
     */
	private static final int FIXED_CHANGE = 1024 * 4;

    /**
     * Adapt stream flow to bandwidth from given controllable
     * NOTE: Only takes effect on the controllable that has overall bandwidth config
     * @param flow                    Stream flow
     * @param controllable            Flow controllable object
     * @return                        <code>true</code> on success, <code>false</code> otherwise
     * @throws CloneNotSupportedException   Clone operation is not supported by object
     */
	public boolean adaptBandwidthForFlow(IStreamFlow flow,
			IBWControllable controllable) throws CloneNotSupportedException {
		IBandwidthConfigure parentBwConf = controllable
				.getParentBWControllable().getBandwidthConfigure();
		IBandwidthConfigure bwConf = controllable.getBandwidthConfigure();
		long[] parentBandwidth = null;
		if (bwConf == null) {
			if (parentBwConf == null) {
				// No informations about bandwidth settings available
				return false;
			}
			parentBandwidth = parentBwConf.getChannelBandwidth();
			if (parentBandwidth[IBandwidthConfigure.OVERALL_CHANNEL] < 0) return false;
			bwConf = new SimpleBandwidthConfigure(parentBwConf);
			controllable.setBandwidthConfigure(bwConf);
		} else {
			if (parentBwConf != null) {
				parentBandwidth = parentBwConf.getChannelBandwidth();
			}
		}
		long[] channelBandwidth = bwConf.getChannelBandwidth();
		if (channelBandwidth[IBandwidthConfigure.OVERALL_CHANNEL] < 0) return false;
		boolean change = false;
		final int bufferTime = flow.getBufferTime();
		long bw = channelBandwidth[IBandwidthConfigure.OVERALL_CHANNEL];
		// log.info("Buffer: " + bufferTime + " min: " + flow.getMinTimeBuffer()
		// + " max: " + flow.getMaxTimeBuffer());
		if (bufferTime > flow.getMaxTimeBuffer()) {
			if (flow.isBufferTimeIncreasing()) {
				if (bw > flow.getDataBitRate()) {
					bw = flow.getDataBitRate();
				}
				bw -= computeChange(bw);
				change = true;
				//log.info("<<");
			}
		} else if (bufferTime < flow.getMinTimeBuffer()) {
			if (!flow.isBufferTimeIncreasing()) {
				if (bw < flow.getDataBitRate()) {
					bw = flow.getDataBitRate();
				}
				bw += computeChange(bw) * 2;
				change = true;
				//log.info(">>");
			}
		} else if (bufferTime < (flow.getMinTimeBuffer() + ((flow
				.getMaxTimeBuffer() - flow.getMinTimeBuffer()) / 1.8))) {
			if (!flow.isBufferTimeIncreasing()) {
				if (bw < flow.getDataBitRate() * 0.9) {
					bw = (flow.getDataBitRate());
				}
				bw += computeChange(bw) / 2;
				change = true;
				//log.info('>');
			}
		} else if (bufferTime < flow.getMaxTimeBuffer()) {
			if (flow.isBufferTimeIncreasing()) {
				if (bw > flow.getDataBitRate() * 1.1) {
					bw = (flow.getDataBitRate());
				}
				bw -= computeChange(bw) / 2;
				change = true;
				//log.info('<');
			}
		}

		//change = false;
		if (change) {
			if (parentBandwidth != null && bw > parentBandwidth[IBandwidthConfigure.OVERALL_CHANNEL]) {
				bw = parentBandwidth[IBandwidthConfigure.OVERALL_CHANNEL];
			} else if (bw < FIXED_CHANGE) {
				bw = FIXED_CHANGE;
			}
			channelBandwidth[IBandwidthConfigure.OVERALL_CHANNEL] = bw; 
			controllable.setBandwidthConfigure(bwConf);
		}
		if (log.isDebugEnabled()) {
			log.debug("bw: " + bw + " buf: " + bufferTime + " data bit rate: " + flow.getDataBitRate());
		}
		return change;
	}

    /**
     * Return fixed change by now
     * @param bw                 Bandwidth value
     * @return                   Fixed change, 1024 * 4
     */
    int computeChange(long bw) {
		return FIXED_CHANGE;
	}
}