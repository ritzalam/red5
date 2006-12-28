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
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;
import org.springframework.core.style.ToStringCreator;

/**
 * Stream flow
 */
public class StreamFlow implements IStreamFlow {
    /**
     * Logger
     */
	private static final Log log = LogFactory.getLog(StreamFlow.class);
    /**
     * Data segment constant
     */
	private static final int DATA = 0;
    /**
     * Audio segment constant
     */
	private static final int AUDIO = 1;
    /**
     * Video segment constant
     */
	private static final int VIDEO = 2;
    /**
     * Streaming flag
     */
	private boolean streaming;
    /**
     * Stream start time
     */
	private long streamStartTime;
    /**
     * Total number of bytes transferred
     */
	private long totalBytesTransfered;
    /**
     * Data times
     */
	private int[] totalDataTimes = new int[] { 0, 0, 0 };
    /**
     * Combined totalBytesTransfered data time
     * Not used?
     */
	private long combinedTotalDataTime;
    /**
     * Segment start time
     */
	private long segmentStartTime;
    /**
     * Segment bytes transfered
     */
	private int segmentBytesTransfered;
    /**
     * Segment data times
     */
	private int[] segmentDataTimes = new int[] { 0, 0, 0 };
    /**
     * Combined segment data time
     */
	private int combinedSegmentDataTime;
    /**
     * Minimal time buffer
     */
	private int minTimeBuffer = 10000;
    /**
     * Max time buffer
     */
    private int maxTimeBuffer = 20000;
    /**
     * Client time buffer
     */
	private long clientTimeBuffer = 10000;
    /**
     * TODO : Ask Steven what the hell is this
     */
	private long zeroToStreamTime = -1;
    /**
     * Buffer time
     */
	private int bufferTime;
    /**
     * Array of last buffer times
     */
	private int lastBufferTimes[] = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    /**
     * Last buffer time
     */
	private int lastBufferTime;
    /**
     * Last buffer time index
     */
	private int lastBufferTimeIndex;
    /**
     * Stream tracker
     */
	private StreamTracker streamTracker = new StreamTracker();

	/** Constructs a new StreamFlow. */
    public StreamFlow() {
	}

	/** {@inheritDoc} */
    public int getMaxTimeBuffer() {
		return maxTimeBuffer;
	}

	/** {@inheritDoc} */
    public void setMaxTimeBuffer(int maxTimeBuffer) {
		this.maxTimeBuffer = maxTimeBuffer;
	}

	/** {@inheritDoc} */
    public int getMinTimeBuffer() {
		return minTimeBuffer;
	}

	/** {@inheritDoc} */
    public void setMinTimeBuffer(int minTimeBuffer) {
		this.minTimeBuffer = minTimeBuffer;
	}

	/** {@inheritDoc} */
    public long getClientTimeBuffer() {
		return clientTimeBuffer;
	}

	/** {@inheritDoc} */
    public void setClientTimeBuffer(long clientTimeBuffer) {
		this.clientTimeBuffer = clientTimeBuffer;
	}

	/** {@inheritDoc} */
    public int getDataBitRate() {
		int dataTime = getSegmentDataTime() / 1000;
		if (dataTime == 0) {
			return 0;
		}
		return ((segmentBytesTransfered * 8) / dataTime);
	}

	/** {@inheritDoc} */
    public int getSegmentBytesTransfered() {
		return segmentBytesTransfered;
	}

	/** {@inheritDoc} */
    public int getSegmentDataTime() {
		if (segmentDataTimes[VIDEO] >= segmentDataTimes[AUDIO]
				&& segmentDataTimes[VIDEO] >= segmentDataTimes[DATA]) {
			return segmentDataTimes[VIDEO];
		} else if (segmentDataTimes[AUDIO] >= segmentDataTimes[VIDEO]
				&& segmentDataTimes[AUDIO] >= segmentDataTimes[DATA]) {
			return segmentDataTimes[AUDIO];
		} else {
			return segmentDataTimes[DATA];
		}
	}

	/** {@inheritDoc} */
    public long getSegmentStreamTime() {
		if (segmentStartTime == 0) {
			return 0;
		}
		return System.currentTimeMillis() - segmentStartTime;
	}

	/** {@inheritDoc} */
    public int getStreamBitRate() {
		return (int) ((segmentBytesTransfered * 8) / (getSegmentStreamTime() / 1000));
	}

