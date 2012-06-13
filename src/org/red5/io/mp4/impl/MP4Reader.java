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

package org.red5.io.mp4.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.impl.Tag;
import org.red5.io.mp4.MP4Atom;
import org.red5.io.mp4.MP4Atom.CompositionTimeSampleRecord;
import org.red5.io.mp4.MP4DataStream;
import org.red5.io.mp4.MP4Descriptor;
import org.red5.io.mp4.MP4Frame;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This reader is used to read the contents of an MP4 file.
 * 
 * NOTE: This class is not implemented as thread-safe, the caller
 * should ensure the thread-safety.
 * <p>
 * New NetStream notifications
 * <br />
 * Two new notifications facilitate the implementation of the playback components:
 * <ul>
 * <li>NetStream.Play.FileStructureInvalid: This event is sent if the player detects 
 * an MP4 with an invalid file structure. Flash Player cannot play files that have 
 * invalid file structures.</li>
 * <li>NetStream.Play.NoSupportedTrackFound: This event is sent if the player does not 
 * detect any supported tracks. If there aren't any supported video, audio or data 
 * tracks found, Flash Player does not play the file.</li>
 * </ul>
 * </p>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class MP4Reader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

	private static Logger log = LoggerFactory.getLogger(MP4Reader.class);

	/** Audio packet prefix */
	public final static byte[] PREFIX_AUDIO_FRAME = new byte[] { (byte) 0xaf, (byte) 0x01 };

	/** Blank AAC data **/
	public final static byte[] EMPTY_AAC = { (byte) 0x21, (byte) 0x10, (byte) 0x04, (byte) 0x60, (byte) 0x8c, (byte) 0x1c };

	/** Video packet prefix for the decoder frame */
	public final static byte[] PREFIX_VIDEO_CONFIG_FRAME = new byte[] { (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	/** Video packet prefix for key frames */
	public final static byte[] PREFIX_VIDEO_KEYFRAME = new byte[] { (byte) 0x17, (byte) 0x01 };

	/** Video packet prefix for standard frames (interframe) */
	public final static byte[] PREFIX_VIDEO_FRAME = new byte[] { (byte) 0x27, (byte) 0x01 };

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

	/** Mapping between file position and timestamp in ms. */
	private HashMap<Integer, Long> timePosMap;

	private HashMap<Integer, Long> samplePosMap;

	/** Whether or not the clip contains a video track */
	private boolean hasVideo = false;

	/** Whether or not the clip contains an audio track */
	private boolean hasAudio = false;

	//default video codec 
	private String videoCodecId = "avc1";

	//default audio codec 
	private String audioCodecId = "mp4a";

	//decoder bytes / configs
	private byte[] audioDecoderBytes;

	private byte[] videoDecoderBytes;

	// duration in milliseconds
	private long duration;

	// movie time scale
	private int timeScale;

	private int width;

	private int height;

	//audio sample rate kHz
	private double audioTimeScale;

	private int audioChannels;

	//default to aac lc
	private int audioCodecType = 1;

	private int videoSampleCount;

	private double fps;

	private double videoTimeScale;

	private int avcLevel;

	private int avcProfile;

	private String formattedDuration;

	private long moovOffset;

	private long mdatOffset;

	//samples to chunk mappings
	private Vector<MP4Atom.Record> videoSamplesToChunks;

	private Vector<MP4Atom.Record> audioSamplesToChunks;

	//keyframe - sample numbers
	private Vector<Integer> syncSamples;

	//samples 
	private Vector<Integer> videoSamples;

	private Vector<Integer> audioSamples;

	//chunk offsets
	private Vector<Long> videoChunkOffsets;

	private Vector<Long> audioChunkOffsets;

	//sample duration
	private int videoSampleDuration = 125;

	private int audioSampleDuration = 1024;

	//keep track of current frame / sample
	private int currentFrame = 0;

	private int prevFrameSize = 0;

	private int prevVideoTS = -1;

	private List<MP4Frame> frames = new ArrayList<MP4Frame>();

	@SuppressWarnings("unused")
	private long audioCount;

	@SuppressWarnings("unused")
	private long videoCount;

	// composition time to sample entries
	private Vector<MP4Atom.CompositionTimeSampleRecord> compositionTimes;

	/**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
	private LinkedList<ITag> firstTags = new LinkedList<ITag>();

	/**
	 * Container for seek points in the video. These are the time stamps
	 * for the key frames or samples.
	 */
	private LinkedList<Integer> seekPoints;

	private final Semaphore lock = new Semaphore(1, true);

	/** Constructs a new MP4Reader. */
	MP4Reader() {
	}

	/**
	 * Creates MP4 reader from file input stream, sets up metadata generation flag.
	 *
	 * @param f                    File input stream
	 */
	public MP4Reader(File f) throws IOException {
		if (null == f) {
			log.warn("Reader was passed a null file");
			log.debug("{}", ToStringBuilder.reflectionToString(this));
		}
		this.file = f;
		this.fis = new MP4DataStream(new FileInputStream(f));
		channel = fis.getChannel();
		//decode all the info that we want from the atoms
		decodeHeader();
		//analyze the samples/chunks and build the keyframe meta data
		analyzeFrames();
		//add meta data
		firstTags.add(createFileMeta());
		//create / add the pre-streaming (decoder config) tags
		createPreStreamingTags(0, false);
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
			// we want a moov and an mdat, anything else will throw the invalid file type error
			MP4Atom moov = null;
			MP4Atom mdat = null;
			while (moov == null || mdat == null) {
				MP4Atom atom = MP4Atom.createAtom(fis);
				switch (atom.getType()) {
					case 1836019574: //moov
						moov = atom;
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
						MP4Atom meta = moov.lookup(MP4Atom.typeToInt("meta"), 0);
						if (meta != null) {
							log.debug("Meta atom found");
							log.debug("{}", ToStringBuilder.reflectionToString(meta));
						}
						MP4Atom udta = moov.lookup(MP4Atom.typeToInt("udta"), 0);
						if (udta != null) {
							log.debug("User data atom found");
							log.debug("{}", ToStringBuilder.reflectionToString(udta));
							if ((meta = udta.lookup(MP4Atom.typeToInt("meta"), 0)) != null) {
								log.debug("Meta atom found");
								log.debug("{}", ToStringBuilder.reflectionToString(meta));
							}
						}
						//we would like to have two tracks, but it shouldn't be a requirement
						int loops = 0;
						int tracks = 0;
						do {
							MP4Atom trak = moov.lookup(MP4Atom.typeToInt("trak"), loops);
							if (trak != null) {
								log.debug("Track atom found");
								log.debug("trak children: {}", trak.getChildren());
								// trak: tkhd, edts, mdia
								MP4Atom tkhd = trak.lookup(MP4Atom.typeToInt("tkhd"), 0);
								if (tkhd != null) {
									log.debug("Track header atom found");
									log.debug("tkhd children: {}", tkhd.getChildren());
									if (tkhd.getWidth() > 0) {
										width = tkhd.getWidth();
										height = tkhd.getHeight();
										log.debug("Width {} x Height {}", width, height);
									}
								}
								MP4Atom edts = trak.lookup(MP4Atom.typeToInt("edts"), 0);
								if (edts != null) {
									log.debug("Edit atom found");
									log.debug("edts children: {}", edts.getChildren());
									//log.debug("Width {} x Height {}", edts.getWidth(), edts.getHeight());
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
										if ("vide".equals(hdlrType)) {
											hasVideo = true;
											if (scale > 0) {
												videoTimeScale = scale * 1.0;
												log.debug("Video time scale: {}", videoTimeScale);
											}
										} else if ("soun".equals(hdlrType)) {
											hasAudio = true;
											if (scale > 0) {
												audioTimeScale = scale * 1.0;
												log.debug("Audio time scale: {}", audioTimeScale);
											}
										}
										tracks++;
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
													log.debug("Sample description atom found, containing {} children", stsd.getChildren().size());
													MP4Atom mp4a = stsd.getChildren().get(0);
													log.debug("MP4 audio atom found, containing {} children", mp4a.getChildren().size());
													log.debug("mp4a: {}", ToStringBuilder.reflectionToString(mp4a));
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
													// look for esds
													MP4Atom esds = mp4a.lookup(MP4Atom.typeToInt("esds"), 0);
													if (esds == null) {
														// check for decompression param atom
														MP4Atom wave = mp4a.lookup(MP4Atom.typeToInt("wave"), 0);
														log.debug("Decompression param atom found, containing {} children", wave.getChildren().size());
														log.debug("wave: {}", ToStringBuilder.reflectionToString(wave));
														MP4Atom tmp = wave.getChildren().get(0);
														log.debug("Temp: {}", ToStringBuilder.reflectionToString(tmp));
														esds = wave.lookup(MP4Atom.typeToInt("esds"), 0);
														if (esds == null) {
															tmp = wave.lookup(MP4Atom.typeToInt("mp4a"), 0);
															esds = tmp.lookup(MP4Atom.typeToInt("esds"), 0);
														}
													}
													//mp4a: esds
													if (esds != null) {
														log.debug("Elementary stream descriptor atom found, containing {} children", esds.getChildren().size());
														log.debug("esds: {}", ToStringBuilder.reflectionToString(esds));
														MP4Descriptor descriptor = esds.getEsd_descriptor();
														if (descriptor != null) {
															log.debug("ESD descriptor: {}", ToStringBuilder.reflectionToString(descriptor));
															Vector<MP4Descriptor> children = descriptor.getChildren();
															for (int e = 0; e < children.size(); e++) {
																MP4Descriptor descr = children.get(e);
																log.debug("Descriptor child: {}", ToStringBuilder.reflectionToString(descr));
																if (descr.getChildren().size() > 0) {
																	Vector<MP4Descriptor> children2 = descr.getChildren();
																	for (int e2 = 0; e2 < children2.size(); e2++) {
																		MP4Descriptor descr2 = children2.get(e2);
																		log.debug("{}", ToStringBuilder.reflectionToString(descr2));
																		// http://stackoverflow.com/questions/3987850/mp4-atom-how-to-discriminate-the-audio-codec-is-it-aac-or-mp3
																		if (descr2.getType() == MP4Descriptor.MP4DecSpecificInfoDescriptorTag) {
																			//we only want the MP4DecSpecificInfoDescriptorTag
																			audioDecoderBytes = descr2.getDSID();
																			/* the first 5 (0-4) bits tell us about the coder used for aacaot/aottype
																			 * http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
																			 0 - NULL
																			 1 - AAC Main (a deprecated AAC profile from MPEG-2)
																			 2 - AAC LC or backwards compatible HE-AAC 
																			 3 - AAC Scalable Sample Rate
																			 4 - AAC LTP (a replacement for AAC Main, rarely used)
																			 5 - HE-AAC explicitly signaled (Non-backward compatible)
																			23 - Low Delay AAC
																			29 - HE-AACv2 explicitly signaled
																			32 - MP3on4 Layer 1
																			33 - MP3on4 Layer 2
																			34 - MP3on4 Layer 3
																			*/
																			byte audioCoderType = audioDecoderBytes[0];
																			//match first byte
																			switch (audioCoderType) {
																				case 0x02:
																					log.debug("Audio type AAC LC");
																				case 0x11: //ER (Error Resilient) AAC LC
																					log.debug("Audio type ER AAC LC");
																				default:
																					audioCodecType = 1; //AAC LC
																					break;
																				case 0x01:
																					log.debug("Audio type AAC Main");
																					audioCodecType = 0; //AAC Main
																					break;
																				case 0x03:
																					log.debug("Audio type AAC SBR");
																					audioCodecType = 2; //AAC LC SBR
																					break;
																				case 0x05:
																				case 0x1d:
																					log.debug("Audio type AAC HE");
																					audioCodecType = 3; //AAC HE
																					break;
																				case 0x20:
																				case 0x21:
																				case 0x22:
																					log.debug("Audio type MP3");
																					audioCodecType = 33; //MP3
																					audioCodecId = "mp3";
																					break;
																			}
																			log.debug("Audio coder type: {} {} id: {}",
																					new Object[] { audioCoderType, Integer.toBinaryString(audioCoderType), audioCodecId });
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
												} else {
													//co64 - has Chunks
													MP4Atom co64 = stbl.lookup(MP4Atom.typeToInt("co64"), 0);
													if (co64 != null) {
														log.debug("Chunk offset (64) atom found");
														//vector full of longs
														audioChunkOffsets = co64.getChunks();
														log.debug("Chunk count: {}", audioChunkOffsets.size());
													}
												}
												//stts - has TimeSampleRecords
												MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
												if (stts != null) {
													log.debug("Time to sample atom found");
													Vector<MP4Atom.TimeSampleRecord> records = stts.getTimeToSamplesRecords();
													log.debug("Record count: {}", records.size());
													MP4Atom.TimeSampleRecord rec = records.firstElement();
													log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
													//if we have 1 record it means all samples have the same duration
													audioSampleDuration = rec.getSampleDuration();
												}
											}
										}
										MP4Atom vmhd = minf.lookup(MP4Atom.typeToInt("vmhd"), 0);
										if (vmhd != null) {
											log.debug("Video header atom found");
											MP4Atom dinf = minf.lookup(MP4Atom.typeToInt("dinf"), 0);
											if (dinf != null) {
												log.debug("Data info atom found");
												// dinf: dref
												log.debug("Video dinf children: {}", dinf.getChildren());
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
												log.debug("Video stbl children: {}", stbl.getChildren());
												// stsd - sample description
												// stts - (decoding) time to sample
												// stsc - sample to chunk
												// stsz - sample size
												// stco - chunk offset
												// ctts - (composition) time to sample
												// stss - sync sample
												// sdtp - independent and disposable samples

												//stsd - has codec child
												MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
												if (stsd != null) {
													log.debug("Sample description atom found");
													log.debug("Sample description (video) stsd children: {}", stsd.getChildren());
													MP4Atom avc1 = stsd.lookup(MP4Atom.typeToInt("avc1"), 0);
													if (avc1 != null) {
														log.debug("AVC1 children: {}", avc1.getChildren());
														//set the video codec here - may be avc1 or mp4v
														setVideoCodecId(MP4Atom.intToType(avc1.getType()));
														//video decoder config
														//TODO may need to be generic later
														MP4Atom codecChild = avc1.lookup(MP4Atom.typeToInt("avcC"), 0);
														if (codecChild != null) {
															avcLevel = codecChild.getAvcLevel();
															log.debug("AVC level: {}", avcLevel);
															avcProfile = codecChild.getAvcProfile();
															log.debug("AVC Profile: {}", avcProfile);
															log.debug("AVCC size: {}", codecChild.getSize());
															videoDecoderBytes = codecChild.getVideoConfigBytes();
															log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
														} else {
															//quicktime and ipods use a pixel aspect atom
															//since we have no avcC check for this and avcC may
															//be a child
															MP4Atom pasp = avc1.lookup(MP4Atom.typeToInt("pasp"), 0);
															if (pasp != null) {
																log.debug("PASP children: {}", pasp.getChildren());
																codecChild = pasp.lookup(MP4Atom.typeToInt("avcC"), 0);
																if (codecChild != null) {
																	avcLevel = codecChild.getAvcLevel();
																	log.debug("AVC level: {}", avcLevel);
																	avcProfile = codecChild.getAvcProfile();
																	log.debug("AVC Profile: {}", avcProfile);
																	log.debug("AVCC size: {}", codecChild.getSize());
																	videoDecoderBytes = codecChild.getVideoConfigBytes();
																	log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
																}
															}
														}
													} else {
														//look for mp4v
														MP4Atom mp4v = stsd.lookup(MP4Atom.typeToInt("mp4v"), 0);
														if (mp4v != null) {
															log.debug("MP4V children: {}", mp4v.getChildren());
															//set the video codec here - may be avc1 or mp4v
															setVideoCodecId(MP4Atom.intToType(mp4v.getType()));
															//look for esds 
															MP4Atom codecChild = mp4v.lookup(MP4Atom.typeToInt("esds"), 0);
															if (codecChild != null) {
																//look for descriptors
																MP4Descriptor descriptor = codecChild.getEsd_descriptor();
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
																					videoDecoderBytes = new byte[descr2.getDSID().length - 8];
																					System.arraycopy(descr2.getDSID(), 8, videoDecoderBytes, 0, videoDecoderBytes.length);
																					log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
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
													}
													log.debug("{}", ToStringBuilder.reflectionToString(avc1));
												}
												//stsc - has Records
												MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
												if (stsc != null) {
													log.debug("Sample to chunk atom found");
													videoSamplesToChunks = stsc.getRecords();
													log.debug("Record count: {}", videoSamplesToChunks.size());
													MP4Atom.Record rec = videoSamplesToChunks.firstElement();
													log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
												}
												//stsz - has Samples
												MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
												if (stsz != null) {
													log.debug("Sample size atom found");
													//vector full of integers							
													videoSamples = stsz.getSamples();
													//if sample size is 0 then the table must be checked due
													//to variable sample sizes
													log.debug("Sample size: {}", stsz.getSampleSize());
													videoSampleCount = videoSamples.size();
													log.debug("Sample count: {}", videoSampleCount);
												}
												//stco - has Chunks
												MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
												if (stco != null) {
													log.debug("Chunk offset atom found");
													//vector full of integers
													videoChunkOffsets = stco.getChunks();
													log.debug("Chunk count: {}", videoChunkOffsets.size());
												} else {
													//co64 - has Chunks
													MP4Atom co64 = stbl.lookup(MP4Atom.typeToInt("co64"), 0);
													if (co64 != null) {
														log.debug("Chunk offset (64) atom found");
														//vector full of longs
														videoChunkOffsets = co64.getChunks();
														log.debug("Chunk count: {}", videoChunkOffsets.size());
														// double the timescale for video, since it seems to run at
														// half-speed when co64 is used (seems hacky)
														videoTimeScale = scale * 2.0;
														log.debug("Video time scale: {}", videoTimeScale);
													}
												}
												//stss - has Sync - no sync means all samples are keyframes
												MP4Atom stss = stbl.lookup(MP4Atom.typeToInt("stss"), 0);
												if (stss != null) {
													log.debug("Sync sample atom found");
													//vector full of integers
													syncSamples = stss.getSyncSamples();
													log.debug("Keyframes: {}", syncSamples.size());
												}
												//stts - has TimeSampleRecords
												MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
												if (stts != null) {
													log.debug("Time to sample atom found");
													Vector<MP4Atom.TimeSampleRecord> records = stts.getTimeToSamplesRecords();
													log.debug("Record count: {}", records.size());
													MP4Atom.TimeSampleRecord rec = records.firstElement();
													log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
													//if we have 1 record it means all samples have the same duration
													videoSampleDuration = rec.getSampleDuration();
												}
												//ctts - (composition) time to sample
												MP4Atom ctts = stbl.lookup(MP4Atom.typeToInt("ctts"), 0);
												if (ctts != null) {
													log.debug("Composition time to sample atom found");
													//vector full of integers
													compositionTimes = ctts.getCompositionTimeToSamplesRecords();
													log.debug("Record count: {}", compositionTimes.size());
													if (log.isTraceEnabled()) {
														for (MP4Atom.CompositionTimeSampleRecord rec : compositionTimes) {
															double offset = rec.getSampleOffset();
															if (scale > 0d) {
																offset = (offset / (double) scale) * 1000.0;
																rec.setSampleOffset((int) offset);
															}
															log.trace("Record data: Consecutive samples={} Offset={}", rec.getConsecutiveSamples(), rec.getSampleOffset());
														}
													}
												}
											}
										}
									}
								}
							}
							loops++;
						} while (loops < 3);
						log.trace("Busted out of track loop with {} tracks after {} loops", tracks, loops);
						//calculate FPS
						fps = (videoSampleCount * timeScale) / (double) duration;
						log.debug("FPS calc: ({} * {}) / {}", new Object[] { videoSampleCount, timeScale, duration });
						log.debug("FPS: {}", fps);
						//real duration
						StringBuilder sb = new StringBuilder();
						double videoTime = ((double) duration / (double) timeScale);
						log.debug("Video time: {}", videoTime);
						int minutes = (int) (videoTime / 60);
						if (minutes > 0) {
							sb.append(minutes);
							sb.append('.');
						}
						//formatter for seconds / millis
						NumberFormat df = DecimalFormat.getInstance();
						df.setMaximumFractionDigits(2);
						sb.append(df.format((videoTime % 60)));
						formattedDuration = sb.toString();
						log.debug("Time: {}", formattedDuration);
						break;
					case 1835295092: //mdat
						mdat = atom;
						long dataSize = mdat.getSize();
						log.debug("mdat: {}", ToStringBuilder.reflectionToString(mdat));
						log.debug("Read the mdat atom at: {}", fis.getOffset());
						mdatOffset = fis.getOffset() - dataSize;
						log.debug("File size: {} mdat size: {}", file.length(), dataSize);
						break;
					case 1718773093: //free
					case 2003395685: //wide
						break;
					default:
						log.debug("Unexpected atom: {}", MP4Atom.intToType(atom.getType()));
						FileChannel fc = fis.getChannel();
						ByteBuffer buf = ByteBuffer.allocate(4);
						if (moov != null && mdat == null) {
							// if we have moov but no mdat
							fc.position(moov.getSize());
							log.debug("Search starting at: {}", fc.position());
							String atype = null;
							// search for mdat
							do {
								long pos = fc.position();
								//long offset = fis.getOffset();
								//log.debug("Pos: {} Offset: {}", pos, offset);
								fc.read(buf);
								buf.flip();
								atype = MP4Atom.intToType(buf.getInt());
								//log.debug("Type: {}", atype);
								fc.position(pos + 1);
								buf.clear();
							} while (!atype.equals("mdat"));
							fc.position(fc.position() - 5);
							log.debug("Found the mdat atom at: {}", fc.position());
						}
						if (moov == null && mdat == null) {
							// we're missing both primary atoms at the beginning
							log.debug("Search starting at: {}", fc.position());
							String atype = null;
							// search for moov first
							do {
								long pos = fc.position();
								log.debug("Pos: {}", pos);
								fc.read(buf);
								buf.flip();
								atype = MP4Atom.intToType(buf.getInt());
								//log.debug("Type: {}", atype);
								fc.position(pos + 1);
								buf.clear();
							} while (!atype.equals("moov"));
							fc.position(fc.position() - 5);
							log.debug("Found the moov atom at: {}", fc.position());
						}
						buf.clear();
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

	/**
	 * Get the total readable bytes in a file or IoBuffer.
	 *
	 * @return          Total readable bytes
	 */
	public long getTotalBytes() {
		try {
			return channel.size();
		} catch (Exception e) {
			log.error("Error getTotalBytes", e);
		}
		if (file != null) {
			//just return the file size
			return file.length();
		} else {
			return 0;
		}
	}

	/**
	 * Get the current position in a file or IoBuffer.
	 *
	 * @return           Current position in a file
	 */
	private long getCurrentPosition() {
		try {
			//if we are at the end of the file drop back to mdat offset
			if (channel.position() == channel.size()) {
				log.debug("Reached end of file, going back to data offset");
				channel.position(mdatOffset);
			}
			return channel.position();
		} catch (Exception e) {
			log.error("Error getCurrentPosition", e);
			return 0;
		}
	}

	/** {@inheritDoc} */
	public boolean hasVideo() {
		return hasVideo;
	}

	/**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
	public IoBuffer getFileData() {
		// TODO as of now, return null will disable cache
		// we need to redesign the cache architecture so that
		// the cache is layered underneath FLVReader not above it,
		// thus both tag cache and file cache are feasible.
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
		// XXX should summarize the total bytes read or
		// just the current position?
		return getCurrentPosition();
	}

	/** {@inheritDoc} */
	public long getDuration() {
		return duration;
	}

	public String getVideoCodecId() {
		return videoCodecId;
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
	 * Information mostly from http://www.kaourantin.net/2007/08/what-just-happened-to-video-on-web_20.html
	 * <pre>
		width: Display width in pixels.
		height: Display height in pixels.
		duration: Duration in seconds. But unlike for FLV files this field will always be present.
		videocodecid: Usually a string such as "avc1" or "VP6F", for H.264 we report 'avc1'.
		audiocodecid: Usually a string such as ".mp3" or "mp4a", for AAC we report 'mp4a' and MP3 we report '.mp3'.
	    avcprofile: AVC profile number, values of 66, 77, 88, 100, 110, 122 or 144; which correspond to the H.264 profiles.
	    avclevel: AVC IDC level number, values between 10 and 51.
	    aottype: Either 0, 1 or 2. This corresponds to AAC Main, AAC LC and SBR audio types.
	    moovposition: The offset in bytes of the moov atom in a file.
	    trackinfo: An array of objects containing various infomation about all the tracks in a file
	      ex.
	    	trackinfo[0].length: 7081
	    	trackinfo[0].timescale: 600
	    	trackinfo[0].sampledescription.sampletype: avc1
	    	trackinfo[0].language: und
	    	trackinfo[1].length: 525312
	    	trackinfo[1].timescale: 44100
	    	trackinfo[1].sampledescription.sampletype: mp4a
	    	trackinfo[1].language: und
	    
	    chapters: As mentioned above information about chapters in audiobooks.
		seekpoints: Array that lists the available keyframes in a file as time stamps in milliseconds. 
				This is optional as the MP4 file might not contain this information. Generally speaking, 
				most MP4 files will include this by default. You can directly feed the values into NetStream.seek();
	    videoframerate: The frame rate of the video if a monotone frame rate is used. 
	    		Most videos will have a monotone frame rate.
	    audiosamplerate: The original sampling rate of the audio track.
	    audiochannels: The original number of channels of the audio track.
		progressivedownloadinfo: Object that provides information from the "pdin" atom. This is optional 
				and many files will not have this field.
		tags: Array of key value pairs representing the information present in the "ilst" atom, which is 
				the equivalent of ID3 tags for MP4 files. These tags are mostly used by iTunes. 
	 * </pre>
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
		props.put("width", width);
		props.put("height", height);
		// Video codec id
		props.put("videocodecid", videoCodecId);
		props.put("avcprofile", avcProfile);
		props.put("avclevel", avcLevel);
		props.put("videoframerate", fps);
		// Audio codec id - watch for mp3 instead of aac
		props.put("audiocodecid", audioCodecId);
		props.put("aacaot", audioCodecType);
		props.put("audiosamplerate", audioTimeScale);
		props.put("audiochannels", audioChannels);
		// position of the moov atom
		props.put("moovposition", moovOffset);
		//props.put("chapters", ""); //this is for f4b - books
		if (seekPoints != null) {
			log.debug("Seekpoint list size: {}", seekPoints.size());
			props.put("seekpoints", seekPoints);
		}
		//tags will only appear if there is an "ilst" atom in the file
		//props.put("tags", "");
		List<Map<String, Object>> arr = new ArrayList<Map<String, Object>>(2);
		if (hasAudio) {
			Map<String, Object> audioMap = new HashMap<String, Object>(4);
			audioMap.put("timescale", audioTimeScale);
			audioMap.put("language", "und");

			List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
			audioMap.put("sampledescription", desc);

			Map<String, String> sampleMap = new HashMap<String, String>(1);
			sampleMap.put("sampletype", audioCodecId);
			desc.add(sampleMap);

			if (audioSamples != null) {
				audioMap.put("length_property", audioSampleDuration * audioSamples.size());
				//release some memory, since we're done with the vectors
				audioSamples.clear();
				audioSamples = null;
			}
			arr.add(audioMap);
		}
		if (hasVideo) {
			Map<String, Object> videoMap = new HashMap<String, Object>(3);
			videoMap.put("timescale", videoTimeScale);
			videoMap.put("language", "und");

			List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
			videoMap.put("sampledescription", desc);

			Map<String, String> sampleMap = new HashMap<String, String>(1);
			sampleMap.put("sampletype", videoCodecId);
			desc.add(sampleMap);
			if (videoSamples != null) {
				videoMap.put("length_property", videoSampleDuration * videoSamples.size());
				//release some memory, since we're done with the vectors
				videoSamples.clear();
				videoSamples = null;
			}
			arr.add(videoMap);
		}
		props.put("trackinfo", arr);
		//set this based on existence of seekpoints
		props.put("canSeekToEnd", (seekPoints != null));
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
	 * MetaData, Video config, Audio config, remaining audio and video 
	 * 
	 * Packet prefixes:
	 * 17 00 00 00 00 = Video extra data (first video packet)
	 * 17 01 00 00 00 = Video keyframe
	 * 27 01 00 00 00 = Video interframe
	 * af 00 ...   06 = Audio extra data (first audio packet)
	 * af 01          = Audio frame
	 * 
	 * Audio extra data(s): 
	 * af 00                = Prefix
	 * 11 90 4f 14          = AAC Main   = aottype 0 // 11 90
	 * 12 10                = AAC LC     = aottype 1
	 * 13 90 56 e5 a5 48 00 = HE-AAC SBR = aottype 2
	 * 06                   = Suffix
	 * 
	 * Still not absolutely certain about this order or the bytes - need to verify later
	 */
	private void createPreStreamingTags(int timestamp, boolean clear) {
		log.debug("Creating pre-streaming tags");
		if (clear) {
			firstTags.clear();
		}
		ITag tag = null;
		IoBuffer body = null;
		if (hasVideo) {
			//video tag #1
			body = IoBuffer.allocate(41);
			body.setAutoExpand(true);
			body.put(PREFIX_VIDEO_CONFIG_FRAME); //prefix
			if (videoDecoderBytes != null) {
				//because of other processing we do this check
				if (log.isDebugEnabled()) {
					log.debug("Video decoder bytes: {}", HexDump.byteArrayToHexString(videoDecoderBytes));
					try {
						log.debug("Video bytes data: {}", new String(videoDecoderBytes, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						log.error("", e);
					}
				}
				body.put(videoDecoderBytes);
			}
			tag = new Tag(IoConstants.TYPE_VIDEO, timestamp, body.position(), null, 0);
			body.flip();
			tag.setBody(body);
			//add tag
			firstTags.add(tag);
		}
		// TODO: Handle other mp4 container audio codecs like mp3
		// mp3 header magic number ((int & 0xffe00000) == 0xffe00000) 
		if (hasAudio) {
			//audio tag #1
			if (audioDecoderBytes != null) {
				//because of other processing we do this check
				if (log.isDebugEnabled()) {
					log.debug("Audio decoder bytes: {}", HexDump.byteArrayToHexString(audioDecoderBytes));
					try {
						log.debug("Audio bytes data: {}", new String(audioDecoderBytes, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						log.error("", e);
					}
				}
				body = IoBuffer.allocate(audioDecoderBytes.length + 3);
				body.setAutoExpand(true);
				body.put(new byte[] { (byte) 0xaf, (byte) 0 }); //prefix
				body.put(audioDecoderBytes);
				body.put((byte) 0x06); //suffix
				tag = new Tag(IoConstants.TYPE_AUDIO, timestamp, body.position(), null, 0);
				body.flip();
				tag.setBody(body);
				//add tag
				firstTags.add(tag);
			} else {
				log.info("Audio decoder bytes were not available");
			}
		}
	}

	/**
	 * Packages media data for return to providers
	 */
	public ITag readTag() {
		ITag tag = null;
		try {
			lock.acquire();
			//log.debug("Read tag");
			//empty-out the pre-streaming tags first
			if (!firstTags.isEmpty()) {
				//log.debug("Returning pre-tag");
				// Return first tags before media data
				return firstTags.removeFirst();
			}
			//log.debug("Read tag - sample {} prevFrameSize {} audio: {} video: {}", new Object[]{currentSample, prevFrameSize, audioCount, videoCount});
			//get the current frame
			MP4Frame frame = frames.get(currentFrame);
			if (frame != null) {
				log.debug("Playback #{} {}", currentFrame, frame);
				int sampleSize = frame.getSize();
				int time = (int) Math.round(frame.getTime() * 1000.0);
				//log.debug("Read tag - dst: {} base: {} time: {}", new Object[]{frameTs, baseTs, time});
				long samplePos = frame.getOffset();
				//log.debug("Read tag - samplePos {}", samplePos);
				//determine frame type and packet body padding
				byte type = frame.getType();
				//assume video type
				int pad = 5;
				if (type == TYPE_AUDIO) {
					pad = 2;
				}
				//create a byte buffer of the size of the sample
				ByteBuffer data = ByteBuffer.allocate(sampleSize + pad);
				try {
					//prefix is different for keyframes
					if (type == TYPE_VIDEO) {
						if (frame.isKeyFrame()) {
							//log.debug("Writing keyframe prefix");
							data.put(PREFIX_VIDEO_KEYFRAME);
						} else {
							//log.debug("Writing interframe prefix");
							data.put(PREFIX_VIDEO_FRAME);
						}
						// match the sample with its ctts / mdhd adjustment time
						int timeOffset = prevVideoTS != -1 ? time - prevVideoTS : 0;
						data.put((byte) ((timeOffset >>> 16) & 0xff));
						data.put((byte) ((timeOffset >>> 8) & 0xff));
						data.put((byte) (timeOffset & 0xff));
						if (log.isTraceEnabled()) {
							byte[] prefix = new byte[5];
							int p = data.position();
							data.position(0);
							data.get(prefix);
							data.position(p);
							log.trace("{}", prefix);
						}
						// track video frame count
						videoCount++;
						prevVideoTS = time;
					} else {
						//log.debug("Writing audio prefix");
						data.put(PREFIX_AUDIO_FRAME);
						// track audio frame count
						audioCount++;
					}
					// do we need to add the mdat offset to the sample position?
					channel.position(samplePos);
					// read from the channel
					channel.read(data);
				} catch (IOException e) {
					log.error("Error on channel position / read", e);
				}
				// chunk the data
				IoBuffer payload = IoBuffer.wrap(data.array());
				// create the tag
				tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
				//log.debug("Read tag - type: {} body size: {}", (type == TYPE_AUDIO ? "Audio" : "Video"), tag.getBodySize());
				// increment the frame number
				currentFrame++;
				// set the frame / tag size
				prevFrameSize = tag.getBodySize();
			}
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
		// Maps positions, samples, timestamps to one another
		timePosMap = new HashMap<Integer, Long>();
		samplePosMap = new HashMap<Integer, Long>();
		// tag == sample
		int sample = 1;
		// position
		Long pos = null;
		// if audio-only, skip this
		if (videoSamplesToChunks != null) {
			// handle composite times
			int compositeIndex = 0;
			CompositionTimeSampleRecord compositeTimeEntry = null;
			if (compositionTimes != null && !compositionTimes.isEmpty()) {
				compositeTimeEntry = compositionTimes.remove(0);
			}
			for (int i = 0; i < videoSamplesToChunks.size(); i++) {
				MP4Atom.Record record = videoSamplesToChunks.get(i);
				int firstChunk = record.getFirstChunk();
				int lastChunk = videoChunkOffsets.size();
				if (i < videoSamplesToChunks.size() - 1) {
					MP4Atom.Record nextRecord = videoSamplesToChunks.get(i + 1);
					lastChunk = nextRecord.getFirstChunk() - 1;
				}
				for (int chunk = firstChunk; chunk <= lastChunk; chunk++) {
					int sampleCount = record.getSamplesPerChunk();
					pos = videoChunkOffsets.elementAt(chunk - 1);
					while (sampleCount > 0) {
						//log.debug("Position: {}", pos);
						samplePosMap.put(sample, pos);
						//calculate ts
						double ts = (videoSampleDuration * (sample - 1)) / videoTimeScale;
						//check to see if the sample is a keyframe
						boolean keyframe = false;
						//some files appear not to have sync samples
						if (syncSamples != null) {
							keyframe = syncSamples.contains(sample);
							if (seekPoints == null) {
								seekPoints = new LinkedList<Integer>();
							}
							// get the timestamp
							int frameTs = (int) Math.round(ts * 1000.0);
							// add each key frames timestamp to the seek points list
							if (keyframe) {
								seekPoints.add(frameTs);
							}
							timePosMap.put(frameTs, pos);
						} else {
							log.debug("No sync samples available");
						}
						//size of the sample
						int size = (videoSamples.get(sample - 1)).intValue();
						// exclude data that is not within the mdat box
						if ((moovOffset < mdatOffset && pos > mdatOffset) || (moovOffset > mdatOffset && pos < moovOffset)) {
							//create a frame
							MP4Frame frame = new MP4Frame();
							frame.setKeyFrame(keyframe);
							frame.setOffset(pos);
							frame.setSize(size);
							frame.setTime(ts);
							frame.setType(TYPE_VIDEO);
							//set time offset value from composition records
							if (compositeTimeEntry != null) {
								// how many samples have this offset
								int consecutiveSamples = compositeTimeEntry.getConsecutiveSamples();
								frame.setTimeOffset(compositeTimeEntry.getSampleOffset());
								// increment our count
								compositeIndex++;
								if (compositeIndex - consecutiveSamples == 0) {
									// ensure there are still times available
									if (!compositionTimes.isEmpty()) {
										// get the next one
										compositeTimeEntry = compositionTimes.remove(0);
									}
									// reset
									compositeIndex = 0;
								}
								log.debug("Composite sample #{} {}", sample, frame);
							}
							// add the frame
							frames.add(frame);
							//log.debug("Sample #{} {}", sample, frame);
						} else {
							log.warn("Skipping video frame with invalid position");
						}
						//inc and dec stuff
						pos += size;
						sampleCount--;
						sample++;
					}
				}
			}
			log.debug("Sample position map (video): {}", samplePosMap);
		}
		// if video-only, skip this
		if (audioSamplesToChunks != null) {
			//add the audio frames / samples / chunks		
			sample = 1;
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
						// skip empty AAC data which is 6 bytes long
						log.trace("Audio sample - size: {} pos: {}", size, pos);
						if (size == 6) {
							try {
								// get current pos
								long position = channel.position();
								// jump to data position
								channel.position(pos);
								// create buffer to store bytes so we can check them
								ByteBuffer dst = ByteBuffer.allocate(6);
								// read the data
								channel.read(dst);
								// flip it
								dst.flip();
								// reset the position
								channel.position(position);
								byte[] tmp = dst.array();
								log.trace("Audio bytes: {} equal: {}", HexDump.byteArrayToHexString(tmp), Arrays.equals(EMPTY_AAC, tmp));
								if (Arrays.equals(EMPTY_AAC, tmp)) {
									log.trace("Skipping empty AAC data frame");
									// update counts
									pos += size;
									sampleCount--;
									sample++;
									// read next
									continue;
								}
							} catch (IOException e) {
								log.warn("Exception during audio analysis", e);
							}
						}
						// exclude data that is not within the mdat box
						if ((moovOffset < mdatOffset && pos > mdatOffset) || (moovOffset > mdatOffset && pos < moovOffset)) {
							//create a frame
							MP4Frame frame = new MP4Frame();
							frame.setOffset(pos);
							frame.setSize(size);
							frame.setTime(ts);
							frame.setType(TYPE_AUDIO);
							frames.add(frame);
							//log.debug("Sample #{} {}", sample, frame);
						} else {
							log.warn("Skipping audio frame with invalid position");
						}
						// update counts
						pos += size;
						sampleCount--;
						sample++;
					}
				}
			}
		}
		//sort the frames
		Collections.sort(frames);
		log.debug("Frames count: {}", frames.size());
		//log.debug("Frames: {}", frames);
		//release some memory, since we're done with the vectors
		if (audioSamplesToChunks != null) {
			audioChunkOffsets.clear();
			audioChunkOffsets = null;
			audioSamplesToChunks.clear();
			audioSamplesToChunks = null;
		}
		if (videoSamplesToChunks != null) {
			videoChunkOffsets.clear();
			videoChunkOffsets = null;
			videoSamplesToChunks.clear();
			videoSamplesToChunks = null;
		}
		if (syncSamples != null) {
			syncSamples.clear();
			syncSamples = null;
		}
	}

	/**
	 * Put the current position to pos. The caller must ensure the pos is a valid one.
	 *
	 * @param pos position to move to in file / channel
	 */
	public void position(long pos) {
		log.debug("Position: {}", pos);
		log.debug("Current frame: {}", currentFrame);
		int len = frames.size();
		MP4Frame frame = null;
		for (int f = 0; f < len; f++) {
			frame = frames.get(f);
			long offset = frame.getOffset();
			//look for pos to match frame offset or grab the first keyframe 
			//beyond the offset
			if (pos == offset || (offset > pos && frame.isKeyFrame())) {
				//ensure that it is a keyframe
				if (!frame.isKeyFrame()) {
					log.debug("Frame #{} was not a key frame, so trying again..", f);
					continue;
				}
				log.info("Frame #{} found for seek: {}", f, frame);
				createPreStreamingTags((int) (frame.getTime() * 1000), true);
				currentFrame = f;
				break;
			}
			prevVideoTS = (int) (frame.getTime() * 1000);
		}
		//
		log.debug("Setting current frame: {}", currentFrame);
	}

	/** {@inheritDoc}
	 */
	public void close() {
		log.debug("Close");
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

	public void setVideoCodecId(String videoCodecId) {
		this.videoCodecId = videoCodecId;
	}

	public void setAudioCodecId(String audioCodecId) {
		this.audioCodecId = audioCodecId;
	}

	public ITag readTagHeader() {
		return null;
	}

	@Override
	public KeyFrameMeta analyzeKeyFrames() {
		KeyFrameMeta result = new KeyFrameMeta();
		result.audioOnly = hasAudio && !hasVideo;
		result.duration = duration;
		if (result.audioOnly) {
			result.positions = new long[frames.size()];
			result.timestamps = new int[frames.size()];
			result.audioOnly = true;
			for (int i = 0; i < result.positions.length; i++) {
				frames.get(i).setKeyFrame(true);
				result.positions[i] = frames.get(i).getOffset();
				result.timestamps[i] = (int) Math.round(frames.get(i).getTime() * 1000.0);
			}
		} else {
			result.positions = new long[seekPoints.size()];
			result.timestamps = new int[seekPoints.size()];
			for (int idx = 0; idx < seekPoints.size(); idx++) {
				final Integer ts = seekPoints.get(idx);
				result.positions[idx] = timePosMap.get(ts);
				result.timestamps[idx] = ts;
			}
		}
		return result;
	}

}
