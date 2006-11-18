package org.red5.server.stream;

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IStreamCodecInfo;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IVideoStreamCodec;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
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
import org.springframework.core.io.Resource;

public class ClientBroadcastStream extends AbstractClientStream implements
		IClientBroadcastStream, IFilter, IPushableConsumer,
		IPipeConnectionListener, IEventDispatcher {

	private static final Log log = LogFactory
			.getLog(ClientBroadcastStream.class);

	private String publishedName;

	private IMessageOutput connMsgOut;

	private VideoCodecFactory videoCodecFactory = null;

	private boolean checkVideoCodec = false;

	private IPipe livePipe;

	private IPipe recordPipe;
	
	private boolean recording = false;

	private boolean sendStartNotification = true;

	/** Stores absolute time for video stream. */
	private int audioTime = -1;

	/** Stores absolute time for audio stream. */
	private int videoTime = -1;

	/** Stores absolute time for data stream. */
	private int dataTime = -1;

	/** Stores timestamp of first packet. */
	private int firstTime = -1;

	private int chunkSize = 0;

	public void start() {
		IConsumerService consumerManager = (IConsumerService) getScope()
				.getContext().getBean(IConsumerService.KEY);
		try {
			videoCodecFactory = (VideoCodecFactory) getScope().getContext()
					.getBean(VideoCodecFactory.KEY);
			checkVideoCodec = true;
		} catch (Exception err) {
			log.warn("No video codec factory available.", err);
		}
		firstTime = audioTime = videoTime = dataTime = -1;
		connMsgOut = consumerManager.getConsumerOutput(this);
		recordPipe = new InMemoryPushPushPipe();
		Map<Object, Object> recordParamMap = new HashMap<Object, Object>();
		recordParamMap.put("record", null);
		recordPipe.subscribe((IProvider) this, recordParamMap);
		recording = false;
		setCodecInfo(new StreamCodecInfo());
	}

	public void close() {
		if (livePipe != null) {
			livePipe.unsubscribe((IProvider) this);
		}
		recordPipe.unsubscribe((IProvider) this);
		if (recording)
			sendRecordStopNotify();
		else
			sendPublishStopNotify();
		notifyBroadcastClose();
	}

	public void saveAs(String name, boolean isAppend)
			throws ResourceNotFoundException, ResourceExistException {
		try {
			IScope scope = getConnection().getScope();
			IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils
					.getScopeService(scope, IStreamFilenameGenerator.class,
							DefaultStreamFilenameGenerator.class);

			String filename = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
			Resource res = scope.getContext().getResource(filename);
			if (!isAppend) {
				if (res.exists()) {
					// Per livedoc of FCS/FMS:
					// When "live" or "record" is used,
					// any previously recorded stream with the same stream URI is deleted.
					res.getFile().delete();
				}
			} else {
				if (!res.exists()) {
					// Per livedoc of FCS/FMS:
					// If a recorded stream at the same URI does not already exist,
					// "append" creates the stream as though "record" was passed.
					isAppend = false;
				}
			}

			if (!res.exists()) {
				// Make sure the destination directory exists
				try {
					String path = res.getFile().getAbsolutePath();
					int slashPos = path.lastIndexOf(File.separator);
					if (slashPos != -1) {
						path = path.substring(0, slashPos);
					}
					File tmp = new File(path);
					if (!tmp.isDirectory()) {
						tmp.mkdirs();
					}
				} catch (IOException err) {
					log.error("Could not create destination directory.", err);
				}
				res = scope.getResource(filename);
			}

			if (!res.exists()) {
				res.getFile().createNewFile();
			}
			FileConsumer fc = new FileConsumer(scope, res.getFile());
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			if (isAppend) {
				paramMap.put("mode", "append");
			} else {
				paramMap.put("mode", "record");
			}
			recordPipe.subscribe(fc, paramMap);
			recording = true;
		} catch (IOException e) {
		}
	}

	public IProvider getProvider() {
		return this;
	}

	public String getPublishedName() {
		return publishedName;
	}

	public void setPublishedName(String name) {
		this.publishedName = name;
	}

	public void pushMessage(IPipe pipe, IMessage message) {
	}

	private void notifyChunkSize() {
		if (chunkSize > 0 && livePipe != null) {
			OOBControlMessage setChunkSize = new OOBControlMessage();
			setChunkSize.setTarget("ConnectionConsumer");
			setChunkSize.setServiceName("chunkSize");
			if (setChunkSize.getServiceParamMap() == null) {
				setChunkSize.setServiceParamMap(new HashMap());
			}
			setChunkSize.getServiceParamMap().put("chunkSize", chunkSize);
			livePipe.sendOOBControlMessage(getProvider(), setChunkSize);
		}
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
		if (!"ClientBroadcastStream".equals(oobCtrlMsg.getTarget())) {
			return;
		}

		if ("chunkSize".equals(oobCtrlMsg.getServiceName())) {
			chunkSize = (Integer) oobCtrlMsg.getServiceParamMap().get(
					"chunkSize");
			notifyChunkSize();
		}
	}

	public void dispatchEvent(IEvent event) {
		if (!(event instanceof IRTMPEvent)
				&& (event.getType() != IEvent.Type.STREAM_CONTROL)
				&& (event.getType() != IEvent.Type.STREAM_DATA)
				&& !(event instanceof Notify)) {
			return;
		}

		IStreamCodecInfo codecInfo = getCodecInfo();
		StreamCodecInfo streamCodec = null;
		if (codecInfo instanceof StreamCodecInfo) {
			streamCodec = (StreamCodecInfo) codecInfo;
		}

		IRTMPEvent rtmpEvent = (IRTMPEvent) event;
		int thisTime = -1;
		if (firstTime == -1) {
			firstTime = rtmpEvent.getTimestamp();
		}
		if (rtmpEvent instanceof AudioData) {
			if (streamCodec != null) {
				streamCodec.setHasAudio(true);
			}
			if (rtmpEvent.getHeader().isTimerRelative()) {
				audioTime += rtmpEvent.getTimestamp();
			} else {
				audioTime = rtmpEvent.getTimestamp();
			}
			thisTime = audioTime;
		} else if (rtmpEvent instanceof VideoData) {
			IVideoStreamCodec videoStreamCodec = null;
			if (videoCodecFactory != null && checkVideoCodec) {
				videoStreamCodec = videoCodecFactory
						.getVideoCodec(((VideoData) rtmpEvent).getData());
				if (codecInfo instanceof StreamCodecInfo) {
					((StreamCodecInfo) codecInfo)
							.setVideoCodec(videoStreamCodec);
				}
				checkVideoCodec = false;
			} else if (codecInfo != null) {
				videoStreamCodec = codecInfo.getVideoCodec();
			}

			if (videoStreamCodec != null) {
				videoStreamCodec.addData(((VideoData) rtmpEvent).getData());
			}

			if (streamCodec != null) {
				streamCodec.setHasVideo(true);
			}
			if (rtmpEvent.getHeader().isTimerRelative()) {
				videoTime += rtmpEvent.getTimestamp();
			} else {
				videoTime = rtmpEvent.getTimestamp();
			}
			thisTime = videoTime;
		} else if(rtmpEvent instanceof Invoke) {
			if (rtmpEvent.getHeader().isTimerRelative()) {
				dataTime += rtmpEvent.getTimestamp();
			} else {
				dataTime = rtmpEvent.getTimestamp();
			}
			return;
		} else if (rtmpEvent instanceof Notify) {
			if (rtmpEvent.getHeader().isTimerRelative()) {
				dataTime += rtmpEvent.getTimestamp();
			} else {
				dataTime = rtmpEvent.getTimestamp();
			}
			thisTime = dataTime;
		}
		checkSendNotifications(event);

		RTMPMessage msg = new RTMPMessage();
		msg.setBody(rtmpEvent);
		msg.getBody().setTimestamp(thisTime);
		if (livePipe != null) {
			livePipe.pushMessage(msg);
		}
		recordPipe.pushMessage(msg);
	}

	private void checkSendNotifications(IEvent event) {
		IEventListener source = event.getSource();
		if (sendStartNotification) {
			// Notify handler that stream starts recording/publishing
			sendStartNotification = false;
			if (source instanceof IConnection) {
				IScope scope = ((IConnection) source).getScope();
				if (scope.hasHandler()) {
					Object handler = scope.getHandler();
					if (handler instanceof IStreamAwareScopeHandler) {
						if (recording) {
							((IStreamAwareScopeHandler) handler).streamRecordStart(this);
						} else {
							((IStreamAwareScopeHandler) handler).streamPublishStart(this);
						}
					}
				}
			}
			
			if (recording) {
				sendRecordStartNotify();
			} else
				sendPublishStartNotify();
			notifyBroadcastStart();
		}
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				if (event.getProvider() == this
						&& (event.getParamMap() == null || !event.getParamMap()
								.containsKey("record"))) {
					this.livePipe = (IPipe) event.getSource();
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				if (this.livePipe == event.getSource()) {
					this.livePipe = null;
				}
				break;
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
				if (this.livePipe == event.getSource()) {
					notifyChunkSize();
				}
				break;
			default:
				break;
		}
	}

	private void sendPublishStartNotify() {
		Status start = new Status(StatusCodes.NS_PUBLISH_START);
		start.setClientid(getStreamId());
		start.setDetails(getPublishedName());

		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		connMsgOut.pushMessage(startMsg);
	}

	private void sendPublishStopNotify() {
		Status stop = new Status(StatusCodes.NS_UNPUBLISHED_SUCCESS);
		stop.setClientid(getStreamId());
		stop.setDetails(getPublishedName());

		StatusMessage stopMsg = new StatusMessage();
		stopMsg.setBody(stop);
		connMsgOut.pushMessage(stopMsg);
	}

	private void sendRecordStartNotify() {
		Status start = new Status(StatusCodes.NS_RECORD_START);
		start.setClientid(getStreamId());
		start.setDetails(getPublishedName());

		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		connMsgOut.pushMessage(startMsg);
	}

	private void sendRecordStopNotify() {
		Status start = new Status(StatusCodes.NS_RECORD_STOP);
		start.setClientid(getStreamId());
		start.setDetails(getPublishedName());

		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		connMsgOut.pushMessage(startMsg);
	}

	private void notifyBroadcastStart() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastStart(this);
			} catch (Throwable t) {
				log.error("error notify streamBroadcastStart", t);
			}
		}
	}

	private void notifyBroadcastClose() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastClose(this);
			} catch (Throwable t) {
				log.error("error notify streamBroadcastStop", t);
			}
		}
	}
}