	/** {@inheritDoc} */
    public boolean isBufferTimeIncreasing() {
		int combinedBufferTime = 0;
		for (int element : lastBufferTimes) {
			combinedBufferTime += element;
		}
		int newLastBufferTime = (combinedBufferTime / lastBufferTimes.length);
		boolean isIncreasing = (newLastBufferTime >= lastBufferTime);
		// log.debug("lastBufferTime: "+lastBufferTime+"
		// new:"+newLastBufferTime);
		lastBufferTime = newLastBufferTime;
		return isIncreasing;
	}

	/** {@inheritDoc} */
    public long getTotalBytesTransfered() {
		return totalBytesTransfered;
	}

	/** {@inheritDoc} */
    public long getTotalDataTime() {
		if (totalDataTimes[VIDEO] >= totalDataTimes[AUDIO]
				&& totalDataTimes[VIDEO] >= totalDataTimes[DATA]) {
			return totalDataTimes[VIDEO];
		} else if (totalDataTimes[AUDIO] >= totalDataTimes[VIDEO]
				&& totalDataTimes[AUDIO] >= totalDataTimes[DATA]) {
			return totalDataTimes[AUDIO];
		} else {
			return totalDataTimes[DATA];
		}
	}

	/** {@inheritDoc} */
    public long getTotalStreamTime() {
		return System.currentTimeMillis() - streamStartTime;
	}

	/** {@inheritDoc} */
    public long getZeroToStreamTime() {
		if (zeroToStreamTime == -1) {
			return System.currentTimeMillis() - segmentStartTime;
		}
		return zeroToStreamTime;
	}

	/** {@inheritDoc} */
    public int getBufferTime() {
		return (int) (getSegmentDataTime() - getSegmentStreamTime());
	}

	void startSegment() {
		streaming = true;
		final long now = System.currentTimeMillis();
		if (streamStartTime == 0) {
			streamStartTime = now;
		}
		segmentStartTime = now;
	}

	/** {@inheritDoc} */
    public void pause() {
		clear();
	}

	/** {@inheritDoc} */
    public void resume() {
		startSegment();
	}

	/** {@inheritDoc} */
    public void clear() {
		streaming = false;
		segmentBytesTransfered = 0;
		combinedSegmentDataTime = 0;
		for (int i = 0; i < lastBufferTimes.length; i++) {
			lastBufferTimes[i] = 0;
		}
		zeroToStreamTime = -1;
		segmentDataTimes[0] = segmentDataTimes[1] = segmentDataTimes[2] = 0;
		streamTracker.reset();
	}

	/** {@inheritDoc} */
    public void reset() {
		clear();
		streamStartTime = 0;
		totalBytesTransfered = 0;
		combinedTotalDataTime = 0;
		totalDataTimes[0] = totalDataTimes[1] = totalDataTimes[2] = 0;
	}

	/** {@inheritDoc} */
    public void update(RTMPMessage rtmpMsg) {
		//log.info(">>>"+msg.getBody());
		IRTMPEvent msg = rtmpMsg.getBody();
		int ts = streamTracker.add(msg);
		if (!streamTracker.isRelative()) {
			ts = 0;
		}

		switch (msg.getDataType()) {

			case Constants.TYPE_NOTIFY:
			case Constants.TYPE_INVOKE:
				Notify notify = (Notify) msg;
				updateSegment(DATA, notify.getData().limit(), ts);
				break;

			case Constants.TYPE_VIDEO_DATA:
				VideoData videoData = (VideoData) msg;
				updateSegment(VIDEO, videoData.getData().limit(), ts);
				break;

			case Constants.TYPE_AUDIO_DATA:
				AudioData audioData = (AudioData) msg;
				updateSegment(AUDIO, audioData.getData().limit(), ts);
				break;

			default:
				break;

		}

		lastBufferTimes[lastBufferTimeIndex++] = bufferTime;
		if (lastBufferTimeIndex == lastBufferTimes.length) {
			lastBufferTimeIndex = 0;
		}
		int dataTime = getSegmentDataTime();
		if (zeroToStreamTime == -1 && dataTime > clientTimeBuffer) {
			zeroToStreamTime = System.currentTimeMillis() - segmentStartTime;
		}
		bufferTime = (int) (dataTime - getSegmentStreamTime());
	}

	void updateSegment(int index, int bytes, int relativeTime) {
		if (!streaming) {
			startSegment();
		}
		segmentBytesTransfered += bytes;
		segmentDataTimes[index] += relativeTime;
		combinedSegmentDataTime += relativeTime;
		totalBytesTransfered += bytes;
		totalDataTimes[index] += relativeTime;
		combinedTotalDataTime += relativeTime;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return new ToStringCreator(this).append("BT", getBufferTime()).append(
				"SBT", segmentBytesTransfered).append("SDT",
				getSegmentDataTime()).append("SST", getSegmentStreamTime())
				.toString();
	}

}
