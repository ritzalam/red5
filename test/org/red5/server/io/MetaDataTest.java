package org.red5.server.io;

/*
 * * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright © 2006 by respective authors. All rights reserved.
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
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 */

import junit.framework.Assert;
import junit.framework.TestCase;

import org.red5.io.flv.meta.MetaData;


/**
 * MetaData TestCase
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author daccattato (daccattato@gmail.com)
 * @version 0.3
 */
public class MetaDataTest extends TestCase {
	MetaData data;

	/** Constructs a new MetaDataTest. */
    public MetaDataTest() {
		data = new MetaData();
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
