package org.red5.io.mp3.impl;

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
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.impl.Tag;

public class MP3Reader implements ITagReader, IKeyFrameDataAnalyzer {

	protected static Log log = LogFactory.getLog(MP3Reader.class.getName());

	private FileInputStream fis = null;

	private FileChannel channel = null;

	private MappedByteBuffer mappedFile = null;

	private ByteBuffer in = null;

	private ITag tag = null;

	private int prevSize = 0;

	private double currentTime = 0;

	private KeyFrameMeta frameMeta = null;

	private HashMap<Integer, Double> posTimeMap = null;

	private int dataRate = 0;

	private boolean firstFrame;

	private ITag fileMeta;

	private long duration = 0;

	public MP3Reader(FileInputStream stream) {
		fis = stream;
		channel = fis.getChannel();
		try {
			mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel
					.size());
		} catch (IOException e) {
			log.error("MP3Reader :: MP3Reader ::>\n", e);
		}
		mappedFile.order(ByteOrder.BIG_ENDIAN);
		in = ByteBuffer.wrap(mappedFile);
		analyzeKeyFrames();
		firstFrame = true;
		fileMeta = createFileMeta();
		if (in.remaining() > 4) {
			searchNextFrame();
			int pos = in.position();
			MP3Header header = readHeader();
			in.position(pos);
			if (header != null) {
				checkValidHeader(header);
			} else {
				throw new RuntimeException("No initial header found.");
			}
		}
	}

	/**
	 * Check if the file can be played back with Flash. 
	 * @param header
	 */
	private void checkValidHeader(MP3Header header) {
		switch (header.getSampleRate()) {
			case 44100:
			case 22050:
			case 11025:
			case 5513:
				// Supported sample rate
				break;

			default:
				throw new RuntimeException("Unsupported sample rate: "
						+ header.getSampleRate());
		}
	}

	private ITag createFileMeta() {
		// Create tag for onMetaData event
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		out.writeStartMap(3);
		out.writePropertyName("duration");
		out
				.writeNumber(frameMeta.timestamps[frameMeta.timestamps.length - 1] / 1000.0);
		out.writePropertyName("audiocodecid");
		out.writeNumber(IoConstants.FLAG_FORMAT_MP3);
		if (dataRate > 0) {
			out.writePropertyName("audiodatarate");
			out.writeNumber(dataRate);
		}
		out.writePropertyName("canSeekToEnd");
		out.writeBoolean(true);
		out.markEndMap();
		buf.flip();

		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null,
				prevSize);
		result.setBody(buf);
		return result;
	}

	/** Search for next frame sync. */
	public void searchNextFrame() {
		while (in.remaining() > 1) {
			int ch = in.get() & 0xff;
			if (ch != 0xff) {
				continue;
			}

			if ((in.get() & 0xe0) == 0xe0) {
				// Found it
				in.position(in.position() - 2);
				return;
			}
		}
	}

	public IStreamableFile getFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getBytesRead() {
		return in.position();
	}

	public long getDuration() {
		return duration;
	}

	public boolean hasMoreTags() {
		MP3Header header = null;
		while (header == null && in.remaining() > 4) {
			try {
				header = new MP3Header(in.getInt());
			} catch (IOException e) {
				log.error("MP3Reader :: hasMoreTags ::>\n", e);
				break;
			} catch (Exception e) {
				searchNextFrame();
			}
		}

		if (header == null) {
			return false;
		}

		if (in.position() + header.frameSize() - 4 > in.limit()) {
			// Last frame is incomplete
			in.position(in.limit());
			return false;
		}

		in.position(in.position() - 4);
		return true;
	}

	private MP3Header readHeader() {
		MP3Header header = null;
		while (header == null && in.remaining() > 4) {
			try {
				header = new MP3Header(in.getInt());
			} catch (IOException e) {
				log.error("MP3Reader :: readTag ::>\n", e);
				break;
			} catch (Exception e) {
				searchNextFrame();
			}
		}
		return header;
	}

	public synchronized ITag readTag() {
		if (firstFrame) {
			// Return file metadata as first tag.
			firstFrame = false;
			return fileMeta;
		}

		MP3Header header = readHeader();
		if (header == null) {
			return null;
		}

		int frameSize = header.frameSize();
		if (in.position() + frameSize - 4 > in.limit()) {
			// Last frame is incomplete
			in.position(in.limit());
			return null;
		}

		tag = new Tag(IoConstants.TYPE_AUDIO, (int) currentTime, frameSize + 1,
				null, prevSize);
		prevSize = frameSize + 1;
		currentTime += header.frameDuration();
		ByteBuffer body = ByteBuffer.allocate(tag.getBodySize());
		byte tagType = (IoConstants.FLAG_FORMAT_MP3 << 4)
				| (IoConstants.FLAG_SIZE_16_BIT << 1);
		switch (header.getSampleRate()) {
			case 44100:
				tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
				break;
			case 22050:
				tagType |= IoConstants.FLAG_RATE_22_KHZ << 2;
				break;
			case 11025:
				tagType |= IoConstants.FLAG_RATE_11_KHZ << 2;
				break;
			default:
				tagType |= IoConstants.FLAG_RATE_5_5_KHZ << 2;
		}
		tagType |= (header.isStereo() ? IoConstants.FLAG_TYPE_STEREO
				: IoConstants.FLAG_TYPE_MONO);
		body.put(tagType);
		final int limit = in.limit();
		body.putInt(header.getData());
		in.limit(in.position() + frameSize - 4);
		body.put(in);
		body.flip();
		in.limit(limit);

		tag.setBody(body);

		return tag;
	}

	public void close() {
		if (posTimeMap != null) {
			posTimeMap.clear();
		}
		mappedFile.clear();
		if (in != null) {
			in.release();
			in = null;
		}
		try {
			fis.close();
			channel.close();
		} catch (IOException e) {
			log.error("MP3Reader :: close ::>\n", e);
		}
	}

	public void decodeHeader() {
	}

	public void position(long pos) {
		in.position((int) pos);
		// Advance to next frame
		searchNextFrame();
		// Make sure we can resolve file positions to timestamps
		analyzeKeyFrames();
		Double time = posTimeMap.get(in.position());
		if (time != null) {
			currentTime = time;
		} else {
			// Unknown frame position - this should never happen
			currentTime = 0;
		}
	}

	public synchronized KeyFrameMeta analyzeKeyFrames() {
		if (frameMeta != null) {
			return frameMeta;
		}

		List<Integer> positionList = new ArrayList<Integer>();
		List<Double> timestampList = new ArrayList<Double>();
		dataRate = 0;
		long rate = 0;
		int count = 0;
		int origPos = in.position();
		double time = 0;
		in.position(0);
		searchNextFrame();
		while (this.hasMoreTags()) {
			MP3Header header = readHeader();
			if (header == null) {
				// No more tags
				break;
			}

			int pos = in.position() - 4;
			if (pos + header.frameSize() > in.limit()) {
				// Last frame is incomplete
				break;
			}

			positionList.add(pos);
			timestampList.add(time);
			rate += header.getBitRate() / 1000;
			time += header.frameDuration();
			in.position(pos + header.frameSize());
			count++;
		}
		// restore the pos
		in.position(origPos);

		dataRate = (int) (rate / count);
		posTimeMap = new HashMap<Integer, Double>();
		frameMeta = new KeyFrameMeta();
		frameMeta.positions = new int[positionList.size()];
		frameMeta.timestamps = new int[timestampList.size()];
		for (int i = 0; i < frameMeta.positions.length; i++) {
			frameMeta.positions[i] = positionList.get(i);
			frameMeta.timestamps[i] = timestampList.get(i).intValue();
			posTimeMap.put(positionList.get(i), timestampList.get(i));
		}
		duration = (long) time;
		return frameMeta;
	}

}
