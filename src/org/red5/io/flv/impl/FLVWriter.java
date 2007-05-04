package org.red5.io.flv.impl;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.amf.Output;
import org.red5.io.flv.IFLV;
import org.red5.io.object.Serializer;
import org.red5.io.utils.IOUtils;

/**
 * A Writer is used to write the contents of a FLV file
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class FLVWriter implements ITagWriter {
    /**
     * Logger
     */
	private static Log log = LogFactory.getLog(FLVWriter.class.getName());

    /**
     * File output stream
     */
    private FileOutputStream fos;

    /**
     * Writable byte channel (not concurrent)
     */
    private FileChannel channel;

	//private MappedByteBuffer mappedFile;

    /**
     * Output byte buffer
     */
    private ByteBuffer out;

    /**
     * Last tag
     */
    private ITag lastTag;

    /**
     * FLV object
     */
    private IFLV flv;

    /**
     * Number of bytes written
     */
    private long bytesWritten;

    /**
     * Position in file
     */
    private int offset;
    
    private boolean appendMode = false;

    /**
     * Position of onMetaData tag in file.
     */
	private long fileMetaPosition = 0;
	
	/**
	 * Size of tag containing onMetaData.
	 */
	private int fileMetaSize = 0;
	
	/**
	 * Id of the video codec used.
	 */
	private int videoCodecId = -1;
	
	/**
	 * Id of the audio codec used.
	 */
	private int audioCodecId = -1;

    /**
     * Creates new FLV writer from file output stream
     * @param fos
     */
    public FLVWriter(FileOutputStream fos) {
		this(fos, null);
	}

	/**
	 * Creates writer implementation with given file output stream and last tag
	 *
	 * @param fos               File output stream
     * @param lastTag           Last tag
	 */
	public FLVWriter(FileOutputStream fos, ITag lastTag) {
		this.fos = fos;
		if (lastTag != null) {
			offset = lastTag.getTimestamp();
			appendMode = true;
		}
		channel = this.fos.getChannel();
		out = ByteBuffer.allocate(1024);
		out.setAutoExpand(true);
	}

	/**
	 * Writes the header bytes
	 *
	 * @throws IOException      Any I/O exception
	 */
	public void writeHeader() throws IOException {

		out.put((byte) 0x46);
		out.put((byte) 0x4C);
		out.put((byte) 0x56);

		// Write version
		out.put((byte) 0x01);

		// For testing purposes write video only
		// TODO CHANGE
		// out.put((byte)0x08);
		// NOTE (luke): I looked at the docs on the wiki and it says it should
		// be 0x05 for audio and video
		// I think its safe to assume it will be both
		out.put((byte) 0x05);

		// Data Offset
		out.putInt(0x09);

		out.flip();

		channel.write(out.buf());

		// Write intermediate onMetaData tag, will be replaced later
		writeMetadataTag(0, -1, -1);
	}

	/** {@inheritDoc}
	 */
	public IStreamableFile getFile() {
		return flv;
	}

	/**
     * Setter for FLV object
     *
     * @param flv  FLV source
     *
	 */
	public void setFLV(IFLV flv) {
		this.flv = flv;
	}

	/** {@inheritDoc}
	 */
	public int getOffset() {
		return offset;
	}

	/**
     * Setter for offset
     *
     * @param offset Value to set for offset
     */
    public void setOffset(int offset) {
		this.offset = offset;
	}

	/** {@inheritDoc}
	 */
	public long getBytesWritten() {
		return bytesWritten;
	}

	/** {@inheritDoc}
	 */
	public boolean writeTag(ITag tag) throws IOException {
		// PreviousTagSize
		//out = out.reset();
		out.clear();

		// append mode and the first tag to write
		// the last tag size is already at the end of file
		if (!appendMode || lastTag != null) {
			out.putInt((lastTag == null) ? 0 : (lastTag.getBodySize() + 11));
		}

		// Data Type
		out.put(tag.getDataType());

		// Body Size
		IOUtils.writeMediumInt(out, tag.getBodySize());

		// Timestamp
		IOUtils.writeMediumInt(out, tag.getTimestamp() + offset);

		// Reserved
		out.putInt(0x00);

		out.flip();
		bytesWritten += channel.write(out.buf());

		ByteBuffer bodyBuf = tag.getBody();
		bytesWritten += channel.write(bodyBuf.buf());

		if (audioCodecId == -1 && tag.getDataType() == ITag.TYPE_AUDIO) {
			bodyBuf.flip();
			byte id = bodyBuf.get();
			audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
		} else if (videoCodecId == -1 && tag.getDataType() == ITag.TYPE_VIDEO) {
			bodyBuf.flip();
			byte id = bodyBuf.get();
			videoCodecId = id & ITag.MASK_VIDEO_CODEC;
		}
		
		lastTag = tag;

		return false;
	}

	/** {@inheritDoc}
	 */
	public boolean writeTag(byte type, ByteBuffer data) throws IOException {
		// TODO
		return false;
	}

	/** {@inheritDoc}
	 */
	public void close() {
		if (lastTag != null && !appendMode) {
			// TODO: update existing metadata when appending audio/video to adjust duration
			try {
				writeMetadataTag((lastTag.getTimestamp() + offset) * 0.001,
						videoCodecId != -1 ? videoCodecId : null,
						audioCodecId != -1 ? audioCodecId : null);
			} catch (IOException err) {
				log.error("Could not update onMetaData tag.", err);
			}
		}
		if (out != null) {
			// Write size of last tag to file before closing it.
			out.clear();
			out.putInt((lastTag == null) ? 0 : (lastTag.getBodySize() + 11));
			out.flip();
			try {
				bytesWritten += channel.write(out.buf());
			} catch (IOException err) {
				log.error("Could not write size of last tag to file.", err);
			}
			out.release();
			out = null;
		}
		try {
			channel.close();
			fos.close();
		} catch (IOException e) {
			log.error("FLVWriter :: close ::>\n", e);
		}

	}

	/** {@inheritDoc} */
    public boolean writeStream(byte[] b) {
		// TODO
		return false;
	}

    /**
     * Write "onMetaData" tag to the file.
     * 
     * @param duration			Duration to write in milliseconds.
     * @param videoCodecId		Id of the video codec used while recording.
     * @param audioCodecId		Id of the audio codec used while recording.
     * @throws IOException if the tag could not be written
     */
	private void writeMetadataTag(double duration, Integer videoCodecId, Integer audioCodecId) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("duration", duration);
		if (videoCodecId != null) {
			params.put("videocodecid", videoCodecId.intValue());
		}
		if (audioCodecId != null) {
			params.put("audiocodecid", audioCodecId.intValue());
		}
		params.put("canSeekToEnd", true);
		out.writeMap(params, new Serializer());
		buf.flip();
		
		if (fileMetaSize == 0) {
			fileMetaSize = buf.limit();
		}
		
		ITag onMetaData = new Tag(ITag.TYPE_METADATA, 0, fileMetaSize, buf, 0);
		if (fileMetaPosition == 0) {
			// Remember where we wrote the intermediate tag so we can update it later
			fileMetaPosition = channel.position();
			writeTag(onMetaData);
		} else {
			long old = channel.position();
			ITag last = lastTag;
			try {
				// We don't have a previous tag when writing metadata
				lastTag = null;
				channel.position(fileMetaPosition);
				writeTag(onMetaData);
			} finally {
				lastTag = last;
				channel.position(old);
			}
		}
	}
	

}
