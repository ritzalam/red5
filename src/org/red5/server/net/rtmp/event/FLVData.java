/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp.event;

import org.apache.mina.core.buffer.IoBuffer;

public class FLVData {

	/*
	 * 0x08 AUDIO Contains an audio packet similar to a SWF SoundStreamBlock
	 * plus codec information 
	 * 0x09 VIDEO Contains a video packet similar to a SWF VideoFrame plus
	 * codec information 
	 * 0x12 META Contains two AMF packets, the name of the event and the data
	 * to go with it
	 */

	/*
	 * soundType (byte & 0x01) == 0 0: mono, 1: stereo 
	 * soundSize (byte & 0x02) == 1 0: 8-bit, 2: 16-bit 
	 * soundRate (byte & 0x0C) == 2 0: 5.5kHz, 1: 11kHz, 2: 22kHz, 3: 44kHz 
	 * soundFormat (byte & 0xf0) == 4 0: Uncompressed, 1: ADPCM, 2: MP3, 
	 *     5: Nellymoser 8kHz mono, 6: Nellymoser
	 *     
	 * codecID (byte & 0x0f) == 0 2: Sorensen H.263, 3: Screen video, 4: On2 VP6
	 * frameType (byte & 0xf0) == 4 1: keyframe, 2: inter frame, 3: disposable
	 * inter frame
	 * 
	 * http://www.adobe.com/devnet/flv/pdf/video_file_format_spec_v10.pdf
	 */

	protected IoBuffer data;

	protected int timestamp = -1;

	/**
	 * Getter for disposable state
	 *
	 * @return  <code>true</code> if FLV data is disposable, <code>false</code> otherwise
	 */
	public boolean isDisposable() {
		return false;
	}

	/**
	 * Audio data
	 */
	public static final int TYPE_AUDIO = 8;

	/**
	 * Video data
	 */
	public static final int TYPE_VIDEO = 9;

	/**
	 * Metadata
	 */
	public static final int TYPE_METADATA = 12;

	/**
	 * Keyframe
	 */
	public static final int FRAMETYPE_KEYFRAME = 1;

	/**
	 * Interframe
	 */
	public static final int FRAMETYPE_INTERFRAME = 2;

	/**
	 * Disposable
	 */
	public static final int FRAMETYPE_DISPOSABLE = 3;

	/**
	 * Server-side generated keyframe
	 */
	public static final int FRAMETYPE_SERVERSIDE_KEYFRAME = 4;
	
	/**
	 * Video information or command
	 */
	public static final int FRAMETYPE_INFO = 5;
	
	/**
	 * Sound size when 8 khz quality marker
	 */
	public static final int SOUND_SIZE_8_BIT = 0;

	/**
	 * Sound size when 16 khz quality marker
	 */
	public static final int SOUND_SIZE_16_BIT = 2;

	/**
	 * Sound size when 5.5 khz rate marker
	 */
	public static final int SOUND_RATE_5_5_KHZ = 1;

	/**
	 * Sound size when 11 khz rate marker
	 */
	public static final int SOUND_RATE_11_KHZ = 2;

	/**
	 * Sound size when 22 khz rate marker
	 */
	public static final int SOUND_RATE_22_KHZ = 3;

	/**
	 * Sound size when 44 khz rate marker
	 */
	public static final int SOUND_RATE_44_KHZ = 4;

	/**
	 * Getter for codec. Returns 0 by now.
	 *
	 * @return  Codec
	 */
	public int getCodec() {
		return 0;
	}

}