package org.red5.io.flv.impl;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.amf.Output;
import org.red5.io.flv.FLVHeader;
import org.red5.io.flv.IFLV;
import org.red5.io.object.Serializer;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Writer is used to write the contents of a FLV file
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Tiago Jacobs (tiago@imdt.com.br)
 */
public class FLVWriter implements ITagWriter {
	/**
	 * Logger
	 */
	private static Logger log = LoggerFactory.getLogger(FLVWriter.class);

	/**
	 * File output stream
	 */
	private FileOutputStream fos;

	/**
	 * Writable byte channel (not concurrent)
	 */
	private FileChannel channel;

	/**
	 * FLV object
	 */
	private IFLV flv;

	/**
	 * Number of bytes written
	 */
	private volatile long bytesWritten;

	/**
	 * Position in file
	 */
	private int offset;

	/**
	 * Size of tag containing onMetaData.
	 */
	private int fileMetaSize = 0;

	/**
	 * Id of the video codec used.
	 */
	private volatile int videoCodecId = -1;

	/**
	 * Id of the audio codec used.
	 */
	private volatile int audioCodecId = -1;

	/**
	 * Are we appending to an existing file?
	 */
	private boolean append;

	/**
	 * Duration of the file.
	 */
	private int duration;

	/**
	 * Position of the meta data tag in our file.
	 */
	private volatile long metaPosition;

	/**
	 * need direct access to file to append full duration metadata
	 */
	private File file;

	// create a buffer for header bytes
	private final ByteBuffer headerBuf = ByteBuffer.allocate(11);

	// create a buffer for trailer bytes (tag size)
	private final ByteBuffer trailerBuf = ByteBuffer.allocate(4);

	private volatile int previousTagSize = 0;

	/**
	 * Creates writer implementation with given file output stream and last tag
	 *
	 * @param fos               File output stream
	 * @param append            true if append to existing file
	 */
	public FLVWriter(FileOutputStream fos, boolean append) {
		this.fos = fos;
		this.append = append;
		init();
	}

	/**
	 * Creates writer implementation with given file and last tag
	 *
	 * FLV.java uses this constructor so we have access to the file object
	 *
	 * @param file              File output stream
	 * @param append            true if append to existing file
	 */
	public FLVWriter(File file, boolean append) {
		this.file = file;
		this.append = append;
		try {
			this.fos = new FileOutputStream(file, append);
			init();
		} catch (Exception e) {
			log.error("Failed to create FLV writer", e);
		}
	}

	/**
	 * Initialize the writer
	 */
	private void init() {
		channel = this.fos.getChannel();
		if (!append) {
			try {
				// write the flv file type header
				writeHeader();
				// write intermediate onMetaData tag, will be replaced later
				writeMetadataTag(0, videoCodecId, audioCodecId);
			} catch (IOException e) {
				log.warn("Exception writing header or intermediate meta data", e);
			}
		}
	}

	/**
	 * Writes the header bytes
	 *
	 * @throws IOException      Any I/O exception
	 */
	public void writeHeader() throws IOException {
		FLVHeader flvHeader = new FLVHeader();
		flvHeader.setFlagAudio(true);
		flvHeader.setFlagVideo(true);
		// create a buffer
		ByteBuffer header = ByteBuffer.allocate(13); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
		flvHeader.write(header);
		// write header to output channel
		channel.write(header);
		channel.force(false);
		header.clear();
	}

	/** {@inheritDoc}
	 */
	public IStreamableFile getFile() {
		return flv;
	}
	
