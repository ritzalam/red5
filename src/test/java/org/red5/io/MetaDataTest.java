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

package org.red5.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.red5.io.flv.meta.MetaData;

/**
 * MetaData TestCase
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author daccattato (daccattato@gmail.com)
 */
public class MetaDataTest extends TestCase {
	MetaData<?, ?> data;

	/** Constructs a new MetaDataTest. */
	public MetaDataTest() {
		data = new MetaData<Object, Object>();
	}

	/** {@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		data.setCanSeekToEnd(true);
		data.setDuration(7.347);
		data.setFrameRate(15);
		data.setHeight(333);
		data.setVideoCodecId(4);
		data.setVideoDataRate(400);
		data.setWidth(300);
	}

	/** {@inheritDoc} */
	@Override
	public void tearDown() {
		data = null;
	}

	public void testCanSeekToEnd() {
		Assert.assertEquals(true, data.getCanSeekToEnd());
	}

	public void testDuration() {
		Assert.assertEquals(7.347, data.getDuration(), 0);
	}

	public void testFrameRate() {
		Assert.assertEquals(15.0, data.getFrameRate());
	}

	public void testHeight() {
		Assert.assertEquals(333, data.getHeight());
	}

	public void testVideoCodecId() {
		Assert.assertEquals(4, data.getVideoCodecId());
	}

	public void testVideoDataRate() {
		Assert.assertEquals(400, data.getVideoDataRate());
	}

	public void testWidth() {
		Assert.assertEquals(400, data.getVideoDataRate());
	}

}
