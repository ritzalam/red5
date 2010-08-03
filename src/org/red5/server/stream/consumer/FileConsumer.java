package org.red5.server.stream.consumer;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileFactory;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.StreamableFileFactory;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.FlexStreamSend;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer that pushes messages to file. Used when recording live streams.
 */
public class FileConsumer implements Constants, IPushableConsumer, IPipeConnectionListener {
	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(FileConsumer.class);

	/**
	 * Queue to hold data for delayed writing
	 */
	private CopyOnWriteArrayList<QueuedData> queue = new CopyOnWriteArrayList<QueuedData>();
	
	/**
	 * Scope
	 */
	private IScope scope;

	/**
	 * File
	 */
	private File file;

	/**
	 * Tag writer
	 */
	private ITagWriter writer;

	/**
	 * Operation mode
	 */
	private String mode;

	/**
	 * Offset
	 */
	private int offset;

	/**
	 * Last write timestamp
	 */
	private int lastTimestamp;

	/**
	 * Start timestamp
	 */
	private int startTimestamp = -1;
	
	/**
	 * Video decoder configuration
	 */
	private ITag videoConfigurationTag;
	
	/**
	 * Number of queued items needed before writes are initiated
	 */
	private int queueThreshold = 17;

	/**
	 * Whether or not to use a queue for delaying file writes. The queue is useful
	 * for keeping Tag items in their expected order based on their time stamp.
	 */
	private boolean delayWrite = false;

	/**
	 * Creates file consumer
	 * @param scope        Scope of consumer
	 * @param file         File
	 */
	public FileConsumer(IScope scope, File file) {
		this.scope = scope;
		this.file = file;
		// Get the duration from the existing file
		long duration = FLVReader.getDuration(file);
		log.debug("Duration: {}", duration);
		if (duration > 0) {
			offset = (int) duration + 1;
		}
	}

	/**
	 * Push message through pipe
	 * @param pipe         Pipe
	 * @param message      Message to push
	 * @throws IOException if message could not be written
	 */
	public void pushMessage(IPipe pipe, IMessage message) throws IOException {
		if (message instanceof RTMPMessage) {
    		final IRTMPEvent msg = ((RTMPMessage) message).getBody();
    		// get the timestamp
    		int timestamp = msg.getTimestamp();
    		log.debug("Timestamp: {}", timestamp);
    		// if we're dealing with a FlexStreamSend IRTMPEvent, this avoids relative timestamp calculations
    		if (!(msg instanceof FlexStreamSend)) {
    			log.trace("Not FlexStreamSend type");
    			lastTimestamp = timestamp;
    		}    
			//initialize a writer
			if (writer == null) {
    			init();
    		}
    		// if writes are delayed, queue the data and sort it by time
			if (!delayWrite) {		
				write(timestamp, msg);
			} else {
        		QueuedData queued = null;
        		if (msg instanceof IStreamData) {
        			queued = new QueuedData(timestamp, msg.getDataType(), ((IStreamData) msg).getData().asReadOnlyBuffer());
        		} else {
        			//XXX what type of message are we saving that has no body data??
        			log.debug("Non-stream data, body not saved. Type: {}", msg.getClass().getName());
        			queued = new QueuedData(timestamp, msg.getDataType());			
        		}        		
        		//add to the queue
        		queue.add(queued);
        		//when we reach the threshold, spawn a write thread
        		if (queue.size() >= queueThreshold) {
        			Thread worker = new Thread() {
        				public void run() {
        					log.trace("Spawning queue writer thread");
        					doWrites();
        				}
        			};
        			worker.setDaemon(true);
        			worker.start();
        		}
			}
		} else if (message instanceof ResetMessage) {
			startTimestamp = -1;
			offset += lastTimestamp;
		} 
	}

