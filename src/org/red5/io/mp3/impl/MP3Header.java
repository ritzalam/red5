package org.red5.io.mp3.impl;

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

/**
 * Header of a MP3 frame.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @see <a href="http://mpgedit.org/mpgedit/mpeg_format/mpeghdr.htm">File format</a>
 */

public class MP3Header {

	private static final int[][] BITRATES = {
			{ 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416,
					448, -1 },
			{ 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320,
					384, -1 },
			{ 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320,
					-1 },
			{ 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224,
					256, -1 },
			{ 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 }, };

	private static final int[][] SAMPLERATES = {
	// Version 2.5
			{ 11025, 12000, 8000, -1 },
			// Unknown version
			{ -1, -1, -1, -1 },
			// Version 2
			{ 22050, 24000, 16000, -1 },
			// Version 1
			{ 44100, 44800, 32000, -1 }, };

	private int data;

	private byte audioVersionId;

	private byte layerDescription;

	private boolean protectionBit;

	private byte bitRateIndex;

	private byte samplingRateIndex;

	private boolean paddingBit;

	private byte channelMode;

	public MP3Header(int data) throws Exception {
		if ((data & 0xffe00000) != 0xffe00000) {
			throw new Exception("invalid frame sync");
		}

		this.data = data;
		// Strip signed bit
		data &= 0x1fffff;
		audioVersionId = (byte) ((data >> 19) & 3);
		layerDescription = (byte) ((data >> 17) & 3);
		protectionBit = ((data >> 16) & 1) == 0;
		bitRateIndex = (byte) ((data >> 12) & 15);
		samplingRateIndex = (byte) ((data >> 10) & 3);
		paddingBit = ((data >> 9) & 1) != 0;
		channelMode = (byte) ((data >> 6) & 3);
	}

	/**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'.
     */
    public int getData() {
		return data;
	}

	/**
     * Getter for property 'stereo'.
     *
     * @return Value for property 'stereo'.
     */
    public boolean isStereo() {
		return (channelMode != 3);
	}

	/**
     * Getter for property 'protected'.
     *
     * @return Value for property 'protected'.
     */
    public boolean isProtected() {
		return protectionBit;
	}

	/**
     * Getter for property 'bitRate'.
     *
     * @return Value for property 'bitRate'.
     */
    public int getBitRate() {
		int result;
		switch (audioVersionId) {
			case 1:
				// Unknown
				return -1;

			case 0:
			case 2:
				// Version 2 or 2.5
				if (layerDescription == 3) {
					// Layer 1
					result = BITRATES[3][bitRateIndex];
				} else if (layerDescription == 2 || layerDescription == 1) {
					// Layer 2 or 3
					result = BITRATES[4][bitRateIndex];
				} else {
					// Unknown layer
					return -1;
				}
				break;

			case 3:
				// Version 1
				if (layerDescription == 3) {
					// Layer 1
					result = BITRATES[0][bitRateIndex];
				} else if (layerDescription == 2) {
					// Layer 2
					result = BITRATES[1][bitRateIndex];
				} else if (layerDescription == 1) {
					// Layer 3
					result = BITRATES[2][bitRateIndex];
				} else {
					// Unknown layer
					return -1;
				}
				break;

			default:
				// Unknown version
				return -1;
		}

		return result * 1000;
	}

	/**
     * Getter for property 'sampleRate'.
     *
     * @return Value for property 'sampleRate'.
     */
    public int getSampleRate() {
		return SAMPLERATES[audioVersionId][samplingRateIndex];
	}

	/**
	 * Calculate the size of a MP3 frame for this header.
	 * 
	 * @return size of the frame including the header
	 */
	public int frameSize() {
		switch (layerDescription) {
			case 3:
				// Layer 1
				return (12 * getBitRate() / getSampleRate() + (paddingBit ? 1
						: 0)) * 4;

			case 2:
			case 1:
				// Layer 2 and 3
				if (audioVersionId == 3) {
					// MPEG 1
					return 144 * getBitRate() / getSampleRate()
							+ (paddingBit ? 1 : 0);
				} else {
					// MPEG 2 or 2.5
					return 72 * getBitRate() / getSampleRate()
							+ (paddingBit ? 1 : 0);
				}

			default:
				// Unknown
				return -1;
		}
	}

	/**
	 * Return the duration of the frame for this header.
	 * 
	 * @return the duration in milliseconds
	 */
	public double frameDuration() {
		switch (layerDescription) {
			case 3:
				// Layer 1
				return 384 / (getSampleRate() * 0.001);

			case 2:
			case 1:
				if (audioVersionId == 3) {
					// MPEG 1, Layer 2 and 3
					return 1152 / (getSampleRate() * 0.001);
				} else {
					// MPEG 2 or 2.5, Layer 2 and 3
					return 576 / (getSampleRate() * 0.001);
				}

			default:
				// Unknown
				return -1;
		}
	}

}
