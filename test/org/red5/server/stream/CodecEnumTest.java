package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import junit.framework.Assert;

import org.junit.Test;
import org.red5.server.stream.codec.AudioCodec;
import org.red5.server.stream.codec.VideoCodec;


public class CodecEnumTest {

	@Test
	public void testAudio() {
		Assert.assertTrue(AudioCodec.MP3.getId() == 2);
		Assert.assertTrue(AudioCodec.SPEEX.getId() == 11);
		Assert.assertTrue(AudioCodec.MP3_8K.getId() == 14);
	}

	@Test
	public void testVideo() {
		Assert.assertTrue(VideoCodec.AVC.getId() == 7);
	}
	
}
