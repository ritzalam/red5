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

package org.red5.server.stream;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IKeyFrameMetaCache;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Stream listener for recording stream events to a file.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RecordingListener implements IStreamListener {

	private static final Logger log = LoggerFactory.getLogger(RecordingListener.class);

	/**
	 * Whether we are recording or not
	 */
	private boolean recording;

	/**
	 * Whether we are appending or not
	 */
	private boolean appending;

	/**
	 * FileConsumer used to output recording to disk
	 */
	private FileConsumer recordingConsumer;

	/**
	 * The filename we are recording to.
	 */
	private String fileName;

	/**
	 * Queue to hold incoming stream event packets.
	 */
	private final BlockingQueue<CachedEvent> queue = new LinkedBlockingQueue<CachedEvent>();
	
	/**
	 * Internal worker stop flag.
	 */
	private boolean stop;

	/**
	 * Initialize the listener.
	 * 
	 * @param conn Stream source connection
	 * @param name Stream name
	 * @param isAppend Append mode
	 * @return true if initialization completes and false otherwise
	 */
	public boolean init(IConnection conn, String name, boolean isAppend) {
		// get connections scope
		IScope scope = conn.getScope();
		// get the file for our filename
		File file = getRecordFile(scope, name);
		// If append mode is on...
		if (!isAppend) {
			if (file.exists()) {
				// when "live" or "record" is used, any previously recorded stream with the same stream URI is deleted.
				if (!file.delete()) {
					log.warn("Existing file: {} could not be deleted", file.getName());
					return false;
				}
			}
		} else {
			if (file.exists()) {
				appending = true;
			} else {
				// if a recorded stream at the same URI does not already exist, "append" creates the stream as though "record" was passed.
				isAppend = false;
			}
		}
		// if the file doesn't exist yet, create it
		if (!file.exists()) {
			// Make sure the destination directory exists
			String path = file.getAbsolutePath();
			int slashPos = path.lastIndexOf(File.separator);
			if (slashPos != -1) {
				path = path.substring(0, slashPos);
			}
			File tmp = new File(path);
			if (!tmp.isDirectory()) {
				tmp.mkdirs();
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				log.warn("New recording file could not be created for: {}", file.getName(), e);
				return false;
			}
		}
		if (log.isDebugEnabled()) {
			try {
				log.debug("Recording file: {}", file.getCanonicalPath());
			} catch (IOException e) {
				log.warn("Exception getting file path", e);
			}
		}
		//remove existing meta info
		if (scope.getContext().hasBean("keyframe.cache")) {
			IKeyFrameMetaCache keyFrameCache = (IKeyFrameMetaCache) scope.getContext().getBean("keyframe.cache");
			keyFrameCache.removeKeyFrameMeta(file);
		}
		// get instance via spring
		if (scope.getContext().hasBean("fileConsumer")) {
			log.debug("Context contains a file consumer");
			recordingConsumer = (FileConsumer) scope.getContext().getBean("fileConsumer");
			recordingConsumer.setScope(scope);
			recordingConsumer.setFile(file);
		} else {
			log.debug("Context does not contain a file consumer, using direct instance");
			// get a new instance
			recordingConsumer = new FileConsumer(scope, file);
		}
		// set the mode on the consumer
		if (isAppend) {
			recordingConsumer.setMode("append");
		} else {
			recordingConsumer.setMode("record");
		}		
		// set recording true
		recording = true;
		// since init finished, return true as well
		return true;
	}

	public void start() {
		// start the worker thread
		Thread worker = new Thread(new EventQueueWorker(), "RecorderWorker@" + fileName);
		worker.setDaemon(true);
		worker.start();
	}
	
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		// store everything we would need to perform a write of the stream data
		CachedEvent event = new CachedEvent();
		event.setData(packet.getData().duplicate());
		event.setDataType(packet.getDataType());
		event.setReceivedTime(System.currentTimeMillis());
		event.setTimestamp(packet.getTimestamp());
		// queue the event
		if (!queue.add(event)) {
			log.debug("Event packet not added to recording queue");
		}
	}

	/**
	 * Get the file we'd be recording to based on scope and given name.
	 * 
	 * @param scope
	 * @param name
	 * @return file
	 */
	public static File getRecordFile(IScope scope, String name) {
		// get stream filename generator
		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
		// generate filename
		String fileName = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
		File file = null;
		if (generator.resolvesToAbsolutePath()) {
			file = new File(fileName);
		} else {
			Resource resource = scope.getContext().getResource(fileName);
			if (resource.exists()) {
				try {
					file = resource.getFile();
					log.debug("File exists: {} writable: {}", file.exists(), file.canWrite());
				} catch (IOException ioe) {
					log.error("File error: {}", ioe);
				}
			} else {
				String appScopeName = ScopeUtils.findApplication(scope).getName();
				file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, fileName));
			}
		}
		return file;
	}

	public void closeStream() {
		// flag the worker to stop when queue is empty
		stop = true;
	}

	/**
	 * @return the recording
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * @return the appending
	 */
	public boolean isAppending() {
		return appending;
	}

	/**
	 * @return the recordingConsumer
	 */
	public FileConsumer getFileConsumer() {
		return recordingConsumer;
	}

	/**
	 * @param recordingConsumer the recordingConsumer to set
	 */
	public void setFileConsumer(FileConsumer recordingConsumer) {
		this.recordingConsumer = recordingConsumer;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private class EventQueueWorker implements Runnable {

		public void run() {
			do {
				CachedEvent cachedEvent;
				try {
					IRTMPEvent event = null;
					RTMPMessage message = null;					
					// get first event in the queue
					cachedEvent = queue.take();
					// get the data type
					final byte dataType = cachedEvent.getDataType();
					// get the data
					IoBuffer buffer = cachedEvent.getData();
					// get the current size of the buffer / data
					int bufferLimit = buffer.limit();
					if (bufferLimit > 0) {
						// create new RTMP message and push to the consumer
						switch (dataType) {
							case Constants.TYPE_AGGREGATE:
								event = new Aggregate(buffer);
								event.setTimestamp(cachedEvent.getTimestamp());
								message = RTMPMessage.build(event);
								break;
							case Constants.TYPE_AUDIO_DATA:
								event = new AudioData(buffer);
								event.setTimestamp(cachedEvent.getTimestamp());
								message = RTMPMessage.build(event);
								break;
							case Constants.TYPE_VIDEO_DATA:
								event = new VideoData(buffer);
								event.setTimestamp(cachedEvent.getTimestamp());
								message = RTMPMessage.build(event);
								break;
							default:
								event = new Notify(buffer);
								event.setTimestamp(cachedEvent.getTimestamp());
								message = RTMPMessage.build(event);
								break;						
						}
						// push it down to the recorder
						recordingConsumer.pushMessage(null, message);
					} else if (bufferLimit == 0 && dataType == Constants.TYPE_AUDIO_DATA) {
						log.debug("Stream data size was 0, sending empty audio message");
						// allow for 0 byte audio packets
						event = new AudioData(IoBuffer.allocate(0));
						event.setTimestamp(cachedEvent.getTimestamp());
						message = RTMPMessage.build(event);
						// push it down to the recorder
						recordingConsumer.pushMessage(null, message);
					} else {
						log.debug("Stream data size was 0, recording pipe will not be notified");
					}					
				} catch (InterruptedException e) {
					log.warn("Taking from queue interrupted", e);
				} catch (IOException e) {
					log.warn("Exception while pushing to consumer", e);
				}
				// go to sleep
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
				}
			} while (!stop && !queue.isEmpty());
			// tell the consumer we're done and to complete its work
			recordingConsumer.uninit();
		}

	}

}
