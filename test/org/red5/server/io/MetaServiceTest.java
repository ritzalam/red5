package org.red5.server.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors. All rights reserved.
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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.red5.io.flv.IFLV;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.flv.meta.ICueType;
import org.red5.io.flv.meta.IMetaCue;
import org.red5.io.flv.meta.MetaCue;
import org.red5.io.flv.meta.MetaData;
import org.red5.io.flv.meta.MetaService;
import org.red5.io.flv.meta.Resolver;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.cache.NoCacheImpl;

public class MetaServiceTest extends TestCase {

	private FLVService service;
	private MetaService metaService;

	/** {@inheritDoc} */
    @Override
	protected void setUp() throws Exception {
		super.setUp();

		// Create a FLV Service
		service = new FLVService();

		// Create a Meta Service
		metaService = new MetaService();
		metaService.setSerializer(new Serializer());
		metaService.setDeserializer(new Deserializer());
		metaService.setResolver(new Resolver());
	}

	/**
	 * Test writing meta data
	 * @throws IOException
	 */
	public void testWrite() throws IOException {
		// Get MetaData to embed
		MetaData<?, ?> meta = createMeta();
		// Read in a FLV file for reading tags
		File tmp = new File("fixtures/test.flv");
		System.out.println("Path: "+ tmp.getAbsolutePath());
		IFLV flv = (IFLV) service.getStreamableFile(tmp);
		flv.setCache(NoCacheImpl.getInstance());
		// set the MetaService
		flv.setMetaService(metaService);
		// set the MetaData
		flv.setMetaData(meta);
	}

	/**
	 * Create some test Metadata for insertion.
	 *
	 * @return MetaData meta
	 */
	private MetaData<?, ?> createMeta() {
		IMetaCue metaCue[] = new MetaCue[2];

	  	IMetaCue cp = new MetaCue<Object, Object>();
		cp.setName("cue_1");
		cp.setTime(0.01);
		cp.setType(ICueType.EVENT);

		IMetaCue cp1 = new MetaCue<Object, Object>();
		cp1.setName("cue_2");
		cp1.setTime(0.03);
		cp1.setType(ICueType.EVENT);

		// add cuepoints to array
		metaCue[0] = cp;
		metaCue[1] = cp1;

		MetaData<?, ?> meta = new MetaData<Object, Object>();
		meta.setMetaCue(metaCue);
		meta.setCanSeekToEnd(true);
		meta.setDuration(300);
		meta.setFrameRate(15);
		meta.setHeight(400);
		meta.setWidth(300);

		return meta;
	}

}
