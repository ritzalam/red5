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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A Reader is used to read the contents of a FLV file.
 * NOTE: This class is not implemented as threading-safe. The caller
 * should make sure the threading-safety.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class FLVReader implements IoConstants, ITagReader,
		IKeyFrameDataAnalyzer {

    /**
     * Logger
     */
    private static Log log = LogFactory.getLog(FLVReader.class.getName());

    /**
     * File input stream
     */
    private FileInputStream fis;

    /**
     * File channel
     */
    private FileChannel channel;
    /**
     * Keyframe metadata
     */
	private KeyFrameMeta keyframeMeta;

    /**
     * Input byte buffer
     */
    private ByteBuffer in;

	/** Set to true to generate metadata automatically before the first tag. */
	private boolean generateMetadata;

	/** Position of first video tag. */
	private long firstVideoTag = -1;

	/** Position of first audio tag. */
	private long firstAudioTag = -1;

	/** Current tag. */
	private int tagPosition;

	/** Duration in milliseconds. */
	private long duration;

	/** Mapping between file position and timestamp in ms. */
	private HashMap<Long, Long> posTimeMap;

	/** Mapping between file position and tag number. */
	private HashMap<Long, Integer> posTagMap;

	/** Buffer type / style to use **/
	private static BufferType bufferType = BufferType.AUTO;

	private static int bufferSize = 1024;
	
	/** Use load buffer */
	private boolean useLoadBuf = false;

	/** Constructs a new FLVReader. */
    FLVReader() {
	}

    /**
     * Creates FLV reader from file input stream
     * @param f         File input stream
     */
    public FLVReader(FileInputStream f) {
		this(f, false);
	}

    /**
     * Creates FLV reader from file input stream, sets up metadata generation flag
     * @param f                    File input stream
     * @param generateMetadata     <code>true</code> if metadata generation required, <code>false</code> otherwise
     */
    public FLVReader(FileInputStream f, boolean generateMetadata) {
		this.fis = f;
		this.generateMetadata = generateMetadata;
		channel = fis.getChannel();
		
		in = null;
		fillBuffer();

		postInitialize();
	}


    /**
	 * Accepts mapped file bytes to construct internal members.
	 *
	 * @param generateMetadata         <code>true</code> if metadata generation required, <code>false</code> otherwise
     * @param buffer                   Byte buffer
	 */
	public FLVReader(ByteBuffer buffer, boolean generateMetadata) {
		this.generateMetadata = generateMetadata;
		in = buffer;

		postInitialize();
	}
    
    /**
	 * Get the remaining bytes that could be read from a file or ByteBuffer
	 * @return
	 */
	private long getRemainingBytes() {
		if (!useLoadBuf) {
			return in.remaining();
		}

		try {
			return channel.size() - channel.position() + in.remaining();
		} catch (Exception e) {
			log.error("Error getRemainingBytes", e);
			return 0;
		}
	}

	/**
	 * Get the total readable bytes in a file or ByteBuffer
	 * @return          Total readable bytes
	 */
	private long getTotalBytes() {
		if (!useLoadBuf) {
			return in.capacity();
		}

		try {
			return channel.size();
		} catch (Exception e) {
			log.error("Error getTotalBytes", e);
			return 0;
		}
	}

	/**
	 * Get the current position in a file or ByteBuffer
	 * @return           Current position in a file
	 */
	private long getCurrentPosition() {
		long pos = 0;

		if (!useLoadBuf) {
			return in.position();
		}

		try {
			if (in != null) {
				pos = (channel.position() - in.remaining());
			} else {
				pos = channel.position();
			}
			return pos;
		} catch (Exception e) {
			log.error("Error getCurrentPosition", e);
			return 0;
		}
	}

	/**
     * Modifies current position
     *
     * @param pos  Current position in file
     */
    private void setCurrentPosition(long pos) {
		if (!useLoadBuf) {
			in.position((int) pos);
			return;
		}

		try {
			if (pos >= (channel.position() - in.limit())
					&& pos < channel.position()) {
				in.position((int) (pos - (channel.position() - in.limit())));
			} else {
				channel.position(pos);
				fillBuffer(bufferSize, true);
			}
		} catch (Exception e) {
			log.error("Error setCurrentPosition", e);
		}

	}

    /**
     * Loads whole buffer from file channel, with no reloading (that is, appending)
     */
    private void fillBuffer() {
		fillBuffer(bufferSize, false);
	}

	/**
	 * Loads data from channel to buffer
	 * @param amount         Amount of data to load with no reloading
	 */
	private void fillBuffer(long amount) {
		fillBuffer(amount, false);
	}

	/**
	 * Load enough bytes from channel to buffer.
	 * After the loading process, the caller can make sure the amount
	 * in buffer is of size 'amount' if we haven't reached the end of channel.
	 * @param amount The amount of bytes in buffer after returning,
	 * no larger than bufferSize
	 * @param reload Whether to reload or append.
	 */
	private void fillBuffer(long amount, boolean reload) {
		try {
			if (amount > bufferSize) {
				amount = bufferSize;
			}
			// Read all remaining bytes if the requested amount reach the end
			// of channel.
			if (channel.size() - channel.position() < amount) {
				amount = channel.size() - channel.position();
			}

			if (in == null) {
				switch (bufferType) {
					case HEAP:
						in = ByteBuffer.allocate(bufferSize, false);
						break;
					case DIRECT:
						in = ByteBuffer.allocate(bufferSize, true);
						break;
					case AUTO:
						in = ByteBuffer.allocate(bufferSize);
						break;
					default:
						in = ByteBuffer.allocate(bufferSize);
				}
				channel.read(in.buf());
				in.flip();
				useLoadBuf = true;
			}

			if (!useLoadBuf) {
				return;
			}

			if (reload || in.remaining() < amount) {
				long toRead = amount;
				if (!reload) {
					toRead = (bufferSize - in.remaining());
					in.compact();
				} else {
					in.clear();
				}

				java.nio.ByteBuffer tmpbuf = java.nio.ByteBuffer.allocate((int) toRead);
				channel.read(tmpbuf);
				tmpbuf.flip();
				in.put(tmpbuf);
				in.flip();
                // Let GC sweep it
                tmpbuf = null;
			}

		} catch (Exception e) {
			log.error("Error fillBuffer", e);
		}
	}

    /**
     * Post-initialization hook, reads keyframe metadata and decodes header (if any)
     */
    private void postInitialize() {
		if (log.isDebugEnabled()) {
			log.debug("FLVReader 1 - Buffer size: " + getTotalBytes()
					+ " position: " + getCurrentPosition() + " remaining: "
					+ getRemainingBytes());
		}
		if (getRemainingBytes() >= 9) {
			decodeHeader();
		}
		keyframeMeta = analyzeKeyFrames();
	}
	




	/**
     * Getter for buffer type (auto, direct or heap)
     *
     * @return Value for property 'bufferType'.
     */
    public static String getBufferType() {
		switch (bufferType) {
			case AUTO:
				return "auto";
			case DIRECT:
				return "direct";
			case HEAP:
				return "heap";
			default:
				return null;
		}
	}

	/**
     * Setter for buffer type
     *
     * @param bufferType Value to set for property 'bufferType'.
     */
    public static void setBufferType(String bufferType) {
		int bufferTypeHash = bufferType.hashCode();
		 switch (bufferTypeHash) {
			 case 3198444: //heap
				 //Get a heap buffer from buffer pool
				 FLVReader.bufferType = BufferType.HEAP;
				 break;
			 case -1331586071: //direct
				 //Get a direct buffer from buffer pool
				 FLVReader.bufferType = BufferType.DIRECT;
				 break;
			 case 3005871: //auto
				 //Let MINA choose
				 FLVReader.bufferType = BufferType.AUTO;
				 break;
			 default:
				 FLVReader.bufferType = BufferType.AUTO;
			 }
	}

	/**
     * Getter for buffer size
     *
     * @return Value for property 'bufferSize'.
     */
    public static int getBufferSize() {
		return bufferSize;
	}

	/**
     * Setter for property 'bufferSize'.
     *
     * @param bufferSize Value to set for property 'bufferSize'.
     */
    public static void setBufferSize(int bufferSize) {
		// make sure buffer size is no less than 1024 bytes.
		if (bufferSize < 1024) {
			bufferSize = 1024;
		}
		FLVReader.bufferSize = bufferSize;
	}

	/**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
	public ByteBuffer getFileData() {
		// TODO as of now, return null will disable cache
		// we need to redesign the cache architecture so that
		// the cache is layed underneath FLVReader not above it,
		// thus both tag cache and file cache are feasible.
		return null;
	}

	/** {@inheritDoc} */
    public void decodeHeader() {
		// XXX check signature?
		// SIGNATURE, lets just skip
		fillBuffer(9);
		FLVHeader header = new FLVHeader();
		in.skip(3);
		header.setVersion(in.get());
		header.setTypeFlags(in.get());
		header.setDataOffset(in.getInt());
		if (log.isDebugEnabled()) {
			log.debug("Header: " + header.toString());
		}
	}

	/** {@inheritDoc}
	 */
	public IStreamableFile getFile() {
		// TODO wondering if we need to have a reference
		return null;
	}

	/** {@inheritDoc}
	 */
	public int getOffset() {
		// XXX what's the difference from getBytesRead
		return 0;
	}

	/** {@inheritDoc}
	 */
	public long getBytesRead() {
		// XXX should summarize the total bytes read or
		// just the current position?
		return getCurrentPosition();
	}

	/** {@inheritDoc} */
    public long getDuration() {
		return duration;
	}

	/** {@inheritDoc}
	 */
	public boolean hasMoreTags() {
		return getRemainingBytes() > 4;
	}

    /**
     * Create tag for metadata event
     * @return         Metadata event tag
     */
    private ITag createFileMeta() {
		// Create tag for onMetaData event
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);

        // Duration property
        out.writeString("onMetaData");
		out.writeStartMap(3);
		out.writePropertyName("duration");
		out.writeNumber(duration / 1000.0);
		if (firstVideoTag != -1) {
			long old = getCurrentPosition();
			setCurrentPosition(firstVideoTag);
			readTagHeader();
			fillBuffer(1);
			byte frametype = in.get();
            // Video codec id
            out.writePropertyName("videocodecid");
			out.writeNumber(frametype & MASK_VIDEO_CODEC);
			setCurrentPosition(old);
		}
		if (firstAudioTag != -1) {
			long old = getCurrentPosition();
			setCurrentPosition(firstAudioTag);
			readTagHeader();
			fillBuffer(1);
			byte frametype = in.get();
            // Audio codec id
            out.writePropertyName("audiocodecid");
			out.writeNumber((frametype & MASK_SOUND_FORMAT) >> 4);
			setCurrentPosition(old);
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

	/** {@inheritDoc}
	 */
    public synchronized ITag readTag() {
		long oldPos = getCurrentPosition();
		ITag tag = readTagHeader();

		if (tagPosition == 0 && tag.getDataType() != TYPE_METADATA
				&& generateMetadata) {
			// Generate initial metadata automatically
			setCurrentPosition(oldPos);
			KeyFrameMeta meta = analyzeKeyFrames();
			tagPosition++;
			if (meta != null) {
				return createFileMeta();
			}
		}

		ByteBuffer body = ByteBuffer.allocate(tag.getBodySize());

		// XXX Paul: this assists in 'properly' handling damaged FLV files		
		long newPosition = getCurrentPosition() + tag.getBodySize();
		if (newPosition <= getTotalBytes()) {
			int limit;
			while (getCurrentPosition() < newPosition) {
				fillBuffer(newPosition - getCurrentPosition());
				if (getCurrentPosition() + in.remaining() > newPosition) {
					limit = in.limit();
					in.limit((int) (newPosition - getCurrentPosition()) + in.position());
					body.put(in);
					in.limit(limit);
				} else {
					body.put(in);
				}
			}

			body.flip();
			tag.setBody(body);
			tagPosition++;
		}

		return tag;
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.Reader#close()
	 */
	public void close() {
		log.debug("Reader close");
		if (in != null) {
			in.release();
			in = null;
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

	/**
	 * Key frames analysis may be used as a utility method so
	 * synchronize it.
	 */
	synchronized public KeyFrameMeta analyzeKeyFrames() {
		if (keyframeMeta != null) {
			return keyframeMeta;
		}

		List<Long> positionList = new ArrayList<Long>();
		List<Integer> timestampList = new ArrayList<Integer>();
		long origPos = getCurrentPosition();
		// point to the first tag
		setCurrentPosition(9);
		posTagMap = new HashMap<Long, Integer>();
		int idx = 0;
		while (this.hasMoreTags()) {
			long pos = getCurrentPosition();
			posTagMap.put((long) pos, idx++);
			ITag tmpTag = this.readTagHeader();
			duration = tmpTag.getTimestamp();
			if (tmpTag.getDataType() == IoConstants.TYPE_VIDEO) {
				if (firstVideoTag == -1) {
					firstVideoTag = pos;
				}

				// Grab Frame type
				fillBuffer(1);
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
			long newPosition = pos + tmpTag.getBodySize() + 15;
			// log.debug("---->" + in.remaining() + " limit=" + in.limit() + "
			// new pos=" + newPosition);
			if (newPosition >= getTotalBytes()) {
				log.info("New position exceeds limit");
				if (log.isDebugEnabled()) {
					log.debug("-----");
					log.debug("Keyframe analysis");
					log.debug(" data type=" + tmpTag.getDataType()
							+ " bodysize=" + tmpTag.getBodySize());
					log.debug(" remaining=" + getRemainingBytes() + " limit="
							+ getTotalBytes() + " new pos=" + newPosition);
					log.debug(" pos=" + pos);
					log.debug("-----");
				}
				break;
			} else {
				setCurrentPosition(newPosition);
			}
		}
		// restore the pos
		setCurrentPosition(origPos);

		keyframeMeta = new KeyFrameMeta();
		posTimeMap = new HashMap<Long, Long>();
		keyframeMeta.positions = new long[positionList.size()];
		keyframeMeta.timestamps = new int[timestampList.size()];
		for (int i = 0; i < keyframeMeta.positions.length; i++) {
			keyframeMeta.positions[i] = positionList.get(i);
			keyframeMeta.timestamps[i] = timestampList.get(i);
			posTimeMap.put((long) positionList.get(i), (long) timestampList
					.get(i));
		}
		return keyframeMeta;
	}

	/**
	 * Put the current position to pos.
	 * The caller must ensure the pos is a valid one
	 * (eg. not sit in the middle of a frame)
	 * @param pos
	 */
	public void position(long pos) {
		setCurrentPosition(pos);
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
		fillBuffer(15);
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
	
	public enum BufferType {
		AUTO,
		DIRECT,
		HEAP
	}
}
