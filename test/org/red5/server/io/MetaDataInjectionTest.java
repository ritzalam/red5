package org.red5.server.io;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
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
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.mina.common.ByteBuffer;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.IFLVService;
import org.red5.io.flv.impl.FLVService;
import org.red5.io.flv.impl.Tag;
import org.red5.io.flv.meta.ICueType;
import org.red5.io.flv.meta.IMetaCue;
import org.red5.io.flv.meta.MetaCue;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author daccattato(daccattato@gmail.com)
 * @version 0.3
 */
public class MetaDataInjectionTest extends TestCase {

	private IFLVService service;

	/**
	 * SetUp is called before each test
	 * @return void
	 */
	@Override
	public void setUp() {
		service = new FLVService();
		service.setSerializer(new Serializer());
		service.setDeserializer(new Deserializer());
	}

	/**
	 * Test MetaData injection
	 * @throws IOException
	 */
	public void testMetaDataInjection() throws IOException {
		File f = new File("tests/test_cue1.flv");

		if(f.exists()) {
			f.delete();
		}

		// Create new file
		f.createNewFile();

		// Use service to grab FLV file
		IFLV flv = (IFLV) service.getStreamableFile(f);

		// Grab a writer for writing a new FLV
		ITagWriter writer = flv.getWriter();

		// Create a reader for testing
		File readfile = new File("tests/test_cue.flv");
		IFLV readflv = (IFLV) service.getStreamableFile(readfile);

		// Grab a reader for reading a FLV in
		ITagReader reader = readflv.getReader();

		// Inject MetaData
		writeTagsWithInjection(reader, writer);

	}

	/**
	 * Write FLV tags and inject Cue Points
	 * @param reader
	 * @param writer
	 * @throws IOException
	 */
	private void writeTagsWithInjection(ITagReader reader, ITagWriter writer) throws IOException {

		IMetaCue cp = new MetaCue();
		cp.setName("cue_1");
		cp.setTime(0.01);
		cp.setType(ICueType.EVENT);

		IMetaCue cp1 = new MetaCue();
		cp1.setName("cue_1");
		cp1.setTime(2.01);
		cp1.setType(ICueType.EVENT);

		// Place in TreeSet for sorting
		TreeSet ts = new TreeSet();
		ts.add(cp);
		ts.add(cp1);

		int cuePointTimeStamp = getTimeInMilliseconds(ts.first());

		ITag tag = null;
		ITag injectedTag = null;

		while(reader.hasMoreTags()) {
			tag = reader.readTag();

			if(tag.getDataType() != IoConstants.TYPE_METADATA) {
				//injectNewMetaData();
			} else {
				//in
			}

			// if there are cuePoints in the TreeSet
			if(!ts.isEmpty()) {

				// If the tag has a greater timestamp than the
				// cuePointTimeStamp, then inject the tag
				while(tag.getTimestamp() > cuePointTimeStamp) {

					injectedTag = injectMetaData(ts.first(), tag);
					writer.writeTag(injectedTag);
					tag.setPreviousTagSize((injectedTag.getBodySize() + 11));

					// Advance to the next CuePoint
					ts.remove(ts.first());

					if(ts.isEmpty()) {
						break;
					}

					cuePointTimeStamp = getTimeInMilliseconds(ts.first());
				}
			}

			writer.writeTag(tag);

		}
	}

	/**
	 * Injects metadata (Cue Points) into a tag
	 * @param cue
	 * @param writer
	 * @param tag
	 * @return ITag tag
	 */
	private ITag injectMetaData(Object cue, ITag tag) {

		IMetaCue cp = (MetaCue) cue;
		Output out = new Output(ByteBuffer.allocate(1000));
		Serializer ser = new Serializer();
		ser.serialize(out,"onCuePoint");
		ser.serialize(out,cp);

		ByteBuffer tmpBody = out.buf().flip();
		int tmpBodySize = out.buf().limit();
		int tmpPreviousTagSize = tag.getPreviousTagSize();
		byte tmpDataType = ((IoConstants.TYPE_METADATA));
		int tmpTimestamp = getTimeInMilliseconds(cp);

		return new Tag(tmpDataType, tmpTimestamp, tmpBodySize, tmpBody, tmpPreviousTagSize);

	}

	/**
	 * Returns a timestamp in milliseconds
	 * @param object
	 * @return int time
	 */
	private int getTimeInMilliseconds(Object object) {
		IMetaCue cp = (MetaCue) object;
		return (int) (cp.getTime() * 1000.00);

	}

}
