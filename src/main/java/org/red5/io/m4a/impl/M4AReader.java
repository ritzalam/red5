/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.io.m4a.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.impl.Tag;
import org.red5.io.mp4.MP4Atom;
import org.red5.io.mp4.MP4DataStream;
import org.red5.io.mp4.MP4Descriptor;
import org.red5.io.mp4.MP4Frame;
import org.red5.io.mp4.impl.MP4Reader;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Reader is used to read the contents of a M4A file.
 * NOTE: This class is not implemented as threading-safe. The caller
 * should make sure the threading-safety.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class M4AReader implements IoConstants, ITagReader {

	/**
	 * Logger
	 */
	private static Logger log = LoggerFactory.getLogger(M4AReader.class);

	/**
	 * File
	 */
	private File file;

	/**
	 * Input stream
	 */
	private MP4DataStream fis;

	/**
	 * File channel
	 */
	private FileChannel channel;

	/**
	 * Memory-mapped buffer for file content
	 */
	private MappedByteBuffer mappedFile;

	/**
	 * Input byte buffer
	 */
	private IoBuffer in;

	private String audioCodecId = "mp4a";

	//decoder bytes / configs
	private byte[] audioDecoderBytes;

	/** Duration in milliseconds. */
	private long duration;

	private int timeScale;

	//audio sample rate kHz
	private double audioTimeScale;

	private int audioChannels;

	//default to aac lc
	private int audioCodecType = 1;

	private String formattedDuration;

	private long moovOffset;

	private long mdatOffset;

	//samples to chunk mappings
	private Vector<MP4Atom.Record> audioSamplesToChunks;

	//samples 
	private Vector<Integer> audioSamples;

	//chunk offsets
	private Vector<Long> audioChunkOffsets;

	//sample duration
	private int audioSampleDuration = 1024;

	//keep track of current sample
	private int currentFrame = 1;

	private int prevFrameSize = 0;

	private List<MP4Frame> frames = new ArrayList<MP4Frame>();

	/**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
	private LinkedList<ITag> firstTags = new LinkedList<ITag>();

	private final Semaphore lock = new Semaphore(1, true);

	/** Constructs a new M4AReader. */
	M4AReader() {
	}

	/**
	 * Creates M4A reader from file input stream, sets up metadata generation flag.
	 *
	 * @param f                    File input stream
	 */
	public M4AReader(File f) throws IOException {
		if (null == f) {
			log.warn("Reader was passed a null file");
			log.debug("{}", ToStringBuilder.reflectionToString(this));
		}
		this.file = f;
		this.fis = new MP4DataStream(new FileInputStream(f));
		channel = fis.getChannel();

		try {
			mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		} catch (IOException e) {
			log.error("M4AReader {}", e);
		}
		// Wrap mapped byte buffer to MINA buffer
		in = IoBuffer.wrap(mappedFile);
		//decode all the info that we want from the atoms
		decodeHeader();
		//analyze the samples/chunks and build the keyframe meta data
		analyzeFrames();
		//add meta data
		firstTags.add(createFileMeta());
		//create / add the pre-streaming (decoder config) tags
		createPreStreamingTags();
	}

	/**
	 * Accepts mapped file bytes to construct internal members.
	 *
	 * @param buffer                   Byte buffer
	 */
	public M4AReader(IoBuffer buffer) throws IOException {
		in = buffer;
		//decode all the info that we want from the atoms
		decodeHeader();
		//analyze the samples/chunks and build the keyframe meta data
		analyzeFrames();
		//add meta data
		firstTags.add(createFileMeta());
		//create / add the pre-streaming (decoder config) tags
		createPreStreamingTags();
	}

	/**
	 * This handles the moov atom being at the beginning or end of the file, so the mdat may also
	 * be before or after the moov atom.
	 */
	public void decodeHeader() {
		try {
			// the first atom will/should be the type
			MP4Atom type = MP4Atom.createAtom(fis);
			// expect ftyp
			log.debug("Type {}", MP4Atom.intToType(type.getType()));
			//log.debug("Atom int types - free={} wide={}", MP4Atom.typeToInt("free"), MP4Atom.typeToInt("wide"));
			// keep a running count of the number of atoms found at the "top" levels
			int topAtoms = 0;
			// we want a moov and an mdat, anything else throw the invalid file type error
			while (topAtoms < 2) {
				MP4Atom atom = MP4Atom.createAtom(fis);
				switch (atom.getType()) {
					case 1836019574: //moov
						topAtoms++;
						MP4Atom moov = atom;
						// expect moov
						log.debug("Type {}", MP4Atom.intToType(moov.getType()));
						log.debug("moov children: {}", moov.getChildren());
						moovOffset = fis.getOffset() - moov.getSize();

						MP4Atom mvhd = moov.lookup(MP4Atom.typeToInt("mvhd"), 0);
						if (mvhd != null) {
							log.debug("Movie header atom found");
							//get the initial timescale
							timeScale = mvhd.getTimeScale();
							duration = mvhd.getDuration();
							log.debug("Time scale {} Duration {}", timeScale, duration);
						}

						/* nothing needed here yet
						MP4Atom meta = moov.lookup(MP4Atom.typeToInt("meta"), 0);
						if (meta != null) {
							log.debug("Meta atom found");
							log.debug("{}", ToStringBuilder.reflectionToString(meta));
						}
						*/

						MP4Atom trak = moov.lookup(MP4Atom.typeToInt("trak"), 0);
						if (trak != null) {
							log.debug("Track atom found");
							log.debug("trak children: {}", trak.getChildren());
							// trak: tkhd, edts, mdia

							MP4Atom edts = trak.lookup(MP4Atom.typeToInt("edts"), 0);
							if (edts != null) {
								log.debug("Edit atom found");
								log.debug("edts children: {}", edts.getChildren());
							}

							MP4Atom mdia = trak.lookup(MP4Atom.typeToInt("mdia"), 0);
							if (mdia != null) {
								log.debug("Media atom found");
								// mdia: mdhd, hdlr, minf

								int scale = 0;
								//get the media header atom
								MP4Atom mdhd = mdia.lookup(MP4Atom.typeToInt("mdhd"), 0);
								if (mdhd != null) {
									log.debug("Media data header atom found");
									//this will be for either video or audio depending media info
									scale = mdhd.getTimeScale();
									log.debug("Time scale {}", scale);
								}

								MP4Atom hdlr = mdia.lookup(MP4Atom.typeToInt("hdlr"), 0);
								if (hdlr != null) {
									log.debug("Handler ref atom found");
									// soun or vide
									log.debug("Handler type: {}", MP4Atom.intToType(hdlr.getHandlerType()));
									String hdlrType = MP4Atom.intToType(hdlr.getHandlerType());
									if ("soun".equals(hdlrType)) {
										if (scale > 0) {
											audioTimeScale = scale * 1.0;
											log.debug("Audio time scale: {}", audioTimeScale);
										}
									}
								}

								MP4Atom minf = mdia.lookup(MP4Atom.typeToInt("minf"), 0);
								if (minf != null) {
									log.debug("Media info atom found");
									// minf: (audio) smhd, dinf, stbl / (video) vmhd,
									// dinf, stbl

									MP4Atom smhd = minf.lookup(MP4Atom.typeToInt("smhd"), 0);
									if (smhd != null) {
										log.debug("Sound header atom found");
										MP4Atom dinf = minf.lookup(MP4Atom.typeToInt("dinf"), 0);
										if (dinf != null) {
											log.debug("Data info atom found");
											// dinf: dref
											log.debug("Sound dinf children: {}", dinf.getChildren());
											MP4Atom dref = dinf.lookup(MP4Atom.typeToInt("dref"), 0);
											if (dref != null) {
												log.debug("Data reference atom found");
											}

										}
										MP4Atom stbl = minf.lookup(MP4Atom.typeToInt("stbl"), 0);
										if (stbl != null) {
											log.debug("Sample table atom found");
											// stbl: stsd, stts, stss, stsc, stsz, stco,
											// stsh
											log.debug("Sound stbl children: {}", stbl.getChildren());
											// stsd - sample description
											// stts - time to sample
											// stsc - sample to chunk
											// stsz - sample size
											// stco - chunk offset

											//stsd - has codec child
											MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
											if (stsd != null) {
												//stsd: mp4a
												log.debug("Sample description atom found");
												MP4Atom mp4a = stsd.getChildren().get(0);
												//could set the audio codec here
												setAudioCodecId(MP4Atom.intToType(mp4a.getType()));
												//log.debug("{}", ToStringBuilder.reflectionToString(mp4a));
												log.debug("Sample size: {}", mp4a.getSampleSize());
												int ats = mp4a.getTimeScale();
												//skip invalid audio time scale
												if (ats > 0) {
													audioTimeScale = ats * 1.0;
												}
												audioChannels = mp4a.getChannelCount();
												log.debug("Sample rate (audio time scale): {}", audioTimeScale);
												log.debug("Channels: {}", audioChannels);
												//mp4a: esds
												if (mp4a.getChildren().size() > 0) {
													log.debug("Elementary stream descriptor atom found");
													MP4Atom esds = mp4a.getChildren().get(0);
													log.debug("{}", ToStringBuilder.reflectionToString(esds));
													MP4Descriptor descriptor = esds.getEsd_descriptor();
													log.debug("{}", ToStringBuilder.reflectionToString(descriptor));
													if (descriptor != null) {
														Vector<MP4Descriptor> children = descriptor.getChildren();
														for (int e = 0; e < children.size(); e++) {
															MP4Descriptor descr = children.get(e);
															log.debug("{}", ToStringBuilder.reflectionToString(descr));
															if (descr.getChildren().size() > 0) {
																Vector<MP4Descriptor> children2 = descr.getChildren();
																for (int e2 = 0; e2 < children2.size(); e2++) {
																	MP4Descriptor descr2 = children2.get(e2);
																	log.debug("{}", ToStringBuilder.reflectionToString(descr2));
																	if (descr2.getType() == MP4Descriptor.MP4DecSpecificInfoDescriptorTag) {
																		//we only want the MP4DecSpecificInfoDescriptorTag
																		audioDecoderBytes = descr2.getDSID();
																		//compare the bytes to get the aacaot/aottype 
																		//match first byte
																		switch (audioDecoderBytes[0]) {
																			case 0x12:
																			default:
																				//AAC LC - 12 10
																				audioCodecType = 1;
																				break;
																			case 0x0a:
																				//AAC Main - 0A 10
																				audioCodecType = 0;
																				break;
																			case 0x11:
																			case 0x13:
																				//AAC LC SBR - 11 90 & 13 xx
																				audioCodecType = 2;
																				break;
																		}
																		//we want to break out of top level for loop
																		e = 99;
																		break;
																	}
																}
															}
														}
													}
												}
											}
											//stsc - has Records
											MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
											if (stsc != null) {
												log.debug("Sample to chunk atom found");
												audioSamplesToChunks = stsc.getRecords();
												log.debug("Record count: {}", audioSamplesToChunks.size());
												MP4Atom.Record rec = audioSamplesToChunks.firstElement();
												log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
											}
											//stsz - has Samples
											MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
											if (stsz != null) {
												log.debug("Sample size atom found");
												audioSamples = stsz.getSamples();
												//vector full of integers										
												log.debug("Sample size: {}", stsz.getSampleSize());
												log.debug("Sample count: {}", audioSamples.size());
											}
											//stco - has Chunks
											MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
											if (stco != null) {
												log.debug("Chunk offset atom found");
												//vector full of integers
												audioChunkOffsets = stco.getChunks();
												log.debug("Chunk count: {}", audioChunkOffsets.size());
											}
											//stts - has TimeSampleRecords
											MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
											if (stts != null) {
												log.debug("Time to sample atom found");
												Vector<MP4Atom.TimeSampleRecord> records = stts.getTimeToSamplesRecords();
												log.debug("Record count: {}", records.size());
												MP4Atom.TimeSampleRecord rec = records.firstElement();
												log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
												//if we have 1 record then all samples have the same duration
												if (records.size() > 1) {
													//TODO: handle audio samples with varying durations
													log.warn("Audio samples have differing durations, audio playback may fail");
												}
												audioSampleDuration = rec.getSampleDuration();
											}
										}
									}
								}
							}
						}

						//real duration
						StringBuilder sb = new StringBuilder();
						double clipTime = ((double) duration / (double) timeScale);
						log.debug("Clip time: {}", clipTime);
						int minutes = (int) (clipTime / 60);
						if (minutes > 0) {
							sb.append(minutes);
							sb.append('.');
						}
						//formatter for seconds / millis
						NumberFormat df = DecimalFormat.getInstance();
						df.setMaximumFractionDigits(2);
						sb.append(df.format((clipTime % 60)));
						formattedDuration = sb.toString();
						log.debug("Time: {}", formattedDuration);

						break;
					case 1835295092: //mdat
						topAtoms++;
						long dataSize = 0L;
						MP4Atom mdat = atom;
						dataSize = mdat.getSize();
						log.debug("{}", ToStringBuilder.reflectionToString(mdat));
						mdatOffset = fis.getOffset() - dataSize;
						log.debug("File size: {} mdat size: {}", file.length(), dataSize);

						break;
					case 1718773093: //free
					case 2003395685: //wide
						break;
					default:
						log.warn("Unexpected atom: {}", MP4Atom.intToType(atom.getType()));
				}
			}

			//add the tag name (size) to the offsets
			moovOffset += 8;
			mdatOffset += 8;
			log.debug("Offsets moov: {} mdat: {}", moovOffset, mdatOffset);

		} catch (IOException e) {
			log.error("Exception decoding header / atoms", e);
		}
	}

	public long getTotalBytes() {
		try {
			return channel.size();
		} catch (Exception e) {
			log.error("Error getTotalBytes", e);
			return 0;
		}
	}

	/** {@inheritDoc} */
	public boolean hasVideo() {
		return false;
	}

	/**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
	public IoBuffer getFileData() {
		return null;
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
		return in.position();
	}

	/** {@inheritDoc} */
	public long getDuration() {
		return duration;
	}

	public String getAudioCodecId() {
		return audioCodecId;
	}

	/** {@inheritDoc}
	 */
	public boolean hasMoreTags() {
		return currentFrame < frames.size();
	}

	/**
	 * Create tag for metadata event.
	 *
	 * @return         Metadata event tag
	 */
	ITag createFileMeta() {
		log.debug("Creating onMetaData");
		// Create tag for onMetaData event
		IoBuffer buf = IoBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> props = new HashMap<Object, Object>();
		// Duration property
		props.put("duration", ((double) duration / (double) timeScale));

		// Audio codec id - watch for mp3 instead of aac
		props.put("audiocodecid", audioCodecId);
		props.put("aacaot", audioCodecType);
		props.put("audiosamplerate", audioTimeScale);
		props.put("audiochannels", audioChannels);

		props.put("moovposition", moovOffset);
		//tags will only appear if there is an "ilst" atom in the file
		//props.put("tags", "");

		props.put("canSeekToEnd", false);
		out.writeMap(props, new Serializer());
		buf.flip();

		//now that all the meta properties are done, update the duration
		duration = Math.round(duration * 1000d);

		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
		result.setBody(buf);
		return result;
	}

	/**
	 * Tag sequence
	 * MetaData, Audio config, remaining audio  
	 * 
	 * Packet prefixes:
	 * af 00 ...   06 = Audio extra data (first audio packet)
	 * af 01          = Audio frame
	 * 
	 * Audio extra data(s): 
	 * af 00                = Prefix
	 * 11 90 4f 14          = AAC Main   = aottype 0
	 * 12 10                = AAC LC     = aottype 1
	 * 13 90 56 e5 a5 48 00 = HE-AAC SBR = aottype 2
	 * 06                   = Suffix
	 * 
	 * Still not absolutely certain about this order or the bytes - need to verify later
	 */
	private void createPreStreamingTags() {
		log.debug("Creating pre-streaming tags");
		if (audioDecoderBytes != null) {
			IoBuffer body = IoBuffer.allocate(audioDecoderBytes.length + 3);
			body.put(new byte[] { (byte) 0xaf, (byte) 0 }); //prefix
			if (log.isDebugEnabled()) {
				log.debug("Audio decoder bytes: {}", HexDump.byteArrayToHexString(audioDecoderBytes));
			}
			body.put(audioDecoderBytes);
			body.put((byte) 0x06); //suffix
			ITag tag = new Tag(IoConstants.TYPE_AUDIO, 0, body.position(), null, prevFrameSize);
			body.flip();
			tag.setBody(body);
			//add tag
			firstTags.add(tag);
		} else {
			//default to aac-lc when the esds doesnt contain descripter bytes
			log.warn("Audio decoder bytes were not available");
		}
	}

	/**
	 * Packages media data for return to providers.
	 *
	 */
	public ITag readTag() {
		//log.debug("Read tag");
		ITag tag = null;
		try {
			lock.acquire();
			//empty-out the pre-streaming tags first
			if (!firstTags.isEmpty()) {
				log.debug("Returning pre-tag");
				// Return first tags before media data
				return firstTags.removeFirst();
			}
			//log.debug("Read tag - sample {} prevFrameSize {} audio: {} video: {}", new Object[]{currentSample, prevFrameSize, audioCount, videoCount});

			//get the current frame
			MP4Frame frame = frames.get(currentFrame);
			log.debug("Playback {}", frame);

			int sampleSize = frame.getSize();

			int time = (int) Math.round(frame.getTime() * 1000.0);
			//log.debug("Read tag - dst: {} base: {} time: {}", new Object[]{frameTs, baseTs, time});

			long samplePos = frame.getOffset();
			//log.debug("Read tag - samplePos {}", samplePos);

			//determine frame type and packet body padding
			byte type = frame.getType();

			//create a byte buffer of the size of the sample
			ByteBuffer data = ByteBuffer.allocate(sampleSize + 2);
			try {
				//log.debug("Writing audio prefix");
				data.put(MP4Reader.PREFIX_AUDIO_FRAME);
				//do we need to add the mdat offset to the sample position?
				channel.position(samplePos);
				channel.read(data);
			} catch (IOException e) {
				log.error("Error on channel position / read", e);
			}

			//chunk the data
			IoBuffer payload = IoBuffer.wrap(data.array());

			//create the tag
			tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
			//log.debug("Read tag - type: {} body size: {}", (type == TYPE_AUDIO ? "Audio" : "Video"), tag.getBodySize());

			//increment the sample number
			currentFrame++;
			//set the frame / tag size
			prevFrameSize = tag.getBodySize();
		} catch (InterruptedException e) {
			log.warn("Exception acquiring lock", e);
		} finally {
			lock.release();
		}
		//log.debug("Tag: {}", tag);
		return tag;
	}

	/**
	 * Performs frame analysis and generates metadata for use in seeking. All the frames
	 * are analyzed and sorted together based on time and offset.
	 */
	public void analyzeFrames() {
		log.debug("Analyzing frames");

		// tag == sample
		int sample = 1;
		Long pos = null;

		//add the audio frames / samples / chunks		
		for (int i = 0; i < audioSamplesToChunks.size(); i++) {
			MP4Atom.Record record = audioSamplesToChunks.get(i);
			int firstChunk = record.getFirstChunk();
			int lastChunk = audioChunkOffsets.size();
			if (i < audioSamplesToChunks.size() - 1) {
				MP4Atom.Record nextRecord = audioSamplesToChunks.get(i + 1);
				lastChunk = nextRecord.getFirstChunk() - 1;
			}
			for (int chunk = firstChunk; chunk <= lastChunk; chunk++) {
				int sampleCount = record.getSamplesPerChunk();
				pos = audioChunkOffsets.elementAt(chunk - 1);
				while (sampleCount > 0) {
					//calculate ts
					double ts = (audioSampleDuration * (sample - 1)) / audioTimeScale;
					//sample size
					int size = (audioSamples.get(sample - 1)).intValue();
					//create a frame
					MP4Frame frame = new MP4Frame();
					frame.setOffset(pos);
					frame.setSize(size);
					frame.setTime(ts);
					frame.setType(TYPE_AUDIO);
					frames.add(frame);

					log.debug("Sample #{} {}", sample, frame);

					//inc and dec stuff
					pos += size;
					sampleCount--;
					sample++;
				}
			}
		}

		//sort the frames
		Collections.sort(frames);

		log.debug("Frames count: {}", frames.size());
		//log.debug("Frames: {}", frames);
	}

	/**
	 * Put the current position to pos. The caller must ensure the pos is a valid one.
	 *
	 * @param pos position to move to in file / channel
	 */
	public void position(long pos) {
		log.debug("position: {}", pos);
		currentFrame = getFrame(pos);
		log.debug("Setting current sample: {}", currentFrame);
	}

	/**
	 * Search through the frames by offset / position to find the sample.
	 * 
	 * @param pos
	 * @return
	 */
	private int getFrame(long pos) {
		int sample = 1;
		int len = frames.size();
		MP4Frame frame = null;
		for (int f = 0; f < len; f++) {
			frame = frames.get(f);
			if (pos == frame.getOffset()) {
				sample = f;
				break;
			}
		}
		return sample;
	}

	/** {@inheritDoc}
	 */
	public void close() {
		log.debug("Close");
		if (in != null) {
			in.free();
			in = null;
		}
		if (channel != null) {
			try {
				channel.close();
				fis.close();
				fis = null;
			} catch (IOException e) {
				log.error("Channel close {}", e);
			} finally {
				if (frames != null) {
					frames.clear();
					frames = null;
				}
			}
		}
	}

	public void setAudioCodecId(String audioCodecId) {
		this.audioCodecId = audioCodecId;
	}

	public ITag readTagHeader() {
		return null;
	}

}
