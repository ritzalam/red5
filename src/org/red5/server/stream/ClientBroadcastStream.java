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
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.statistics.IClientBroadcastStreamStatistics;
import org.red5.server.api.statistics.support.StatisticsCounter;
import org.red5.server.api.stream.IAudioStreamCodec;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamCodecInfo;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.IVideoStreamCodec;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
import org.red5.server.jmx.mxbeans.ClientBroadcastStreamMXBean;
import org.red5.server.messaging.AbstractPipe;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.codec.StreamCodecInfo;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Represents live stream broadcasted from client. As Flash Media Server, Red5 supports
 * recording mode for live streams, that is, broadcasted stream has broadcast mode. It can be either
 * "live" or "record" and latter causes server-side application to record broadcasted stream.
 *
 * Note that recorded streams are recorded as FLV files. The same is correct for audio, because
 * NellyMoser codec that Flash Player uses prohibits on-the-fly transcoding to audio formats like MP3
 * without paying of licensing fee or buying SDK.
 *
 * This type of stream uses two different pipes for live streaming and recording.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
@ManagedResource(objectName = "org.red5.server:type=ClientBroadcastStream", description = "ClientBroadcastStream")
public class ClientBroadcastStream extends AbstractClientStream implements IClientBroadcastStream, IFilter, IPushableConsumer, IPipeConnectionListener, IEventDispatcher,
		IClientBroadcastStreamStatistics, ClientBroadcastStreamMXBean {

	private static final Logger log = LoggerFactory.getLogger(ClientBroadcastStream.class);

	/**
	 * Whether or not to automatically record the associated stream.
	 */
	protected boolean automaticRecording;

	/**
	 * Total number of bytes received.
	 */
	protected long bytesReceived;

	/**
	 * Is there need to check video codec?
	 */
	protected boolean checkVideoCodec = false;

	/**
	 * Is there need to check audio codec?
	 */
	protected boolean checkAudioCodec = false;

	/**
	 * Data is sent by chunks, each of them has size
	 */
	protected int chunkSize;

	/**
	 * Is this stream still active?
	 */
	protected volatile boolean closed;

	/**
	 * Output endpoint that providers use
	 */
	protected IMessageOutput connMsgOut;

	/** 
	 * Stores timestamp of first packet
	 */
	protected long firstPacketTime = -1;

	/**
	 * Pipe for live streaming
	 */
	protected IPipe livePipe;

	/**
	 * Stream published name
	 */
	protected String publishedName;

	/**
	 * Streaming parameters
	 */
	protected Map<String, String> parameters;

	/**
	 * Whether we are recording or not
	 */
	private volatile boolean recording;

	/**
	 * Whether we are appending or not
	 */
	private volatile boolean appending;

	/**
	 * FileConsumer used to output recording to disk
	 */
	private FileConsumer recordingFile;

	/**
	 * The filename we are recording to.
	 */
	private String recordingFilename;

	/**
	 * Pipe for recording
	 */
	private IPipe recordPipe;

	/**
	 * Is there need to send start notification?
	 */
	protected boolean sendStartNotification = true;

	/**
	 * Stores statistics about subscribers.
	 */
	private StatisticsCounter subscriberStats = new StatisticsCounter();

	/** Listeners to get notified about received packets. */
	protected Set<IStreamListener> listeners = new CopyOnWriteArraySet<IStreamListener>();

	protected long latestTimeStamp = -1;

	/**
	 * Check and send notification if necessary
	 * @param event          Event
	 */
	private void checkSendNotifications(IEvent event) {
		IEventListener source = event.getSource();
		sendStartNotifications(source);
	}

	/**
	 * Closes stream, unsubscribes provides, sends stoppage notifications and broadcast close notification.
	 */
	public void close() {
		log.info("Stream close");
		if (closed) {
			// Already closed
			return;
		}
		closed = true;
		if (livePipe != null) {
			livePipe.unsubscribe((IProvider) this);
		}
		if (recordPipe != null) {
			recordPipe.unsubscribe((IProvider) this);
			((AbstractPipe) recordPipe).close();
			recordPipe = null;
		}
		if (recording) {
			sendRecordStopNotify();
		}
		sendPublishStopNotify();
		// TODO: can we sent the client something to make sure he stops sending data?
		connMsgOut.unsubscribe(this);
		notifyBroadcastClose();
		// deregister with jmx
		unregisterJMX();
	}

	/**
	 * Dispatches event
	 * @param event          Event to dispatch
	 */
	public void dispatchEvent(IEvent event) {
		if (event instanceof IRTMPEvent && !closed) {
			switch (event.getType()) {
				case STREAM_CONTROL:
				case STREAM_DATA:
					// create the event
					IRTMPEvent rtmpEvent;
					try {
						rtmpEvent = (IRTMPEvent) event;
					} catch (ClassCastException e) {
						log.error("Class cast exception in event dispatch", e);
						return;
					}
					int eventTime = -1;
					if (log.isTraceEnabled()) {
						// If this is first packet save its timestamp; expect it is
						// absolute? no matter: it's never used!
						if (firstPacketTime == -1) {
							firstPacketTime = rtmpEvent.getTimestamp();
							log.trace(String.format("CBS=@%08x: rtmpEvent=%s creation=%s firstPacketTime=%d", System.identityHashCode(this), rtmpEvent.getClass().getSimpleName(),
									creationTime, firstPacketTime));
						} else {
							log.trace(String.format("CBS=@%08x: rtmpEvent=%s creation=%s firstPacketTime=%d timestamp=%d", System.identityHashCode(this), rtmpEvent.getClass()
									.getSimpleName(), creationTime, firstPacketTime, rtmpEvent.getTimestamp()));
						}

					}
					//get the buffer only once per call
					IoBuffer buf = null;
					if (rtmpEvent instanceof IStreamData && (buf = ((IStreamData<?>) rtmpEvent).getData()) != null) {
						bytesReceived += buf.limit();
					}
					// get stream codec
					IStreamCodecInfo codecInfo = getCodecInfo();
					StreamCodecInfo info = null;
					if (codecInfo instanceof StreamCodecInfo) {
						info = (StreamCodecInfo) codecInfo;
					}
					if (rtmpEvent instanceof AudioData) {
						// SplitmediaLabs - begin AAC fix
						IAudioStreamCodec audioStreamCodec = null;
						if (checkAudioCodec) {
							audioStreamCodec = AudioCodecFactory.getAudioCodec(buf);
							if (info != null) {
								info.setAudioCodec(audioStreamCodec);
							}
							checkAudioCodec = false;
						} else if (codecInfo != null) {
							audioStreamCodec = codecInfo.getAudioCodec();
						}
						if (audioStreamCodec != null) {
							audioStreamCodec.addData(buf.asReadOnlyBuffer());
						}
						if (info != null) {
							info.setHasAudio(true);
						}
						eventTime = rtmpEvent.getTimestamp();
						log.trace("Audio: {}", eventTime);
					} else if (rtmpEvent instanceof VideoData) {
						IVideoStreamCodec videoStreamCodec = null;
						if (checkVideoCodec) {
							videoStreamCodec = VideoCodecFactory.getVideoCodec(buf);
							if (info != null) {
								info.setVideoCodec(videoStreamCodec);
							}
							checkVideoCodec = false;
						} else if (codecInfo != null) {
							videoStreamCodec = codecInfo.getVideoCodec();
						}
						if (videoStreamCodec != null) {
							videoStreamCodec.addData(buf.asReadOnlyBuffer());
						}
						if (info != null) {
							info.setHasVideo(true);
						}
						eventTime = rtmpEvent.getTimestamp();
						log.trace("Video: {}", eventTime);
					} else if (rtmpEvent instanceof Invoke) {
						eventTime = rtmpEvent.getTimestamp();
						//do we want to return from here?
						//event / stream listeners will not be notified of invokes
						return;
					} else if (rtmpEvent instanceof Notify) {
						//TDJ: store METADATA
						Notify notifyEvent = (Notify) rtmpEvent;
						if (metaData == null && notifyEvent.getHeader().getDataType() == Notify.TYPE_STREAM_METADATA) {
							try {
								metaData = notifyEvent.duplicate();
							} catch (Exception e) {
								log.warn("Metadata could not be duplicated for this stream", e);
							}
						}
						eventTime = rtmpEvent.getTimestamp();
					}
					// update last event time
					if (eventTime > latestTimeStamp) {
						latestTimeStamp = eventTime;
					}
					// notify event listeners
					checkSendNotifications(event);
					// note this timestamp is set in event/body but not in the associated header
					try {
						// route to recording
						if (recording) {
							if (recordPipe != null) {
								// get the current size of the buffer / data
								int bufferLimit = buf.limit();
								if (bufferLimit > 0) {
									// make a copy for the record pipe
									buf.mark();
									byte[] buffer = new byte[bufferLimit];
									buf.get(buffer);
									buf.reset();
									// Create new RTMP message, initialize it and push through pipe
									RTMPMessage msg = null;
									if (rtmpEvent instanceof AudioData) {
										AudioData audio = new AudioData(IoBuffer.wrap(buffer));
										audio.setTimestamp(eventTime);
										msg = RTMPMessage.build(audio);
									} else if (rtmpEvent instanceof VideoData) {
										VideoData video = new VideoData(IoBuffer.wrap(buffer));
										video.setTimestamp(eventTime);
										msg = RTMPMessage.build(video);
									} else if (rtmpEvent instanceof Notify) {
										Notify not = new Notify(IoBuffer.wrap(buffer));
										not.setTimestamp(eventTime);
										msg = RTMPMessage.build(not);
									} else {
										log.info("Data was not of A/V type: {}", rtmpEvent.getType());
										msg = RTMPMessage.build(rtmpEvent, eventTime);
									}
									// push it down to the recorder
									recordPipe.pushMessage(msg);
								} else if (bufferLimit == 0 && rtmpEvent instanceof AudioData) {
									log.debug("Stream data size was 0, sending empty audio message");
									// allow for 0 byte audio packets
									AudioData audio = new AudioData(IoBuffer.allocate(0));
									audio.setTimestamp(eventTime);
									RTMPMessage msg = RTMPMessage.build(audio);
									// push it down to the recorder
									recordPipe.pushMessage(msg);
								} else {
									log.debug("Stream data size was 0, recording pipe will not be notified");
								}
							} else {
								log.debug("Record pipe was null, message was not pushed");
							}
						} else {
							log.trace("Recording not active");
						}
						// route to live
						if (livePipe != null) {
							// create new RTMP message, initialize it and push through pipe
							RTMPMessage msg = RTMPMessage.build(rtmpEvent, eventTime);
							livePipe.pushMessage(msg);
						} else {
							log.debug("Live pipe was null, message was not pushed");
						}
					} catch (IOException err) {
						sendRecordFailedNotify(err.getMessage());
						stop();
					}
					// Notify listeners about received packet
					if (rtmpEvent instanceof IStreamPacket) {
						for (IStreamListener listener : getStreamListeners()) {
							try {
								listener.packetReceived(this, (IStreamPacket) rtmpEvent);
							} catch (Exception e) {
								log.error("Error while notifying listener {}", listener, e);
							}
						}
					}
					break;
				default:
					// ignored event
					log.debug("Ignoring event: {}", event.getType());
			}
		} else {
			log.debug("Event was of wrong type or stream is closed ({})", closed);
		}
	}

	/** {@inheritDoc} */
	public int getActiveSubscribers() {
		return subscriberStats.getCurrent();
	}

	/** {@inheritDoc} */
	public long getBytesReceived() {
		return bytesReceived;
	}

	/** {@inheritDoc} */
	public int getCurrentTimestamp() {
		return (int) latestTimeStamp;
	}

	/** {@inheritDoc} */
	public int getMaxSubscribers() {
		return subscriberStats.getMax();
	}

	/**
	 * Getter for provider
	 * @return            Provider
	 */
	public IProvider getProvider() {
		return this;
	}

	/**
	 * Setter for stream published name
	 * @param name       Name that used for publishing. Set at client side when begin to broadcast with NetStream#publish.
	 */
	public void setPublishedName(String name) {
		log.debug("setPublishedName: {}", name);
		// a publish name of "false" is a special case, used when stopping a stream
		if (!"false".equals(name)) {
			this.publishedName = name;
			registerJMX();
		}
	}

	/**
	 * Getter for published name
	 * @return        Stream published name
	 */
	public String getPublishedName() {
		return publishedName;
	}

	/** {@inheritDoc} */
	public void setParameters(Map<String, String> params) {
		this.parameters = params;
	}

	/** {@inheritDoc} */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/** {@inheritDoc} */
	public String getSaveFilename() {
		return recordingFilename;
	}

	/** {@inheritDoc} */
	public IClientBroadcastStreamStatistics getStatistics() {
		return this;
	}

	/** {@inheritDoc} */
	public int getTotalSubscribers() {
		return subscriberStats.getTotal();
	}

	/**
	 * @return the automaticRecording
	 */
	public boolean isAutomaticRecording() {
		return automaticRecording;
	}

	/**
	 * @param automaticRecording the automaticRecording to set
	 */
	public void setAutomaticRecording(boolean automaticRecording) {
		this.automaticRecording = automaticRecording;
	}

	/**
	 *  Notifies handler on stream broadcast stop
	 */
	private void notifyBroadcastClose() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastClose(this);
			} catch (Throwable t) {
				log.error("Error in notifyBroadcastClose", t);
			}
		}
	}

	/**
	 *  Notifies handler on stream broadcast start
	 */
	private void notifyBroadcastStart() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastStart(this);
			} catch (Throwable t) {
				log.error("Error in notifyBroadcastStart", t);
			}
		}
	}

	/**
	 * Send OOB control message with chunk size
	 */
	private void notifyChunkSize() {
		if (chunkSize > 0 && livePipe != null) {
			OOBControlMessage setChunkSize = new OOBControlMessage();
			setChunkSize.setTarget("ConnectionConsumer");
			setChunkSize.setServiceName("chunkSize");
			if (setChunkSize.getServiceParamMap() == null) {
				setChunkSize.setServiceParamMap(new HashMap<String, Object>());
			}
			setChunkSize.getServiceParamMap().put("chunkSize", chunkSize);
			livePipe.sendOOBControlMessage(getProvider(), setChunkSize);
		}
	}

	/**
	 * Out-of-band control message handler
	 *
	 * @param source           OOB message source
	 * @param pipe             Pipe that used to send OOB message
	 * @param oobCtrlMsg       Out-of-band control message
	 */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		String target = oobCtrlMsg.getTarget();
		if ("ClientBroadcastStream".equals(target)) {
			String serviceName = oobCtrlMsg.getServiceName();
			if ("chunkSize".equals(serviceName)) {
				chunkSize = (Integer) oobCtrlMsg.getServiceParamMap().get("chunkSize");
				notifyChunkSize();
			} else {
				log.debug("Unhandled OOB control message for service: {}", serviceName);
			}
		} else {
			log.debug("Unhandled OOB control message to target: {}", target);
		}
	}

	/**
	 * Pipe connection event handler
	 * @param event          Pipe connection event
	 */
	@SuppressWarnings("unused")
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				log.info("Provider connect");
				if (event.getProvider() == this && event.getSource() != connMsgOut && (event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
					this.livePipe = (IPipe) event.getSource();
					log.debug("Provider: {}", this.livePipe.getClass().getName());
					for (IConsumer consumer : this.livePipe.getConsumers()) {
						subscriberStats.increment();
					}
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				log.info("Provider disconnect");
				if (log.isDebugEnabled() && this.livePipe != null) {
					log.debug("Provider: {}", this.livePipe.getClass().getName());
				}
				if (this.livePipe == event.getSource()) {
					this.livePipe = null;
				}
				break;
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
				log.info("Consumer connect");
				IPipe pipe = (IPipe) event.getSource();
				if (log.isDebugEnabled() && pipe != null) {
					log.debug("Consumer: {}", pipe.getClass().getName());
				}
				if (this.livePipe == pipe) {
					notifyChunkSize();
				}
				subscriberStats.increment();
				break;
			case PipeConnectionEvent.CONSUMER_DISCONNECT:
				log.info("Consumer disconnect");
				log.debug("Consumer: {}", event.getSource().getClass().getName());
				subscriberStats.decrement();
				break;
			default:
		}
	}

	/**
	 * Currently not implemented
	 *
	 * @param pipe           Pipe
	 * @param message        Message
	 */
	public void pushMessage(IPipe pipe, IMessage message) {
	}

	/**
	 * Save broadcasted stream.
	 *
	 * @param name                           Stream name
	 * @param isAppend                       Append mode
	 * @throws IOException					 File could not be created/written to.
	 * @throws ResourceNotFoundException     Resource doesn't exist when trying to append.
	 * @throws ResourceExistException        Resource exist when trying to create.
	 */
	public void saveAs(String name, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException {
		log.debug("SaveAs - name: {} append: {}", name, isAppend);
		Map<String, Object> recordParamMap = new HashMap<String, Object>(1);
		//setup record objects
		if (recordPipe == null) {
			recordPipe = new InMemoryPushPushPipe();
			// Clear record flag
			recordParamMap.put("record", null);
			recordPipe.subscribe((IProvider) this, recordParamMap);
			recordParamMap.clear();
		}
		// Get stream scope
		IStreamCapableConnection conn = getConnection();
		if (conn == null) {
			// TODO: throw other exception here?
			throw new IOException("Stream is no longer connected");
		}
		// get connections scope
		IScope scope = conn.getScope();
		// get the file for our filename
		File file = getRecordFile(scope, name);
		// If append mode is on...
		if (!isAppend) {
			if (file.exists()) {
				// when "live" or "record" is used, any previously recorded stream with the same stream URI is deleted.
				if (!file.delete()) {
					throw new IOException(String.format("File: %s could not be deleted", file.getName()));
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
			file.createNewFile();
		}
		//remove existing meta file
		File meta = new File(file.getCanonicalPath() + ".meta");
		if (meta.exists()) {
			log.trace("Meta file exists");
			if (meta.delete()) {
				log.debug("Meta file deleted - {}", meta.getName());
			} else {
				log.warn("Meta file was not deleted - {}", meta.getName());
				meta.deleteOnExit();
			}
		} else {
			log.debug("Meta file does not exist: {}", meta.getCanonicalPath());
		}
		log.debug("Recording file: {}", file.getCanonicalPath());
		// get instance via spring
		if (scope.getContext().hasBean("fileConsumer")) {
			log.debug("Context contains a file consumer");
			recordingFile = (FileConsumer) scope.getContext().getBean("fileConsumer");
			recordingFile.setScope(scope);
			recordingFile.setFile(file);
		} else {
			log.debug("Context does not contain a file consumer, using direct instance");
			// get a new instance
			recordingFile = new FileConsumer(scope, file);
		}
		//get decoder info if it exists for the stream
		IStreamCodecInfo codecInfo = getCodecInfo();
		log.debug("Codec info: {}", codecInfo);
		if (codecInfo instanceof StreamCodecInfo) {
			StreamCodecInfo info = (StreamCodecInfo) codecInfo;
			IVideoStreamCodec videoCodec = info.getVideoCodec();
			log.debug("Video codec: {}", videoCodec);
			if (videoCodec != null) {
				//check for decoder configuration to send
				IoBuffer config = videoCodec.getDecoderConfiguration();
				if (config != null) {
					log.debug("Decoder configuration is available for {}", videoCodec.getName());
					VideoData conf = new VideoData(config.asReadOnlyBuffer());
					try {
						log.debug("Setting decoder configuration for recording");
						recordingFile.setVideoDecoderConfiguration(conf);
					} finally {
						conf.release();
					}
				}
			} else {
				log.debug("Could not initialize stream output, videoCodec is null.");
			}
			// SplitmediaLabs - begin AAC fix
			IAudioStreamCodec audioCodec = info.getAudioCodec();
			log.debug("Audio codec: {}", audioCodec);
			if (audioCodec != null) {
				//check for decoder configuration to send
				IoBuffer config = audioCodec.getDecoderConfiguration();
				if (config != null) {
					log.debug("Decoder configuration is available for {}", audioCodec.getName());
					AudioData conf = new AudioData(config.asReadOnlyBuffer());
					try {
						log.debug("Setting decoder configuration for recording");
						recordingFile.setAudioDecoderConfiguration(conf);
					} finally {
						conf.release();
					}
				}
			} else {
				log.debug("No decoder configuration available, audioCodec is null.");
			}
		}
		if (isAppend) {
			recordParamMap.put("mode", "append");
		} else {
			recordParamMap.put("mode", "record");
		}
		//mark as "recording" only if we get subscribed
		recording = recordPipe.subscribe(recordingFile, recordParamMap);
	}

	/**
	 * Sends publish start notifications
	 */
	private void sendPublishStartNotify() {
		Status publishStatus = new Status(StatusCodes.NS_PUBLISH_START);
		publishStatus.setClientid(getStreamId());
		publishStatus.setDetails(getPublishedName());

		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(publishStatus);
		try {
			connMsgOut.pushMessage(startMsg);
		} catch (IOException err) {
			log.error("Error while pushing message.", err);
		}
	}

	/**
	 *  Sends publish stop notifications
	 */
	private void sendPublishStopNotify() {
		Status stopStatus = new Status(StatusCodes.NS_UNPUBLISHED_SUCCESS);
		stopStatus.setClientid(getStreamId());
		stopStatus.setDetails(getPublishedName());

		StatusMessage stopMsg = new StatusMessage();
		stopMsg.setBody(stopStatus);
		try {
			connMsgOut.pushMessage(stopMsg);
		} catch (IOException err) {
			log.error("Error while pushing message.", err);
		}
	}

	/**
	 *  Sends record failed notifications
	 */
	private void sendRecordFailedNotify(String reason) {
		Status failedStatus = new Status(StatusCodes.NS_RECORD_FAILED);
		failedStatus.setLevel(Status.ERROR);
		failedStatus.setClientid(getStreamId());
		failedStatus.setDetails(getPublishedName());
		failedStatus.setDesciption(reason);

		StatusMessage failedMsg = new StatusMessage();
		failedMsg.setBody(failedStatus);
		try {
			connMsgOut.pushMessage(failedMsg);
		} catch (IOException err) {
			log.error("Error while pushing message.", err);
		}
	}

	/**
	 *  Sends record start notifications
	 */
	private void sendRecordStartNotify() {
		Status recordStatus = new Status(StatusCodes.NS_RECORD_START);
		recordStatus.setClientid(getStreamId());
		recordStatus.setDetails(getPublishedName());

		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(recordStatus);
		try {
			connMsgOut.pushMessage(startMsg);
		} catch (IOException err) {
			log.error("Error while pushing message.", err);
		}
	}

	/**
	 *  Sends record stop notifications
	 */
	private void sendRecordStopNotify() {
		Status stopStatus = new Status(StatusCodes.NS_RECORD_STOP);
		stopStatus.setClientid(getStreamId());
		stopStatus.setDetails(getPublishedName());

		StatusMessage stopMsg = new StatusMessage();
		stopMsg.setBody(stopStatus);
		try {
			connMsgOut.pushMessage(stopMsg);
		} catch (IOException err) {
			log.error("Error while pushing message.", err);
		}
	}

	private void sendStartNotifications(IEventListener source) {
		if (sendStartNotification) {
			// notify handler that stream starts recording/publishing
			sendStartNotification = false;
			if (source instanceof IConnection) {
				IScope scope = ((IConnection) source).getScope();
				if (scope.hasHandler()) {
					Object handler = scope.getHandler();
					if (handler instanceof IStreamAwareScopeHandler) {
						if (recording) {
							// callback for record start
							((IStreamAwareScopeHandler) handler).streamRecordStart(this);
						} else {
							// delete any previously recorded versions of this now "live" stream per
							// http://livedocs.adobe.com/flashmediaserver/3.0/hpdocs/help.html?content=00000186.html
							try {
								File file = getRecordFile(scope, publishedName);							
								if (file != null && file.exists()) {
									if (!file.delete()) {
										log.debug("File was not deleted: {}", file.getAbsoluteFile());
									}
								}
							} catch (Exception e) {
								log.warn("Exception removing previously recorded file", e);
							}
							// callback for publish start
							((IStreamAwareScopeHandler) handler).streamPublishStart(this);
						}
					}
				}
			}
			// send start notifications
			sendPublishStartNotify();
			if (recording) {
				sendRecordStartNotify();
			}
			notifyBroadcastStart();
		}
	}

	/**
	 * Starts stream. Creates pipes, connects
	 */
	public void start() {
		log.info("Stream start");
		IConsumerService consumerManager = (IConsumerService) getScope().getContext().getBean(IConsumerService.KEY);
		checkVideoCodec = true;
		checkAudioCodec = true;
		firstPacketTime = -1;
		latestTimeStamp = -1;
		connMsgOut = consumerManager.getConsumerOutput(this);
		connMsgOut.subscribe(this, null);
		setCodecInfo(new StreamCodecInfo());
		closed = false;
		bytesReceived = 0;
		creationTime = System.currentTimeMillis();
	}

	/** {@inheritDoc} */
	public void startPublishing() {
		// We send the start messages before the first packet is received.
		// This is required so FME actually starts publishing.
		sendStartNotifications(Red5.getConnectionLocal());
		// force recording
		if (automaticRecording && !appending) {
			log.debug("Starting automatic recording of {}", publishedName);
			try {
				saveAs(publishedName, false);
			} catch (Exception e) {
				log.warn("Start of automatic recording failed", e);
			}
		}
	}

	/** {@inheritDoc} */
	public void stop() {
		log.info("Stream stop");
		stopRecording();
		close();
	}

	/**
	 * Stops any currently active recordings.
	 */
	public void stopRecording() {
		if (recording) {
			recording = false;
			appending = false;
			recordingFilename = null;
			recordPipe.unsubscribe(recordingFile);
			sendRecordStopNotify();
			recordPipe = null;
		}
	}

	public boolean isRecording() {
		return recording;
	}

	/** {@inheritDoc} */
	public void addStreamListener(IStreamListener listener) {
		listeners.add(listener);
	}

	/** {@inheritDoc} */
	public Collection<IStreamListener> getStreamListeners() {
		return listeners;
	}

	/** {@inheritDoc} */
	public void removeStreamListener(IStreamListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Get the file we'd be recording to based on scope and given name.
	 * 
	 * @param scope
	 * @param name
	 * @return file
	 */
	protected File getRecordFile(IScope scope, String name) {
		// get stream filename generator
		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
		// generate filename
		recordingFilename = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
		File file = null;
		if (generator.resolvesToAbsolutePath()) {
			file = new File(recordingFilename);
		} else {
            Resource resource = scope.getContext().getResource(recordingFilename);
            if (resource.exists()) {
                try {
                    file = resource.getFile();
                    log.debug("File exists: {} writable: {}", file.exists(), file.canWrite());
                } catch (IOException ioe) {
                    log.error("File error: {}", ioe);
                }
            } else {
                String appScopeName = ScopeUtils.findApplication(scope).getName();
                file = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, recordingFilename));
            }
		}
		return file;
	}

	
	protected void registerJMX() {
		// register with jmx
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName oName = new ObjectName(String.format("org.red5.server:type=ClientBroadcastStream,publishedName=%s", publishedName));
			mbs.registerMBean(new StandardMBean(this, ClientBroadcastStreamMXBean.class, true), oName);
		} catch (InstanceAlreadyExistsException e) {
			log.info("Instance already registered", e);				
		} catch (Exception e) {
			log.warn("Error on jmx registration", e);
		}
	}

	protected void unregisterJMX() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName oName = new ObjectName(String.format("org.red5.server:type=ClientBroadcastStream,publishedName=%s", publishedName));
			mbs.unregisterMBean(oName);
		} catch (Exception e) {
			log.warn("Exception unregistering", e);
		}
	}

}
