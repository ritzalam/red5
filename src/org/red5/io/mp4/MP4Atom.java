package org.red5.io.mp4;
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

/**
	This software module was originally developed by Apple Computer, Inc. in the 
	course of development of MPEG-4. This software module is an implementation of 
	a part of one or more MPEG-4 tools as specified by MPEG-4. ISO/IEC gives users 
	of MPEG-4 free license to this software module or modifications thereof for 
	use in hardware or software products claiming conformance to MPEG-4. Those 
	intending to use this software module in hardware or software products are 
	advised that its use may infringe existing patents. The original developer of 
	this software module and his/her company, the subsequent editors and their 
	companies, and ISO/IEC have no liability for use of this software module or 
	modifications thereof in an implementation. Copyright is not released for non 
	MPEG-4 conforming products. Apple Computer, Inc. retains full right to use the 
	code for its own purpose, assign or donate the code to a third party and to
	inhibit third parties from using the code for non MPEG-4 conforming products.
	This copyright notice must be included in all copies or	derivative works. 
	Copyright (c) 1999.
*/

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>MP4Atom</code> object represents the smallest information block 
 * of the MP4 file. It could contain other atoms as children.
 * 
 * 01/29/2008 - Added support for avc1 atom (video sample)
 * 02/05/2008 - Added stss - sync sample atom and stts - time to sample atom
 * 10/2008    - Added pasp - pixel aspect ratio atom
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class MP4Atom {

    private static Logger log = LoggerFactory.getLogger(MP4Atom.class);

	/** Constant, the type of the MP4 Atom. */
	public final static int MP4AudioSampleEntryAtomType 			= MP4Atom.typeToInt("mp4a");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4ChunkLargeOffsetAtomType 			= MP4Atom.typeToInt("co64");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4ChunkOffsetAtomType 					= MP4Atom.typeToInt("stco");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4DataInformationAtomType           	= MP4Atom.typeToInt("dinf");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4ESDAtomType                       	= MP4Atom.typeToInt("esds");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4ExtendedAtomType                  	= MP4Atom.typeToInt("uuid");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4HandlerAtomType                   	= MP4Atom.typeToInt("hdlr");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4MediaAtomType                     	= MP4Atom.typeToInt("mdia");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4MediaHeaderAtomType               	= MP4Atom.typeToInt("mdhd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4MediaInformationAtomType          	= MP4Atom.typeToInt("minf");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4MovieAtomType                     	= MP4Atom.typeToInt("moov");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4MovieHeaderAtomType               	= MP4Atom.typeToInt("mvhd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4SampleDescriptionAtomType         	= MP4Atom.typeToInt("stsd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4SampleSizeAtomType                	= MP4Atom.typeToInt("stsz");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4CompactSampleSizeAtomType         	= MP4Atom.typeToInt("stz2");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4SampleTableAtomType               	= MP4Atom.typeToInt("stbl");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4SampleToChunkAtomType             	= MP4Atom.typeToInt("stsc");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4SoundMediaHeaderAtomType         	= MP4Atom.typeToInt("smhd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4TrackAtomType                    	= MP4Atom.typeToInt("trak");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4TrackHeaderAtomType              	= MP4Atom.typeToInt("tkhd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4VideoMediaHeaderAtomType         	= MP4Atom.typeToInt("vmhd");
	/** Constant, the type of the MP4 Atom. */
	public final static int MP4VisualSampleEntryAtomType        	= MP4Atom.typeToInt("mp4v");
	// the type of the avc1 / H.263 
	public final static int MP4VideoSampleEntryAtomType        		= MP4Atom.typeToInt("avc1");
	// contains key frames
	public final static int MP4SyncSampleAtomType         			= MP4Atom.typeToInt("stss");	
	public final static int MP4TimeToSampleAtomType         		= MP4Atom.typeToInt("stts");
	// contains avc properties
	public final static int MP4AVCAtomType							= MP4Atom.typeToInt("avcC");
	// movie data, this ones is not actually parsed
	public final static int MP4MovieDataType                     	= MP4Atom.typeToInt("mdat");
	// pixel aspect ratio
	public final static int MP4PixelAspectAtomType					= MP4Atom.typeToInt("pasp");
	
	/** The size of the atom. */
	protected long size;
	/** The type of the atom. */
	protected int type;
	/** The user's extended type of the atom. */
	protected String uuid;
	/** The amount of bytes that readed from the mpeg stream. */
	protected long readed;  
	/** The children of this atom. */	
	protected List<MP4Atom> children = new ArrayList<MP4Atom>(3);
	
	public MP4Atom(long size, int type, String uuid, long readed) {
		super();
		this.size = size;
		this.type = type;
		this.uuid = uuid;
		this.readed = readed;
	}
	
	/**
	 * Constructs an <code>Atom</code> object from the data in the bitstream.
	 * @param bitstream the input bitstream
	 * @return the constructed atom.
	 */	
	public static MP4Atom createAtom(MP4DataStream bitstream) throws IOException {
		String uuid = null;
		long size = bitstream.readBytes(4);
		if (size == 0) {
			throw new IOException("Invalid size");
		}
		int type = (int)bitstream.readBytes(4);
		long readed = 8;
		if (type == MP4ExtendedAtomType) {
			uuid = bitstream.readString(16);
			readed += 16;
		}
		// large size
		if (size == 1) {
			size = bitstream.readBytes(8);
			readed += 8;
		}
		MP4Atom atom = new MP4Atom(size, type, uuid, readed);
		if((type == MP4MediaAtomType) || (type == MP4DataInformationAtomType) || (type == MP4MovieAtomType)
			|| (type == MP4MediaInformationAtomType) || (type == MP4SampleTableAtomType) || (type == MP4TrackAtomType)) {
			readed = atom.create_composite_atom(bitstream);
		} else if(type == MP4AudioSampleEntryAtomType) {
			readed = atom.create_audio_sample_entry_atom(bitstream);
		} else if(type == MP4ChunkLargeOffsetAtomType) {
			readed = atom.create_chunk_large_offset_atom(bitstream);
		} else if(type == MP4ChunkOffsetAtomType) {
			readed = atom.create_chunk_offset_atom(bitstream);
		} else if(type == MP4HandlerAtomType){
			readed = atom.create_handler_atom(bitstream);
		} else if(type == MP4MediaHeaderAtomType){
			readed = atom.create_media_header_atom(bitstream);
		} else if(type == MP4MovieHeaderAtomType){
			readed = atom.create_movie_header_atom(bitstream);
		} else if(type == MP4SampleDescriptionAtomType){
			readed = atom.create_sample_description_atom(bitstream);
		} else if(type == MP4SampleSizeAtomType){
			readed = atom.create_sample_size_atom(bitstream);
		} else if(type == MP4CompactSampleSizeAtomType){
			readed = atom.create_compact_sample_size_atom(bitstream);
		} else if(type == MP4SampleToChunkAtomType){
			readed = atom.create_sample_to_chunk_atom(bitstream);	
		} else if(type == MP4SyncSampleAtomType){
			readed = atom.create_sync_sample_atom(bitstream);	
		} else if(type == MP4TimeToSampleAtomType){
			readed = atom.create_time_to_sample_atom(bitstream);	
		} else if(type == MP4SoundMediaHeaderAtomType){
			readed = atom.create_sound_media_header_atom(bitstream);
		} else if(type == MP4TrackHeaderAtomType){
			readed = atom.create_track_header_atom(bitstream);
		} else if(type == MP4VideoMediaHeaderAtomType){
			readed = atom.create_video_media_header_atom(bitstream);
		} else if(type == MP4VisualSampleEntryAtomType){
			readed = atom.create_visual_sample_entry_atom(bitstream);
		} else if(type == MP4VideoSampleEntryAtomType){
			readed = atom.create_video_sample_entry_atom(bitstream);
		} else if(type == MP4ESDAtomType) {
			readed = atom.create_esd_atom(bitstream);
		} else if(type == MP4AVCAtomType) {
			readed = atom.create_avc_config_atom(bitstream);
		} else if(type == MP4PixelAspectAtomType) {
			readed = atom.create_pasp_atom(bitstream);
		}
        log.trace("Atom: type = {} size = {}", intToType(type), size);
		bitstream.skipBytes(size - readed);
		return atom;
	}	

	protected int version = 0;
	protected int flags = 0;
	
	/**
	 * Loads the version of the full atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_full_atom(MP4DataStream bitstream) throws IOException {
		long value = bitstream.readBytes(4);
		version = (int)value >> 24;
		flags = (int)value & 0xffffff;
		readed += 4;
		return readed;		
	}

	/**
	 * Loads the composite atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_composite_atom(MP4DataStream bitstream) throws IOException {
		while(readed < size) {
			MP4Atom child = MP4Atom.createAtom(bitstream);
			this.children.add(child);
			readed += child.getSize();
		}
		return readed;		
	}
	
	/**
	 * Lookups for a child atom with the specified <code>type</code>, skips the <code>number</code> 
	 * children with the same type before finding a result. 
	 * @param type the type of the atom.
	 * @param number the number of atoms to skip
	 * @return the atom which was being searched. 
	 */	
	public MP4Atom lookup(long type, int number) {
		int position = 0; 
		for(int i = 0; i < children.size(); i++) {
			MP4Atom atom = children.get(i);
			if(atom.getType() == type) {
				if(position >= number) {
					return atom;
				}
				position++;
			}
		}
		return null;
	}

	private int channelCount = 0;
	
	public int getChannelCount() {
		return channelCount;
	}

	/**
	 * Loads AudioSampleEntry atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_audio_sample_entry_atom(MP4DataStream bitstream) throws IOException {
		//qtff page 117
		log.trace("Audio sample entry");
		bitstream.skipBytes(6);
		int dataReferenceIndex = (int) bitstream.readBytes(2);
		bitstream.skipBytes(8);
		channelCount = (int) bitstream.readBytes(2);
		log.trace("Channels: {}", channelCount);
		sampleSize = (int) bitstream.readBytes(2);
		log.trace("Sample size (bits): {}", sampleSize);
		bitstream.skipBytes(4);
		timeScale = (int) bitstream.readBytes(2);
		log.trace("Time scale: {}", timeScale);
		bitstream.skipBytes(2);
		readed += 28;
		MP4Atom child = MP4Atom.createAtom(bitstream);
		this.children.add(child);
		readed += child.getSize();
		return readed;		
	}
	
	protected int entryCount;
	
	/** The decoding time to sample table. */
	protected Vector<Long> chunks = new Vector<Long>();

	/**
	 * Loads ChunkLargeOffset atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_chunk_large_offset_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		entryCount = (int)bitstream.readBytes(4);
		readed += 8;
		for(int i = 0; i < entryCount; i++) {
			long chunkOffset = bitstream.readBytes(8);
			chunks.addElement(Long.valueOf(chunkOffset));
			readed += 8;
		}
		return readed;		
	}

	public Vector<Long> getChunks() {
		return chunks;
	}

	/**
	 * Loads ChunkOffset atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_chunk_offset_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		entryCount = (int)bitstream.readBytes(4);
		readed += 4;
		for(int i = 0; i < entryCount; i++) {
			long chunkOffset = bitstream.readBytes(4);
			chunks.addElement(Long.valueOf(chunkOffset));
			readed += 4;
		}
		return readed;		
	}

	protected int handlerType;

	/**
	 * Loads Handler atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_handler_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		int qt_componentType = (int)bitstream.readBytes(4);
		handlerType = (int)bitstream.readBytes(4);
		int qt_componentManufacturer = (int)bitstream.readBytes(4);
		int qt_componentFlags = (int)bitstream.readBytes(4);
		int qt_componentFlagsMask = (int)bitstream.readBytes(4);
		readed += 20;
		int length = (int) (size - readed - 1);
		String trackName = bitstream.readString(length);
		log.trace("Track name: {}", trackName);
		readed += length;		
		return readed;		
	}

	/**
	 * Gets the handler type.
	 * @return the handler type.
	 */	
	public int getHandlerType() {
		return handlerType;
	}

	protected Date creationTime;
	protected Date modificationTime;
	protected int timeScale;
	protected long duration;

	/**
	 * Loads MediaHeader atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_media_header_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		if(version == 1) {
			creationTime = createDate(bitstream.readBytes(8));
			modificationTime = createDate(bitstream.readBytes(8));
			timeScale = (int)bitstream.readBytes(4);
			duration = bitstream.readBytes(8);
			readed += 28;
		} else {
			creationTime = createDate(bitstream.readBytes(4));
			modificationTime = createDate(bitstream.readBytes(4));
			timeScale = (int)bitstream.readBytes(4);
			duration = bitstream.readBytes(4);
			readed += 16;
		}
		int packedLanguage = (int)bitstream.readBytes(2);
		int qt_quality = (int)bitstream.readBytes(2);
		readed += 4; 
		return readed;		
	}
	
	public long getDuration() {
		return duration;
	}

	public int getTimeScale() {
		return timeScale;
	}

	/**
	 * Loads MovieHeader atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_movie_header_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		if(version == 1) {
			creationTime = createDate(bitstream.readBytes(8));
			modificationTime = createDate(bitstream.readBytes(8));
			timeScale = (int)bitstream.readBytes(4);
			duration = bitstream.readBytes(8);
			readed += 28;
		} else {
			creationTime = createDate(bitstream.readBytes(4));
			modificationTime = createDate(bitstream.readBytes(4));
			timeScale = (int)bitstream.readBytes(4);
			duration = bitstream.readBytes(4);
			readed += 16;
		}
		int qt_preferredRate = (int)bitstream.readBytes(4);
		int qt_preferredVolume = (int)bitstream.readBytes(2);
		bitstream.skipBytes(10);
		long qt_matrixA = bitstream.readBytes(4);
		long qt_matrixB = bitstream.readBytes(4);
		long qt_matrixU = bitstream.readBytes(4);
		long qt_matrixC = bitstream.readBytes(4);
		long qt_matrixD = bitstream.readBytes(4);
		long qt_matrixV = bitstream.readBytes(4);
		long qt_matrixX = bitstream.readBytes(4);
		long qt_matrixY = bitstream.readBytes(4);
		long qt_matrixW = bitstream.readBytes(4);
		long qt_previewTime = bitstream.readBytes(4);
		long qt_previewDuration = bitstream.readBytes(4);
		long qt_posterTime = bitstream.readBytes(4);
		long qt_selectionTime = bitstream.readBytes(4);
		long qt_selectionDuration = bitstream.readBytes(4);
		long qt_currentTime = bitstream.readBytes(4);
		long nextTrackID = bitstream.readBytes(4);
		readed += 80; 	
		return readed;		
	}

	/**
	 * Loads SampleDescription atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_sample_description_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		entryCount = (int)bitstream.readBytes(4);
		log.trace("stsd entry count: {}", entryCount);
		readed += 4;
		for(int i = 0; i < entryCount; i++) {
			MP4Atom child = MP4Atom.createAtom(bitstream);
			this.children.add(child);
			readed += child.getSize();
		}
		return readed;		
	}

	protected int sampleSize;
	protected int sampleCount;
	
	/** The decoding time to sample table. */
	protected Vector<Integer> samples = new Vector<Integer>();

	/**
	 * Loads MP4SampleSizeAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_sample_size_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		sampleSize = (int)bitstream.readBytes(4);
		sampleCount = (int)bitstream.readBytes(4);
		readed += 8;
		if(sampleSize == 0) {
			for(int i = 0; i < sampleCount; i++) {
				int size = (int)bitstream.readBytes(4);
				samples.addElement(Integer.valueOf(size));
				readed += 4;
			}
		}
		return readed;
	}

	public Vector<Integer> getSamples() {
		return samples;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	protected int fieldSize;
	
	/**
	 * Loads CompactSampleSize atom from the input stream.
	 * @param stream the input stream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_compact_sample_size_atom(MP4DataStream stream) throws IOException {
		create_full_atom(stream);
		stream.skipBytes(3);
		sampleSize = 0;
		fieldSize = (int)stream.readBytes(1);
		sampleCount = (int)stream.readBytes(4);
		readed += 8;
		for(int i = 0; i < sampleCount; i++) {
			int size = 0;
			switch(fieldSize) {
				case 4:
					size = (int)stream.readBytes(1);
					// TODO check the following code
					samples.addElement(Integer.valueOf(size & 0x0f));
					size = (size >> 4) & 0x0f;
					i++;
					readed += 1;
					break;
				case 8:
					size = (int)stream.readBytes(1);
					readed += 1;
					break;
				case 16:
					size = (int)stream.readBytes(2);
					readed += 2;
					break;
			}
			if(i < sampleCount) {
				samples.addElement(Integer.valueOf(size));
			}
		}
		return readed;		
	}

	public static class Record {
		private int firstChunk;
		private int samplesPerChunk;
		private int sampleDescriptionIndex;
		
		public Record(int firstChunk, int samplesPerChunk, int sampleDescriptionIndex) {
			this.firstChunk = firstChunk;
			this.samplesPerChunk = samplesPerChunk;
			this.sampleDescriptionIndex = sampleDescriptionIndex;
		}
		
		public int getFirstChunk() {
			return firstChunk;
		}
		public int getSamplesPerChunk(){
			return samplesPerChunk;
		}
		public int getSampleDescriptionIndex(){
			return sampleDescriptionIndex;
		}
	}

	/** The decoding time to sample table. */
	protected Vector<Record> records = new Vector<Record>();

	public Vector<Record> getRecords() {
		return records;
	}

	/**
	 * Loads MP4SampleToChunkAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_sample_to_chunk_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		entryCount = (int)bitstream.readBytes(4);
		readed += 4;
		for(int i = 0; i < entryCount; i++) {
			int firstChunk = (int)bitstream.readBytes(4);
			int samplesPerChunk = (int)bitstream.readBytes(4);
			int sampleDescriptionIndex = (int)bitstream.readBytes(4);
			records.addElement(new Record(firstChunk, samplesPerChunk, sampleDescriptionIndex));
			readed += 12;
		}
		return readed;		
	}	
	
	protected Vector<Integer> syncSamples = new Vector<Integer>();
	
	public Vector<Integer> getSyncSamples() {
		return syncSamples;
	}
	
	/**
	 * Loads MP4SyncSampleAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_sync_sample_atom(MP4DataStream bitstream) throws IOException {
		log.trace("Sync sample atom contains keyframe info");
		create_full_atom(bitstream);
		entryCount = (int) bitstream.readBytes(4);
		log.trace("Sync entries: {}", entryCount);
		readed += 4;
		for (int i = 0; i < entryCount; i++) {
			int sample = (int) bitstream.readBytes(4);
			//log.trace("Sync entry: {}", sample);
			syncSamples.addElement(Integer.valueOf(sample));
			readed += 4;
		}
		return readed;		
	}
	
	public static class TimeSampleRecord {
		private int consecutiveSamples;
		private int sampleDuration;
		
		public TimeSampleRecord(int consecutiveSamples, int sampleDuration) {
			this.consecutiveSamples = consecutiveSamples;
			this.sampleDuration = sampleDuration;
		}
		
		public int getConsecutiveSamples() {
			return consecutiveSamples;
		}
		public int getSampleDuration(){
			return sampleDuration;
		}
	}
	
	protected Vector<TimeSampleRecord> timeToSamplesRecords = new Vector<TimeSampleRecord>();
	
	public Vector<TimeSampleRecord> getTimeToSamplesRecords() {
		return timeToSamplesRecords;
	}	
	
	/**
	 * Loads MP4TimeToSampleAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_time_to_sample_atom(MP4DataStream bitstream) throws IOException {
		log.trace("Time to sample atom");
		create_full_atom(bitstream);
		entryCount = (int) bitstream.readBytes(4);
		log.trace("Time to sample entries: {}", entryCount);
		readed += 4;
		for (int i = 0; i < entryCount; i++) {
			int sampleCount = (int) bitstream.readBytes(4);
			int sampleDuration = (int) bitstream.readBytes(4);
			//log.trace("Sync entry: {}", sample);
			timeToSamplesRecords.addElement(new TimeSampleRecord(sampleCount, sampleDuration));
			readed += 8;
		}
		return readed;		
	}
	
	protected int balance;

	/**
	 * Loads MP4SoundMediaHeaderAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_sound_media_header_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		balance = (int)bitstream.readBytes(2);
		bitstream.skipBytes(2);
		readed += 4;
		return readed;		
	}

	protected long trackId;
	protected int qt_trackWidth;
	protected int qt_trackHeight;
	
	/**
	 * Loads MP4TrackHeaderAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_track_header_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		log.trace("Version: {}", version);
		if (version == 1) {
			creationTime = createDate(bitstream.readBytes(8));
			modificationTime = createDate(bitstream.readBytes(8));
			trackId = bitstream.readBytes(4);
			bitstream.skipBytes(4);
			duration = bitstream.readBytes(8);
			readed += 32;
		} else {
			creationTime = createDate(bitstream.readBytes(4));
			modificationTime = createDate(bitstream.readBytes(4));
			trackId = bitstream.readBytes(4);
			bitstream.skipBytes(4);
			duration = bitstream.readBytes(4);
			readed += 20;
		}
		bitstream.skipBytes(8); //reserved by apple
		int qt_layer = (int)bitstream.readBytes(2);
		int qt_alternateGroup = (int)bitstream.readBytes(2);
		int qt_volume = (int)bitstream.readBytes(2);
		log.trace("Volume: {}", qt_volume);
		bitstream.skipBytes(2); //reserved by apple
		long qt_matrixA = bitstream.readBytes(4);
		long qt_matrixB = bitstream.readBytes(4);
		long qt_matrixU = bitstream.readBytes(4);
		long qt_matrixC = bitstream.readBytes(4);
		long qt_matrixD = bitstream.readBytes(4);
		long qt_matrixV = bitstream.readBytes(4);
		long qt_matrixX = bitstream.readBytes(4);
		long qt_matrixY = bitstream.readBytes(4);
		long qt_matrixW = bitstream.readBytes(4);
		qt_trackWidth = (int)bitstream.readBytes(4);
		width = (qt_trackWidth >> 16);
		qt_trackHeight = (int)bitstream.readBytes(4);
		height = (qt_trackHeight >> 16);
		readed += 60; 	
		return readed;		
	}
	
	protected int graphicsMode;
	protected int opColorRed;
	protected int opColorGreen;
	protected int opColorBlue;

	/**
	 * Loads MP4VideoMediaHeaderAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_video_media_header_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		if((size - readed) == 8) {
			graphicsMode = (int)bitstream.readBytes(2);
			opColorRed = (int)bitstream.readBytes(2);
			opColorGreen = (int)bitstream.readBytes(2);
			opColorBlue = (int)bitstream.readBytes(2);
			readed += 8;		
		}
		return readed;		
	}

	protected int width;
	protected int height;

	/**
	 * Loads MP4VisualSampleEntryAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_visual_sample_entry_atom(MP4DataStream bitstream) throws IOException {
		log.trace("Visual entry atom contains wxh");
		bitstream.skipBytes(24);
		width = (int)bitstream.readBytes(2);
		log.trace("Width: {}", width);
		height = (int)bitstream.readBytes(2);
		log.trace("Height: {}", height);
		bitstream.skipBytes(50);
		readed += 78;		
		MP4Atom child = MP4Atom.createAtom(bitstream);
		this.children.add(child);
		readed += child.getSize();
		return readed;		
	}
	
	/**
	 * Loads MP4VideoSampleEntryAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	@SuppressWarnings("unused")
	public long create_video_sample_entry_atom(MP4DataStream bitstream) throws IOException {
		log.trace("Video entry atom contains wxh");
		bitstream.skipBytes(6);
		int dataReferenceIndex = (int) bitstream.readBytes(2);		
		bitstream.skipBytes(2);
		bitstream.skipBytes(2);
		bitstream.skipBytes(12);
		width = (int) bitstream.readBytes(2);
		log.trace("Width: {}", width);
		height = (int) bitstream.readBytes(2);
		log.trace("Height: {}", height);
		int horizontalRez = (int) bitstream.readBytes(4) >> 16;
		log.trace("H Resolution: {}", horizontalRez);
		int verticalRez = (int) bitstream.readBytes(4) >> 16;
		log.trace("V Resolution: {}", verticalRez);
		bitstream.skipBytes(4);
		int frameCount = (int) bitstream.readBytes(2);
		log.trace("Frame to sample count: {}", frameCount);
		int stringLen = (int) bitstream.readBytes(1);
		log.trace("String length (cpname): {}", stringLen);
		String compressorName = bitstream.readString(31);
		log.trace("Compressor name: {}", compressorName.trim());
		int depth = (int) bitstream.readBytes(2);
		log.trace("Depth: {}", depth);
		bitstream.skipBytes(2);		
		readed += 78;		
		log.trace("Bytes read: {}", readed);
		MP4Atom child = MP4Atom.createAtom(bitstream);
		this.children.add(child);
		readed += child.getSize();
		return readed;		
	}	

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	protected int avcLevel;
	protected int avcProfile;
	
	public int getAvcLevel() {
		return avcLevel;
	}

	public int getAvcProfile() {
		return avcProfile;
	}

	private byte[] videoConfigBytes;
	
	public byte[] getVideoConfigBytes() {
		return videoConfigBytes;
	}
	
	/**
	 * Loads AVCC atom from the input bitstream.
	 * 
	 * <pre>
                  * 8+ bytes ISO/IEC 14496-10 or 3GPP AVC decode config box
                      = long unsigned offset + long ASCII text string 'avcC'
                    -> 1 byte version = 8-bit hex version  (current = 1)
                    -> 1 byte H.264 profile = 8-bit unsigned stream profile
                    -> 1 byte H.264 compatible profiles = 8-bit hex flags
                    -> 1 byte H.264 level = 8-bit unsigned stream level
                    -> 1 1/2 nibble reserved = 6-bit unsigned value set to 63
                    -> 1/2 nibble NAL length = 2-bit length byte size type
                      - 1 byte = 0 ; 2 bytes = 1 ; 4 bytes = 3
                    -> 1 byte number of SPS = 8-bit unsigned total
                    -> 2+ bytes SPS length = short unsigned length
                    -> + SPS NAL unit = hexdump
                    -> 1 byte number of PPS = 8-bit unsigned total
                    -> 2+ bytes PPS length = short unsigned length
                    -> + PPS NAL unit = hexdump 
	 * </pre>
	 * 
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_avc_config_atom(MP4DataStream bitstream) throws IOException {
		log.trace("AVC config");
		log.trace("Offset: {}", bitstream.getOffset());
		//store the decoder config bytes
		videoConfigBytes = new byte[(int) size];
		for (int b = 0; b < videoConfigBytes.length; b++) {
			videoConfigBytes[b] = (byte) bitstream.readBytes(1);
			
			switch (b) {
				//0 / version
				case 1: //profile
					avcProfile = videoConfigBytes[b];
					log.trace("AVC profile: {}", avcProfile);
					break;
				case 2: //compatible profile
					int avcCompatProfile = videoConfigBytes[b];
					log.trace("AVC compatible profile: {}", avcCompatProfile);
					break;
				case 3: //avc level
					avcLevel = videoConfigBytes[b];
					log.trace("AVC level: {}", avcLevel);
					break;
				case 4: //NAL length
					break;
				case 5: //SPS number
					int numberSPS = videoConfigBytes[b];
					log.trace("Number of SPS: {}", numberSPS);
					break;
				default:
				
			}
			
			readed++;
		}
		return readed;		
	}	
	
	/**
	 * Creates the PASP atom or Pixel Aspect Ratio. It is created by Quicktime
	 * when exporting an MP4 file. The atom is required for ipod's and acts as
	 * a container for the avcC atom in these cases.
	 * 
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.	
	 */
	public long create_pasp_atom(MP4DataStream bitstream) throws IOException {
		log.trace("Pixel aspect ratio");
		int hSpacing = (int) bitstream.readBytes(4);		
		int vSpacing = (int) bitstream.readBytes(4);
		log.trace("hSpacing: {} vSpacing: {}", hSpacing, vSpacing);
		readed += 8;
		MP4Atom child = MP4Atom.createAtom(bitstream);
		this.children.add(child);
		readed += child.getSize();

		return readed;
	}
	
	protected MP4Descriptor esd_descriptor;
	
	/**
	 * Loads M4ESDAtom atom from the input bitstream.
	 * @param bitstream the input bitstream
	 * @return the number of bytes which was being loaded.
	 */
	public long create_esd_atom(MP4DataStream bitstream) throws IOException {
		create_full_atom(bitstream);
		esd_descriptor = MP4Descriptor.createDescriptor(bitstream);
		readed += esd_descriptor.getReaded(); 
		return readed;		
	}

	/**
	 * Returns the ESD descriptor.
	 */
	public MP4Descriptor getEsd_descriptor() {
		return esd_descriptor;
	}

	/**
	 * Converts the time in seconds since midnight 1 Jan 1904 to the <code>Date</code>.  
	 * @param movieTime the time in milliseconds since midnight 1 Jan 1904.
	 * @return the <code>Date</code> object.
	 */
	public static final Date createDate(long movieTime) {
		return new Date(movieTime * 1000 - 2082850791998L); 
	}
	
	public static int typeToInt(String type) {
		int result = (type.charAt(0) << 24) + (type.charAt(1) << 16) + (type.charAt(2) << 8) + type.charAt(3);
		return result;
	}
	
	public static String intToType(int type) {
		StringBuilder st = new StringBuilder();
		st.append((char)((type >> 24) & 0xff)); 
		st.append((char)((type >> 16) & 0xff)); 
		st.append((char)((type >> 8) & 0xff)); 
		st.append((char)(type & 0xff)); 
		return st.toString();
	}
	
	/**
	 * Gets children from this atom.
	 * @return children from this atom.
	 */
	public List<MP4Atom> getChildren() {
		return children;
	}

	/**
	 * Gets the size of this atom.
	 * @return the size of this atom.
	 */
	public long getSize() {
		return size;
	}
	
	/**
	 * Returns the type of this atom.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the name of this atom.
	 */
	public String toString() {
		return intToType(type);
	}

}
