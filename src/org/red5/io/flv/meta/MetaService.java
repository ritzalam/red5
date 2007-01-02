package org.red5.io.flv.meta;

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

import org.apache.mina.common.ByteBuffer;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * MetaService represents a MetaData service in Spring
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class MetaService implements IMetaService {

    /**
     * Source file
     */
    File file;

    /**
     * File input stream
     */
    private FileInputStream fis;

    /**
     * File output stream
     */
    private FileOutputStream fos;

    /**
     * Serializer
     */
    private Serializer serializer;

    /**
     * Deserializer
     */
    private Deserializer deserializer;

    /**
     * Merges metadata objects
     */
    private Resolver resolver;

	/**
	 * @return Returns the resolver.
	 */
	public Resolver getResolver() {
		return resolver;
	}

	/**
	 * @param resolver
	 *            The resolver to set.
	 */
	public void setResolver(Resolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * @return Returns the deserializer.
	 */
	public Deserializer getDeserializer() {
		return deserializer;
	}

	/**
	 * @param deserializer
	 *            The deserializer to set.
	 */
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * @return Returns the serializer.
	 */
	public Serializer getSerializer() {
		return serializer;
	}

	/**
	 * @param serializer
	 *            The serializer to set.
	 */
	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * MetaService constructor
	 */
	public MetaService() {
		super();
	}

	/** {@inheritDoc}
	 */
	public void write(IMetaData meta) throws IOException {
        // Get cue points, FLV reader and writer
		IMetaCue[] metaArr = meta.getMetaCue();
		FLVReader reader = new FLVReader(fis);
		FLVWriter writer = new FLVWriter(fos);

        // Write header (version, data offset, etc)
        writer.writeHeader();

		IMetaData metaData = null;
		ITag tag = null;
		// Read first tag
		if (reader.hasMoreTags()) {
			tag = reader.readTag();
            // If tag is metadata, parse it's body
            if (tag.getDataType() == IoConstants.TYPE_METADATA) {
				metaData = this.readMetaData(tag.getBody());
			}
		}

        // Merged metadata
        IMetaData mergedMeta = (IMetaData) mergeMeta(metaData, meta);
        // Inject metadata
        ITag injectedTag = injectMetaData(mergedMeta, tag);
		//		System.out.println("tag: \n--------\n" + injectedTag);
		writer.writeTag(injectedTag);

		int cuePointTimeStamp = getTimeInMilliseconds(metaArr[0]);
		int counter = 0;
		while (reader.hasMoreTags()) {
			tag = reader.readTag();

			// if there are cuePoints in the TreeSet
			if (counter < metaArr.length) {

				// If the tag has a greater timestamp than the
				// cuePointTimeStamp, then inject the tag
				while (tag.getTimestamp() > cuePointTimeStamp) {

					injectedTag = injectMetaCue(metaArr[counter], tag);
					//					System.out.println("In tag: \n--------\n" + injectedTag);
					writer.writeTag(injectedTag);

					tag.setPreviousTagSize((injectedTag.getBodySize() + 11));

					// Advance to the next CuePoint
					counter++;

					if (counter > (metaArr.length - 1)) {
						break;
					}

					cuePointTimeStamp = getTimeInMilliseconds(metaArr[counter]);

				}
			}

			if (tag.getDataType() != IoConstants.TYPE_METADATA) {
				writer.writeTag(tag);
			}

		}

	}

	/**
	 * Merges the two Meta objects according to user
	 * 
	 * @param metaData        First metadata object
	 * @param md              Second metadata object
	 * @return                Merged metadata
	 */
	private IMeta mergeMeta(IMetaData metaData, IMetaData md) {
		return resolver.resolve(metaData, md);
	}

    /**
     * Injects metadata (other than Cue points) into a tag
     * @param meta           Metadata
     * @param tag            Tag
     * @return               New tag with injected metadata
     */
    private ITag injectMetaData(IMetaData meta, ITag tag) {

		Output out = new Output(ByteBuffer.allocate(1000));
		Serializer ser = new Serializer();
		ser.serialize(out, "onMetaData");
		ser.serialize(out, meta);

		ByteBuffer tmpBody = out.buf().flip();
		int tmpBodySize = out.buf().limit();
		int tmpPreviousTagSize = tag.getPreviousTagSize();
		byte tmpDataType = IoConstants.TYPE_METADATA;
		int tmpTimestamp = 0;

		return new Tag(tmpDataType, tmpTimestamp, tmpBodySize, tmpBody,
				tmpPreviousTagSize);
	}

	/**
	 * Injects metadata (Cue Points) into a tag
	 * 
	 * @param meta           Metadata (cue points)
	 * @param tag            Tag
	 * @return ITag tag      New tag with injected metadata
	 */
	private ITag injectMetaCue(IMetaCue meta, ITag tag) {

		//		IMeta meta = (MetaCue) cue;
		Output out = new Output(ByteBuffer.allocate(1000));
		Serializer ser = new Serializer();
		ser.serialize(out, "onCuePoint");
		ser.serialize(out, meta);

		ByteBuffer tmpBody = out.buf().flip();
		int tmpBodySize = out.buf().limit();
		int tmpPreviousTagSize = tag.getPreviousTagSize();
		byte tmpDataType = IoConstants.TYPE_METADATA;
		int tmpTimestamp = getTimeInMilliseconds(meta);

		return new Tag(tmpDataType, tmpTimestamp, tmpBodySize, tmpBody,
				tmpPreviousTagSize);

	}

	/**
	 * Returns a timestamp of cue point in milliseconds
	 * 
	 * @param metaCue          Cue point
	 * @return int time        Timestamp of given cue point (in milliseconds)
	 */
	private int getTimeInMilliseconds(IMetaCue metaCue) {
		return (int) (metaCue.getTime() * 1000.00);

	}

	/** {@inheritDoc}
	 */
	public void writeMetaData(IMetaData metaData) {
		IMetaCue meta = (MetaCue) metaData;
		Output out = new Output(ByteBuffer.allocate(1000));
		serializer.serialize(out, "onCuePoint");
		serializer.serialize(out, meta);

	}

	/** {@inheritDoc}
	 */
	public void writeMetaCue() {

	}

	/**
	 * @return Returns the file.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            The file to set.
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/** {@inheritDoc} */
    public void setInStream(FileInputStream fis) {
		this.fis = fis;
	}

	/** {@inheritDoc} */
    public void setOutStream(FileOutputStream fos) {
		this.fos = fos;
	}

	/** {@inheritDoc} */ // TODO need to fix
	public MetaData readMetaData(ByteBuffer buffer) {
		MetaData retMeta = new MetaData();
		Input input = new Input(buffer);
		String metaType = (String) deserializer.deserialize(input);
		Map m = (Map) deserializer.deserialize(input);
		retMeta.putAll(m);

		return retMeta;
	}

	/** {@inheritDoc} */
    public IMetaCue[] readMetaCue() {
		// TODO Auto-generated method stub
		return null;
	}

}
