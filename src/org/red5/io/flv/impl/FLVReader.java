package org.red5.io.flv.impl;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.FLVHeader;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.utils.IOUtils;

/**
 * A Reader is used to read the contents of a FLV file
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class FLVReader implements IoConstants, ITagReader,
		IKeyFrameDataAnalyzer {

	private static Log log = LogFactory.getLog(FLVReader.class.getName());

	private FileInputStream fis;

	private FileChannel channel;

	private MappedByteBuffer mappedFile;

	private KeyFrameMeta keyframeMeta;

	private ByteBuffer in;

	/** Set to true to generate metadata automatically before the first tag. */
	private boolean generateMetadata;

	/** Position of first video tag. */
	private int firstVideoTag = -1;

	/** Position of first audio tag. */
	private int firstAudioTag = -1;

	/** Current tag. */
	private int tagPosition;

	/** Duration in milliseconds. */
	private long duration;

	/** Mapping between file position and timestamp in ms. */
	private HashMap<Long, Long> posTimeMap;

	/** Mapping between file position and tag number. */
	private HashMap<Long, Integer> posTagMap;

	/** Buffer type / style to use **/
	private static String bufferType = "auto"; //Default
	
	private static int maxBufferSize = 0;
	private static volatile int bufferUsed = 0;
	
	private boolean useBuffer = false;
	
	FLVReader() {}
	
	public FLVReader(FileInputStream f) {
		this(f, false);
	}

	public FLVReader(FileInputStream f, boolean generateMetadata) {
		this.fis = f;
		this.generateMetadata = generateMetadata;
		channel = fis.getChannel();
		try {
			mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel
					.size());
			mappedFile.order(ByteOrder.BIG_ENDIAN);
			if (log.isDebugEnabled()) {
				log.debug("Mapped file capacity: " + mappedFile.capacity() + " Channel size: " + channel.size());
			}
			int bufferTypeHash = bufferType.hashCode();
			if (bufferTypeHash == 3198444 ||
					bufferTypeHash == -1331586071 ||
					bufferTypeHash == 3005871) {
				synchronized (FLVReader.class) {
					if (bufferUsed + mappedFile.capacity() <= maxBufferSize) {
						bufferUsed += mappedFile.capacity();
						useBuffer = true;
					}
				}
			}
			in = null;
			if (useBuffer) {
				switch (bufferTypeHash) {
					case 3198444: //heap
						//Get a heap buffer from buffer pool
						in = ByteBuffer.allocate(mappedFile.capacity(), false);
						break;
					case -1331586071: //direct
						//Get a direct buffer from buffer pool
						in = ByteBuffer.allocate(mappedFile.capacity(), true);
						break;
					case 3005871: //auto
						//Let MINA choose
						in = ByteBuffer.allocate(mappedFile.capacity());
						break;
					default:
						break;
				}
			}
			if (in != null) {
				//drop in the file
				in.put(mappedFile);
				in.flip();
			} else {
				in = ByteBuffer.wrap(mappedFile);
			}
			if (log.isDebugEnabled()) {
				log.debug("Direct buffer: " + in.isDirect() + " Read only: " + in.isReadOnly() + " Pooled: " + in.isPooled());
			}
		} catch (IOException e) {
			log.error("FLVReader :: FLVReader ::>\n", e);
		} catch (Exception e) {
			log.error("FLVReader :: FLVReader ::>\n", e);
		}
		if (log.isDebugEnabled()) {
			log.debug("FLVReader 1 - Buffer size: " + in.capacity() + " position: "
				+ in.position() + " remaining: " + in.remaining());
		}
		if (in.remaining() >= 9) {
			decodeHeader();
		}
		keyframeMeta = analyzeKeyFrames();
	}

	/**
	 * Accepts mapped file bytes to construct internal members.
	 * 
	 * @param mappedFile
	 * @param generateMetadata
	 */
	public FLVReader(ByteBuffer buffer, boolean generateMetadata) {
		this.generateMetadata = generateMetadata;
		in = buffer;
		if (log.isDebugEnabled()) {
			log.debug("FLVReader 2 - Buffer size: " + in.capacity() + " position: "
				+ in.position() + " remaining: " + in.remaining());
		}
		if (in.remaining() >= 9) {
			decodeHeader();
		}
		keyframeMeta = analyzeKeyFrames();
	}

	public static String getBufferType() {
		return bufferType;
	}

	public static void setBufferType(String bufferType) {
		FLVReader.bufferType = bufferType;
	}
	
	public static int getMaxBufferSize() {
		return maxBufferSize;
	}
	
	public static void setMaxBufferSize(int maxBufferSize) {
		FLVReader.maxBufferSize = maxBufferSize;
	}

	/**
	 * Returns the file buffer.
	 * 
	 * @return file bytes
	 */
	public ByteBuffer getFileData() {
		return in.asReadOnlyBuffer();
	}

	public void decodeHeader() {
		// XXX check signature?
		// SIGNATURE, lets just skip
		FLVHeader header = new FLVHeader();
		in.skip(3);
		header.setVersion(in.get());
		header.setTypeFlags(in.get());
		header.setDataOffset(in.getInt());
		if (log.isDebugEnabled()) {
			log.debug("Header: " + header.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#getFLV()
	 */
	public IStreamableFile getFile() {
		// TODO wondering if we need to have a reference
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#getOffset()
	 */
	public int getOffset() {
		//return header.getDataOffset();
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#getBytesRead()
	 */
	synchronized public long getBytesRead() {
		return in.position();
	}

	synchronized public long getDuration() {
		return duration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#hasMoreTags()
	 */
	synchronized public boolean hasMoreTags() {
		return in.remaining() > 4;
	}

	private ITag createFileMeta() {
		// Create tag for onMetaData event
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		out.writeStartMap(3);
		out.writePropertyName("duration");
		out.writeNumber(duration / 1000.0);
		if (firstVideoTag != -1) {
			int old = in.position();
			in.position(firstVideoTag);
			readTagHeader();
			byte frametype = in.get();
			out.writePropertyName("videocodecid");
			out.writeNumber(frametype & MASK_VIDEO_CODEC);
			in.position(old);
		}
		if (firstAudioTag != -1) {
			int old = in.position();
			in.position(firstAudioTag);
			readTagHeader();
			byte frametype = in.get();
			out.writePropertyName("audiocodecid");
			out.writeNumber((frametype & MASK_SOUND_FORMAT) >> 4);
			in.position(old);
		}
		out.writePropertyName("canSeekToEnd");
		out.writeBoolean(true);
		out.markEndMap();
		buf.flip();

		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null,
				0);
		result.setBody(buf);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#readTag()
	 */
	synchronized public ITag readTag() {
		int oldPos = in.position();
		ITag tag = readTagHeader();

		if (tagPosition == 0 && tag.getDataType() != TYPE_METADATA
				&& generateMetadata) {
			// Generate initial metadata automatically
			in.position(oldPos);
			KeyFrameMeta meta = analyzeKeyFrames();
			tagPosition++;
			if (meta != null) {
				return createFileMeta();
			}
		}

		ByteBuffer body = ByteBuffer.allocate(tag.getBodySize());
		final int limit = in.limit();
		// XXX Paul: this assists in 'properly' handling damaged FLV files		
		int newPosition = in.position() + tag.getBodySize();
		if (newPosition <= limit) {
			in.limit(newPosition);
			body.put(in);
			body.flip();
			in.limit(limit);

			tag.setBody(body);
			tagPosition++;
		}
		return tag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#close()
	 */
	synchronized public void close() {
		log.debug("Reader close");
		if (in != null) {
			if (useBuffer) {
				int bufferTypeHash = bufferType.hashCode();
				if (bufferTypeHash == 3198444 ||
						bufferTypeHash == -1331586071 ||
						bufferTypeHash == 3005871) {
					synchronized (FLVReader.class) {
						if (mappedFile != null) {
							bufferUsed -= mappedFile.capacity();
						}
					}
				}
			}
			in.release();
			in = null;
		}
		if (mappedFile != null) {
			mappedFile.clear();
			mappedFile = null;
		}
		if (channel != null) {
			try {
				channel.close();
				fis.close();
			} catch (IOException e) {
				log.error("FLVReader :: close ::>\n", e);
			}
		}
	}

	synchronized public KeyFrameMeta analyzeKeyFrames() {
		if (keyframeMeta != null) {
			return keyframeMeta;
		}

		List<Integer> positionList = new ArrayList<Integer>();
		List<Integer> timestampList = new ArrayList<Integer>();
		int origPos = in.position();
		// point to the first tag
		in.position(9);
		posTagMap = new HashMap<Long, Integer>();
		int idx = 0;
		while (this.hasMoreTags()) {
			int pos = in.position();
			posTagMap.put((long) pos, idx++);
			ITag tmpTag = this.readTagHeader();
			duration = tmpTag.getTimestamp();
			if (tmpTag.getDataType() == IoConstants.TYPE_VIDEO) {
				if (firstVideoTag == -1) {
					firstVideoTag = pos;
				}

				// Grab Frame type
				byte frametype = in.get();
				if (((frametype & MASK_VIDEO_FRAMETYPE) >> 4) == FLAG_FRAMETYPE_KEYFRAME) {
					positionList.add(pos);
					timestampList.add(tmpTag.getTimestamp());
				}

			} else if (tmpTag.getDataType() == IoConstants.TYPE_AUDIO) {
				if (firstAudioTag == -1) {
					firstAudioTag = pos;
				}
			}
			// XXX Paul: this 'properly' handles damaged FLV files - as far as
			// duration/size is concerned
			int newPosition = (pos + tmpTag.getBodySize() + 15);
			// log.debug("---->" + in.remaining() + " limit=" + in.limit() + "
			// new pos=" + newPosition);
			if (newPosition >= in.limit()) {
				log.info("New position exceeds limit");
				if (log.isDebugEnabled()) {
					log.debug("-----");
					log.debug("Keyframe analysis");
					log.debug(" data type=" + tmpTag.getDataType()
							+ " bodysize=" + tmpTag.getBodySize());
					log.debug(" remaining=" + in.remaining() + " limit="
							+ in.limit() + " new pos=" + newPosition);
					log.debug(" pos=" + pos);
					log.debug("-----");
				}
				break;
			} else {
				in.position(newPosition);
			}
		}
		// restore the pos
		in.position(origPos);

		keyframeMeta = new KeyFrameMeta();
		posTimeMap = new HashMap<Long, Long>();
		keyframeMeta.positions = new int[positionList.size()];
		keyframeMeta.timestamps = new int[timestampList.size()];
		for (int i = 0; i < keyframeMeta.positions.length; i++) {
			keyframeMeta.positions[i] = positionList.get(i);
			keyframeMeta.timestamps[i] = timestampList.get(i);
			posTimeMap.put((long) positionList.get(i), (long) timestampList
					.get(i));
		}
		return keyframeMeta;
	}

	synchronized public void position(long pos) {
		// FIXME what if file size is larger than 2G?
		// TODO: adjust position to point to nearest keyframe
		in.position((int) pos);
		// Make sure we have informations about the keyframes.
		analyzeKeyFrames();
		// Update the current tag number
		Integer tag = posTagMap.get(pos);
		if (tag == null) {
			return;
		}

		tagPosition = tag;
	}

	/**
	 * Read only header part of a tag
	 *
	 * @return
	 */
	private ITag readTagHeader() {
		// PREVIOUS TAG SIZE
		int previousTagSize = in.getInt();

		// START OF FLV TAG
		byte dataType = in.get();

		// The next two lines use a utility method which reads in
		// three consecutive bytes but stores them in a 4 byte int.
		// We are able to write those three bytes back out by using
		// another utility method which strips off the last byte
		// However, we will have to check into this during optimization
		int bodySize = IOUtils.readUnsignedMediumInt(in);
		int timestamp = IOUtils.readUnsignedMediumInt(in);
		// reserved
		in.getInt();

		return new Tag(dataType, timestamp, bodySize, null, previousTagSize);
	}
}