	/**
	 * Out-of-band control message handler
	 *
	 * @param source            Source of message
	 * @param pipe              Pipe that is used to transmit OOB message
	 * @param oobCtrlMsg        OOB control message
	 */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
	}

	/**
	 * Pipe connection event handler
	 * @param event       Pipe connection event
	 */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
				if (event.getConsumer() != this) {
					break;
				}
				Map<String, Object> paramMap = event.getParamMap();
				if (paramMap != null) {
					mode = (String) paramMap.get("mode");
				}
				break;
			case PipeConnectionEvent.CONSUMER_DISCONNECT:
				if (event.getConsumer() != this) {
					break;
				}
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				// we only support one provider at a time
				// so do releasing when provider disconnects
				uninit();
				break;
			default:
				break;
		}
	}

	/**
	 * Initialization
	 *
	 * @throws IOException          I/O exception
	 */
	private void init() throws IOException {
		log.debug("Init");
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope,
				IStreamableFileFactory.class, StreamableFileFactory.class);
		File folder = file.getParentFile();
		if (!folder.exists()) {
			if (!folder.mkdirs()) {
				throw new IOException("Could not create parent folder");
			}
		}
		if (!file.isFile()) {
			// Maybe the (previously existing) file has been deleted
			file.createNewFile();
		} else if (!file.canWrite()) {
			throw new IOException("The file is read-only");
		}
		IStreamableFileService service = factory.getService(file);
		IStreamableFile flv = service.getStreamableFile(file);
		if (mode == null || mode.equals(IClientStream.MODE_RECORD)) {
			writer = flv.getWriter();
			//write the decoder config tag if it exists
			if (videoConfigurationTag != null) {
				writer.writeTag(videoConfigurationTag);
				videoConfigurationTag = null;
			}
		} else if (mode.equals(IClientStream.MODE_APPEND)) {
			writer = flv.getAppendWriter();
		} else {
			throw new IllegalStateException("Illegal mode type: " + mode);
		}
	}

	/**
	 * Reset
	 */
	private void uninit() {		
		log.debug("Uninit");
		if (writer != null) {
			//write all the queued items
			doWrites();
			//close the writer
			writer.close();
			writer = null;
		}
		//clear the queue
		queue.clear();
		queue = null;
		//clear file ref
		file = null;
	}
	
	/**
	 * Write all the queued items to the writer.
	 */
	public final synchronized void doWrites() {
		//get the current items in the queue
		QueuedData[] tmp = queue.toArray(new QueuedData[0]);
		queue.removeAll(Arrays.asList(tmp));
		//sort
		Arrays.sort(tmp);
		//empty the queue
		for (QueuedData queued : tmp) {
			write(queued);
		}		
		//clear and null-out
		tmp = null;
	}
	
	/**
	 * Write incoming data to the file.
	 * 
	 * @param timestamp adjusted timestamp
	 * @param msg stream data
	 */
	private final void write(int timestamp, IRTMPEvent msg) {
		byte dataType = msg.getDataType();
		log.debug("Write - timestamp: {} type: {}", timestamp, dataType);	
		//if the last message was a reset or we just started, use the header timer
		if (startTimestamp == -1) {
			startTimestamp = timestamp;
			timestamp = 0;
		} else {
			timestamp -= startTimestamp;
		}
		// create a tag
		ITag tag = new Tag();
		tag.setDataType(dataType);
		//Always add offset since it's needed for "append" publish mode
		//It adds on disk flv file duration
		//Search for "offset" in this class constructor
		tag.setTimestamp(timestamp + offset);
		// get data bytes
		IoBuffer data = ((IStreamData) msg).getData();
		if (data != null) {
			tag.setBodySize(data.limit());
			tag.setBody(data);					
			try {
				if (timestamp >= 0) {
					if (!writer.writeTag(tag)) {
						log.warn("Tag was not written");
					}
				} else {
					log.warn("Skipping message with negative timestamp.");	
				}
			} catch (IOException e) {
				log.error("Error writing tag", e);
			} finally {
				if (data != null) {
					data.clear();
					data.free();
				}
			}
			data = null;
		}		
	}

	/**
	 * Adjust timestamp and write to the file.
	 * 
	 * @param queued queued data for write
	 */
	private final void write(QueuedData queued) {
		//get timestamp
		int timestamp = queued.getTimestamp();
		log.debug("Write - timestamp: {} type: {}", timestamp, queued.getDataType());	
		//if the last message was a reset or we just started, use the header timer
		if (startTimestamp == -1) {
			startTimestamp = timestamp;
			timestamp = 0;
		} else {
			timestamp -= startTimestamp;
		}
		// create a tag
		ITag tag = new Tag();
		tag.setDataType(queued.getDataType());
		//Always add offset since it's needed for "append" publish mode
		//It adds on disk flv file duration
		//Search for "offset" in this class constructor
		tag.setTimestamp(timestamp + offset);
		// get queued
		IoBuffer data = queued.getData();
		if (data != null) {
			tag.setBodySize(data.limit());
			tag.setBody(data);					
			try {
				if (timestamp >= 0) {
					if (!writer.writeTag(tag)) {
						log.warn("Tag was not written");
					}
				} else {
					log.warn("Skipping message with negative timestamp.");	
				}
			} catch (IOException e) {
				log.error("Error writing tag", e);
			} finally {
				if (data != null) {
					data.clear();
					data.free();
				}
			}
			data = null;
		}		
		queued = null;
	}	
	
	/**
	 * Sets a video decoder configuration; some codecs require this, such as AVC.
	 * 
	 * @param decoderConfig video codec configuration
	 */
	public void setVideoDecoderConfiguration(IRTMPEvent decoderConfig) {
		videoConfigurationTag = new Tag();
		videoConfigurationTag.setDataType(decoderConfig.getDataType());
		videoConfigurationTag.setTimestamp(0);
		if (decoderConfig instanceof IStreamData) {
			IoBuffer data = ((IStreamData) decoderConfig).getData().asReadOnlyBuffer();
			videoConfigurationTag.setBodySize(data.limit());
			videoConfigurationTag.setBody(data);
		}
	}

	/**
	 * Sets the threshold for the queue. When the threshold is met a worker is spawned
	 * to empty the sorted queue to the writer.
	 * 
	 * @param queueThreshold number of items to queue before spawning worker
	 */
	public void setQueueThreshold(int queueThreshold) {
		this.queueThreshold = queueThreshold;
	}

	public int getQueueThreshold() {
		return queueThreshold;
	}	
	
	private final static class QueuedData implements Comparable<QueuedData> {
		final int timestamp;
		final byte dataType;
		final IoBuffer data;

		QueuedData(int timestamp, byte dataType) {
			this.timestamp = timestamp;
			this.dataType = dataType;
			this.data = null;
		}		
		
		QueuedData(int timestamp, byte dataType, IoBuffer data) {
			this.timestamp = timestamp;
			this.dataType = dataType;
			this.data = IoBuffer.allocate(data.limit());
			byte[] copy = new byte[data.limit()];
			data.get(copy);
			this.data.put(copy);
			this.data.flip();
		}

		public int getTimestamp() {
			return timestamp;
		}

		public byte getDataType() {
			return dataType;
		}

		public IoBuffer getData() {
			return data;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + dataType;
			result = prime * result + timestamp;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			QueuedData other = (QueuedData) obj;
			if (dataType != other.dataType) {
				return false;
			}
			if (timestamp != other.timestamp) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(QueuedData other) {
			if (timestamp > other.timestamp) {
				return 1;
			} else if (timestamp < other.timestamp) {
				return -1;
			}
			return 0;
		}
		
	}	
	
}