	/**
	 * Sets the base file.
	 * 
	 * @param file source flv
	 */
	public void setFile(File file) {
		this.file = file;
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

	/** 
	 * {@inheritDoc}
	 */
	public long getBytesWritten() {
		return bytesWritten;
	}

	/** 
	 * {@inheritDoc}
	 */
	public synchronized boolean writeTag(ITag tag) throws IOException {
		log.debug("writeTag - previous size: {}", previousTagSize);
		log.trace("Tag: {}", tag);
		// skip tags with no data
		int bodySize = tag.getBodySize();
		log.debug("Tag body size: {}", bodySize);
		if (bodySize == 0) {
			log.debug("Empty tag skipped: {}", tag);
			return false;
		}
		// ensure that the channel is still open
		if (channel.isOpen()) {
			byte dataType = tag.getDataType();
			// dont reset previous tag size on metadata
			if (previousTagSize != tag.getPreviousTagSize() && dataType != ITag.TYPE_METADATA) {
				log.debug("Previous tag size: {} previous per tag: {}", previousTagSize, tag.getPreviousTagSize());
				tag.setPreviousTagSize(previousTagSize);
			}
			int timestamp = tag.getTimestamp() + offset;
			// get the body
			ByteBuffer bodyBuf = tag.getBody().buf();
			if (dataType == ITag.TYPE_AUDIO && audioCodecId == -1) {
				int id = bodyBuf.get() & 0xff; // must be unsigned
				audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
				log.debug("Audio codec id: {}", audioCodecId);
			} else if (dataType == ITag.TYPE_VIDEO && videoCodecId == -1) {
				int id = bodyBuf.get() & 0xff; // must be unsigned
				videoCodecId = id & ITag.MASK_VIDEO_CODEC;
				log.debug("Video codec id: {}", videoCodecId);
			}
			// Data Type
			headerBuf.put(dataType); //1
			// Body Size - Length of the message. Number of bytes after StreamID to end of tag (Equal to length of the tag – 11) 
			IOUtils.writeMediumInt(headerBuf, bodySize); //3
			// Timestamp
			IOUtils.writeExtendedMediumInt(headerBuf, timestamp); //4
			// Stream id
			IOUtils.writeMediumInt(headerBuf, 0); //3
			headerBuf.flip();
			// write the header
			bytesWritten += channel.write(headerBuf);
			channel.force(false); //flush
			log.debug("Bytes written (header): {}", bytesWritten);
			headerBuf.clear();
			// write the body
			if (bodyBuf.position() > 0) {
				bodyBuf.flip();
			}
			bytesWritten += channel.write(bodyBuf);
			channel.force(false); //flush
			log.debug("Bytes written (body): {}", bytesWritten);
			// we add the tag size
			trailerBuf.putInt(bodySize + 11);
			trailerBuf.flip();
			// write the size
			bytesWritten += channel.write(trailerBuf);
			channel.force(false); //flush
			log.debug("Bytes written (tag size): {}", bytesWritten);
			trailerBuf.clear();
			// update the duration
			duration = Math.max(duration, timestamp);
			log.debug("Writer duration: {}", duration);
			// update previous tag size
			previousTagSize = bodySize + 11;
		} else {
			// throw an exception and let them know the cause
			throw new IOException("FLV write channel has been closed and cannot be written to", new ClosedChannelException());
		}
		return true;
	}

	/** {@inheritDoc}
	 */
	public boolean writeTag(byte type, IoBuffer data) throws IOException {
		return false;
	}

	/** {@inheritDoc}
	 */
	public void close() {
		log.debug("close");
		try {
			// keep track of the original position
			long oldPos = channel.position();
			// set to where the flv header goes
			channel.position(0);
			//Header fields (in same order than spec, for comparison purposes)
			FLVHeader flvHeader = new FLVHeader();
			flvHeader.setFlagAudio(audioCodecId != -1 ? true : false);
			flvHeader.setFlagVideo(videoCodecId != -1 ? true : false);
			// create a buffer
			ByteBuffer header = ByteBuffer.allocate(13);
			flvHeader.write(header);
			// write header to output channel
			channel.write(header);
			channel.force(false);
			header.clear();
			// set it back to the original pos
			channel.position(oldPos);
			if (metaPosition > 0) {
				try {
					log.debug("In the metadata writing (close) method - duration:{}", duration);
					channel.position(metaPosition);
					writeMetadataTag(duration * 0.001, videoCodecId, audioCodecId);
				} finally {
					channel.position(oldPos);
				}
			}
			// here we open the file again and overwrite the metadata with the final duration
			// I tried just writing to the existing fos but for some reason it doesn't overwrite in the right place....
			if (append) {
				// close what we were using, so we can append
				channel.close();
				fos.close();
				channel = null;
				// create the appender
				RandomAccessFile appender = new RandomAccessFile(file, "rw");
				channel = appender.getChannel(); // reuse member variable to make sure writeMetadataTag() works
				channel.position(13); // This is the position of the first tag
				writeMetadataTag(duration * 0.001, videoCodecId, audioCodecId);
				if (appender != null) {
					appender.close();
				}
			}
		} catch (IOException e) {
			log.error("IO error on close", e);
		} finally {
			try {
				if (channel != null) {
					// flush
					channel.force(true);
					// run a test on the flv if debugging is on
					if (log.isDebugEnabled()) {
    					// debugging
    					testFLV();
					}
					// close the channel
					channel.close();
					if (fos != null) {
						fos.close();
					}
				}
			} catch (IOException e) {
				log.error("", e);
			}
		}
	}

	/** {@inheritDoc} */
	public boolean writeStream(byte[] b) {
		// TODO implement writing byte stream
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
		log.debug("writeMetadataTag - duration: {} video codec: {} audio codec: {}", new Object[] { duration, videoCodecId, audioCodecId });
		metaPosition = channel.position();
		log.debug("Metadata position: {}", metaPosition);
		IoBuffer buf = IoBuffer.allocate(192);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> params = new HashMap<Object, Object>(4);
		params.put("server", Red5.getVersion());
		params.put("creationdate", GregorianCalendar.getInstance().getTime().toString());
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
		log.debug("Metadata size: {}", fileMetaSize);
		ITag onMetaData = new Tag(ITag.TYPE_METADATA, 0, fileMetaSize, buf, 9);
		writeTag(onMetaData);
	}

	public void testFLV() {
		log.debug("testFLV");
		try {
			ITagReader reader = null;
			if (flv != null) {
				reader = flv.getReader();
			}
			if (reader == null) {
				FileInputStream fis = new FileInputStream(file);
				reader = new FLVReader(fis.getChannel());				
			}
			log.debug("reader: {}", reader);
			ITag tag = null;
			log.debug("has more tags: {}", reader.hasMoreTags());
			while (reader.hasMoreTags()) {
				tag = reader.readTag();
				log.debug("{}", tag);
			}
			//Assert.assertEquals(true, true);
		} catch (IOException e) {
			log.warn("", e);
		}
	}

}
