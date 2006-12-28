package org.red5.server.stream;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IFlowControllable;

/**
 * Controller for stream flow. Adapts flow bandwidth to given configuration.
 */
public class StreamFlowController {
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
     * @param flow                    Stream flow
     * @param controllable            Flow controllable object
     * @return                        <code>true</code> on success, <code>false</code> otherwise
     * @throws CloneNotSupportedException   Clone operation is not supported by object
     */
	public boolean adaptBandwidthForFlow(IStreamFlow flow,
			IFlowControllable controllable) throws CloneNotSupportedException {

		IBandwidthConfigure parentBwConf = controllable
				.getParentFlowControllable().getBandwidthConfigure();
		IBandwidthConfigure bwConf = controllable.getBandwidthConfigure();
		if (bwConf == null) {
			if (parentBwConf == null) {
				// No informations about bandwidth settings available
				return false;
			}

			bwConf = parentBwConf.clone();
			controllable.setBandwidthConfigure(bwConf);
		}
		boolean change = false;
		final int bufferTime = flow.getBufferTime();
		long bw = bwConf.getOverallBandwidth();
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
		} else {
			//log.info("GOOD!");
		}

		//change = false;
		if (change) {

			if (bw > parentBwConf.getOverallBandwidth()) {
				bw = parentBwConf.getOverallBandwidth();
			} else if (bw < FIXED_CHANGE) {
				bw = FIXED_CHANGE;
			}

			bwConf.setOverallBandwidth(bw);
			controllable.setBandwidthConfigure(bwConf);
		}

		log.debug("bw: " + bw + " buf: " + bufferTime + " data bit rate: "
				+ flow.getDataBitRate());

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