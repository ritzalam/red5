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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FLVWriter implements ITagWriter {

	private static Logger log = LoggerFactory.getLogger(FLVWriter.class);

	/**
	 * Length of the flv header in bytes
	 */
	private final static int HEADER_LENGTH = 9;

	/**
	 * Length of the flv tag in bytes
	 */
	private final static int TAG_HEADER_LENGTH = 11;

	/**
	 * Position of the meta data tag in our file.
	 */
	private final static int META_POSITION = 13;

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
	 * need direct access to file to append full duration metadata
	 */
	private RandomAccessFile file;

	// size of the previous flv tag
	private volatile int previousTagSize = 0;

	/**
	 * Creates writer implementation with given file and last tag
	 *
	 * FLV.java uses this constructor so we have access to the file object
	 *
	 * @param file              File output stream
	 * @param append            true if append to existing file
	 */
	public FLVWriter(File file, boolean append) {
		log.debug("Writing to: {}", file.getAbsolutePath());
		try {
			this.file = new RandomAccessFile(file, "rws"); //rwd
			this.append = append;
			init();
		} catch (Exception e) {
			log.error("Failed to create FLV writer", e);
		}
	}

	/**
	 * Initialize the writer
	 */
	private void init() {
		if (!append) {
			try {
				// write the flv file type header
				writeHeader();
				// write onMetaData tag, it will be replaced when the file is closed
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
		ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4); // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
		flvHeader.write(header);
		// write header to output channel
		file.setLength(HEADER_LENGTH + 4);
		if (header.hasArray()) {
			log.debug("Header bytebuffer has a backing array");
			file.write(header.array());
		} else {
			log.debug("Header bytebuffer does not have a backing array");
			byte[] tmp = new byte[HEADER_LENGTH + 4];
			header.get(tmp);
			file.write(tmp);
		}
		bytesWritten = file.length();
		log.debug("Header size: {} bytes written: {}", (HEADER_LENGTH + 4), bytesWritten);
		header.clear();
		header = null;
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
		if (this.file != null) {
			try {
				log.debug("File was already set, current position: {}", this.file.getChannel().position());
				this.file.close();
			} catch (IOException e) {
				log.warn("Problem closing existing file", e);
			}
		}
		try {
			this.file = new RandomAccessFile(file, "rwd");
		} catch (FileNotFoundException e) {
			log.warn("File could not be set", e);
		}
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

	/** 
	 * {@inheritDoc}
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
		/*
		 * Tag header = 11 bytes
		 * |-|---|----|---|
		 *    0 = type
		 *  1-3 = data size
		 *  4-7 = timestamp
		 * 8-10 = stream id (always 0)
		 * Tag data = variable bytes
		 * Previous tag = 4 bytes (tag header size + tag data size)
		 */
		log.debug("writeTag - previous size: {}", previousTagSize);
		log.trace("Tag: {}", tag);
		long prevBytesWritten = bytesWritten;
		log.trace("Previous bytes written: {}", prevBytesWritten);
		// skip tags with no data
		int bodySize = tag.getBodySize();
		log.debug("Tag body size: {}", bodySize);
		if (bodySize > 0) {
			// ensure that the channel is still open
			if (file != null) {
				log.debug("Current file position: {}", file.getChannel().position());
				// get the data type
				byte dataType = tag.getDataType();
				// set a var holding the entire tag size including the previous tag length
				int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
				// resize
				file.setLength(file.length() + totalTagSize);
				// create a buffer for this tag
				ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
				// get the current file offset
				long fileOffset = file.getFilePointer();
				log.debug("Current file offset: {} expected offset: {}", fileOffset, prevBytesWritten);
				// get the timestamp
				int timestamp = tag.getTimestamp() + offset;
				// if we're writing non-meta tags do seeking and tag size update
				if (dataType != ITag.TYPE_METADATA) {
					if (fileOffset < prevBytesWritten && dataType != ITag.TYPE_METADATA) {
						log.debug("Seeking to expected offset");
						// it's necessary to seek to the length of the file
						// so that we can append new tags
						file.seek(prevBytesWritten);
						log.debug("New file position: {}", file.getChannel().position());
					}
				} else {
					// onMetaData must be ts == 0
					timestamp = 0;
				}
				// create an array big enough
				byte[] bodyBuf = new byte[bodySize];
				// put the bytes into the array
				tag.getBody().get(bodyBuf);
				// get the audio or video codec identifier
				if (dataType == ITag.TYPE_AUDIO && audioCodecId == -1) {
					int id = bodyBuf[0] & 0xff; // must be unsigned
					audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
					log.debug("Audio codec id: {}", audioCodecId);
				} else if (dataType == ITag.TYPE_VIDEO && videoCodecId == -1) {
					int id = bodyBuf[0] & 0xff; // must be unsigned
					videoCodecId = id & ITag.MASK_VIDEO_CODEC;
					log.debug("Video codec id: {}", videoCodecId);
				}
				// Data Type
				tagBuffer.put(dataType); //1
				// Body Size - Length of the message. Number of bytes after StreamID to end of tag 
				// (Equal to length of the tag - 11) 
				IOUtils.writeMediumInt(tagBuffer, bodySize); //3
				// Timestamp
				//IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
				IOUtils.writeMediumInt(tagBuffer, timestamp); //3
				//tagBuffer.putShort((short) (timestamp >>> 8)); //3
				tagBuffer.put((byte) (0x00 & 0xff)); //1
				// Stream id
				IOUtils.writeMediumInt(tagBuffer, 0); //3
				log.trace("Tag buffer (after tag header) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
				// get the body
				tagBuffer.put(bodyBuf);
				log.trace("Tag buffer (after body) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
				// update previous tag size
				previousTagSize = TAG_HEADER_LENGTH + bodySize;
				// we add the tag size
				tagBuffer.putInt(previousTagSize & 0xff);
				log.trace("Tag buffer (after prev tag size) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
				// flip so we can process from the beginning
				tagBuffer.flip();
				if (log.isDebugEnabled()) {
					//StringBuilder sb = new StringBuilder();
					//HexDump.dumpHex(sb, tagBuffer.array());
					//log.debug("\n{}", sb);
				}
				// write the tag
				file.write(tagBuffer.array());
				bytesWritten = file.length();
				log.trace("Tag written, check value: {} (should be 0)", (bytesWritten - prevBytesWritten) - totalTagSize);
				tagBuffer.clear();
				// update the duration
				duration = Math.max(duration, timestamp);
				log.debug("Writer duration: {}", duration);
				// validate written amount
				if ((bytesWritten - prevBytesWritten) != totalTagSize) {
					log.debug("Not all of the bytes appear to have been written, prev-current: {}", (bytesWritten - prevBytesWritten));
				}
			} else {
				// throw an exception and let them know the cause
				throw new IOException("FLV write channel has been closed", new ClosedChannelException());
			}
		} else {
			log.debug("Empty tag skipped: {}", tag);
			return false;
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
			// keep track of where the pointer is before we update the header and meta
			long tail = bytesWritten;
			// set to where the flv header goes
			file.seek(0);
			//Header fields (in same order than spec, for comparison purposes)
			FLVHeader flvHeader = new FLVHeader();
			flvHeader.setFlagAudio(audioCodecId != -1 ? true : false);
			flvHeader.setFlagVideo(videoCodecId != -1 ? true : false);
			// create a buffer
			ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4);
			flvHeader.write(header);
			// write header to output channel
			file.write(header.array());
			header.clear();
			// here we overwrite the metadata with the final duration
			// get the file offset
			long fileOffset = file.getFilePointer();
			log.debug("Current file offset: {} expected offset: {}", fileOffset, META_POSITION);
			if (fileOffset < META_POSITION) {
				file.seek(META_POSITION);
				fileOffset = file.getFilePointer();
				log.debug("Updated file offset: {} expected offset: {}", fileOffset, META_POSITION);
			}
			log.debug("In the metadata writing (close) method - duration:{}", duration);
			writeMetadataTag(duration * 0.001, videoCodecId, audioCodecId);
			// seek to the end of the data
			file.seek(tail);
		} catch (IOException e) {
			log.error("IO error on close", e);
		} finally {
			try {
				if (file != null) {
					// run a test on the flv if debugging is on
					if (log.isDebugEnabled()) {
						// debugging
						//testFLV();
					}
					// close the file
					file.close();
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
	private void writeMetadataTag(double duration, int videoCodecId, int audioCodecId) throws IOException {
		log.debug("writeMetadataTag - duration: {} video codec: {} audio codec: {}", new Object[] { duration, videoCodecId, audioCodecId });
		IoBuffer buf = IoBuffer.allocate(192);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("server", Red5.getVersion().replaceAll("\\$", "").trim());
		params.put("creationdate", GregorianCalendar.getInstance().getTime().toString());
		params.put("duration", duration);
		if (videoCodecId != -1) {
			params.put("videocodecid", videoCodecId);
		} else {
			// place holder
			params.put("novideocodec", 0);
		}
		if (audioCodecId != -1) {
			params.put("audiocodecid", audioCodecId);
		} else {
			// place holder
			params.put("noaudiocodec", 0);
		}
		params.put("canSeekToEnd", true);
		out.writeMap(params, new Serializer());
		buf.flip();
		if (fileMetaSize == 0) {
			fileMetaSize = buf.limit();
		}
		log.debug("Metadata size: {}", fileMetaSize);
		ITag onMetaData = new Tag(ITag.TYPE_METADATA, 0, fileMetaSize, buf, 0);
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
				file.seek(0);
				reader = new FLVReader(file.getChannel());
				//				RandomAccessFile raf = new RandomAccessFile("E:/dev/red5/java/server/trunk/distx/webapps/oflaDemo/streams/toystory3.flv", "r");
				//				reader = new FLVReader(raf.getChannel());
			}
			log.trace("reader: {}", reader);
			log.debug("Has more tags: {}", reader.hasMoreTags());
			ITag tag = null;
			while (reader.hasMoreTags()) {
				tag = reader.readTag();
				log.debug("\n{}", tag);
			}
			//Assert.assertEquals(true, true);
		} catch (IOException e) {
			log.warn("", e);
		}
	}

}
