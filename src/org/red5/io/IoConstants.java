package org.red5.io;

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
 * Constants found in FLV files / streams.
 *
 */
public interface IoConstants {

	public static final byte TYPE_VIDEO = 0x09;

	public static final byte TYPE_AUDIO = 0x08;

	public static final byte TYPE_METADATA = 0x12;

	public static final byte MASK_SOUND_TYPE = 0x01;

	public static final byte FLAG_TYPE_MONO = 0x00;

	public static final byte FLAG_TYPE_STEREO = 0x01;

	public static final byte MASK_SOUND_SIZE = 0x02;

	public static final byte FLAG_SIZE_8_BIT = 0x00;

	public static final byte FLAG_SIZE_16_BIT = 0x01;

	public static final byte MASK_SOUND_RATE = 0x0C;

	public static final byte FLAG_RATE_5_5_KHZ = 0x00;

	public static final byte FLAG_RATE_11_KHZ = 0x01;

	public static final byte FLAG_RATE_22_KHZ = 0x02;

	public static final byte FLAG_RATE_44_KHZ = 0x03;

	public static final byte MASK_SOUND_FORMAT = 0xF0 - 0xFF; // unsigned 

	public static final byte FLAG_FORMAT_RAW = 0x00;

	public static final byte FLAG_FORMAT_ADPCM = 0x01;

	public static final byte FLAG_FORMAT_MP3 = 0x02;

	public static final byte FLAG_FORMAT_NELLYMOSER_8_KHZ = 0x05;

	public static final byte FLAG_FORMAT_NELLYMOSER = 0x06;

	public static final byte MASK_VIDEO_CODEC = 0x0F;

	public static final byte FLAG_CODEC_H263 = 0x02;

	public static final byte FLAG_CODEC_SCREEN = 0x03;

	public static final byte FLAG_CODEC_VP6 = 0x04;

	public static final byte MASK_VIDEO_FRAMETYPE = 0xF0 - 0xFF; // unsigned 

	public static final byte FLAG_FRAMETYPE_KEYFRAME = 0x01;

	public static final byte FLAG_FRAMETYPE_INTERFRAME = 0x02;

	public static final byte FLAG_FRAMETYPE_DISPOSABLE = 0x03;

}
