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
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.stream.codec.StreamCodecInfo;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;
import org.springframework.core.io.Resource;

public class ClientBroadcastStream extends AbstractClientStream implements
		IClientBroadcastStream, IFilter, IPushableConsumer,
		IPipeConnectionListener, IEventDispatcher {
	
	private static final Log log = LogFactory.getLog(ClientBroadcastStream.class);
	private String publishedName;
	
	private IMessageOutput connMsgOut;
	private VideoCodecFactory videoCodecFactory = null;
	private boolean checkVideoCodec = false;
	private IPipe livePipe;
	private IPipe recordPipe;
	private boolean sendStartNotification = true;
	/** Stores absolute time for video stream. */
	private long audioTime = -1;
	/** Stores absolute time for audio stream. */
	private long videoTime = -1;
	/** Stores absolute time for data stream. */
	private long dataTime = -1;
	private int audioAdd = 0;
	
	private int chunkSize = 0;
	
	public void start() {
		IConsumerService consumerManager =
			(IConsumerService) getScope().getContext().getBean(IConsumerService.KEY);
		try {
			videoCodecFactory = (VideoCodecFactory) getScope().getContext().getBean(VideoCodecFactory.KEY);
			checkVideoCodec = true;
		} catch (Exception err) {
			log.warn("No video codec factory available.", err);
		}
		connMsgOut = consumerManager.getConsumerOutput(this);
		recordPipe = new InMemoryPushPushPipe();
		Map<Object, Object> recordParamMap = new HashMap<Object, Object>();
		recordParamMap.put("record", null);
		recordPipe.subscribe((IProvider) this, recordParamMap);
		setCodecInfo(new StreamCodecInfo());
		sendStartNotify();
		notifyBroadcastStart();
	}

	public void close() {
		if (livePipe != null) {
			livePipe.unsubscribe((IProvider) this);
		}
		recordPipe.unsubscribe((IProvider) this);
		notifyBroadcastClose();
	}

	public void saveAs(String name, boolean isAppend)
			throws ResourceNotFoundException, ResourceExistException {
		try {
			IScope scope = getConnection().getScope();
			IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.KEY, DefaultStreamFilenameGenerator.class);
			
			String filename = generator.generateFilename(scope, name, ".flv");
			Resource res = scope.getResource(filename);
			if (!isAppend && res.exists()) 
				res.getFile().delete();
			
			if (!res.exists()) {
				// Make sure the destination directory exists
				try {
					String path = res.getFile().getAbsolutePath();
					int slashPos = path.lastIndexOf(File.separator);
					if (slashPos != -1)
						path = path.substring(0, slashPos);
					File tmp = new File(path);
					if (!tmp.isDirectory())
						tmp.mkdirs();
				} catch (IOException err) {
					log.error("Could not create destination directory.", err);
				}
				res = scope.getResource(filename);
			}
			
			if (!res.exists())
				res.getFile().createNewFile();
			FileConsumer fc = new FileConsumer(scope, res.getFile());
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			if (isAppend) {
				paramMap.put("mode", "append");
			} else {
				paramMap.put("mode", "record");
			}
			recordPipe.subscribe(fc, paramMap);
		} catch (IOException e) {}
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
			if (setChunkSize.getServiceParamMap() == null)
				setChunkSize.setServiceParamMap(new HashMap());
			setChunkSize.getServiceParamMap().put("chunkSize", chunkSize);
			livePipe.sendOOBControlMessage(getProvider(), setChunkSize);
		}
	}
	
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		if (!"ClientBroadcastStream".equals(oobCtrlMsg.getTarget()))
			return;
		
		if ("chunkSize".equals(oobCtrlMsg.getServiceName())) {
			chunkSize = (Integer) oobCtrlMsg.getServiceParamMap().get("chunkSize");
			notifyChunkSize();
		}
	}
	
	public void dispatchEvent(IEvent event) {
		if (!(event instanceof IRTMPEvent) && (event.getType() != IEvent.Type.STREAM_CONTROL) && (event.getType() != IEvent.Type.STREAM_DATA) && !(event instanceof Notify))
			return;
		
		IStreamCodecInfo codecInfo = getCodecInfo();
		StreamCodecInfo streamCodec = null;
		if (codecInfo instanceof StreamCodecInfo)
			streamCodec = (StreamCodecInfo) codecInfo;
		
		IRTMPEvent rtmpEvent = (IRTMPEvent) event;
		long lastTime = -1;
		long thisTime = -1;
		if (rtmpEvent instanceof AudioData) {
			if (streamCodec != null)
				streamCodec.setHasAudio(true);
			lastTime = audioTime;
			if (audioAdd != 0) {
				rtmpEvent.setTimestamp(rtmpEvent.getTimestamp() + audioAdd);
				rtmpEvent.getHeader().setTimer(rtmpEvent.getHeader().getTimer() + audioAdd);
				audioAdd = 0;
			}
			if (rtmpEvent.getHeader().isTimerRelative())
				audioTime += rtmpEvent.getTimestamp();
			else
				audioTime = rtmpEvent.getTimestamp();
			thisTime = audioTime;
		} else if (rtmpEvent instanceof VideoData) {
			IVideoStreamCodec videoStreamCodec = null;
			if (videoCodecFactory != null && checkVideoCodec) {
				videoStreamCodec = videoCodecFactory.getVideoCodec(((VideoData) rtmpEvent).getData());
				if (codecInfo instanceof StreamCodecInfo) {
					((StreamCodecInfo) codecInfo).setVideoCodec(videoStreamCodec);
				}
				checkVideoCodec = false;
			} else if (codecInfo != null)
				videoStreamCodec = codecInfo.getVideoCodec();
			
			if (videoStreamCodec != null)
				videoStreamCodec.addData(((VideoData) rtmpEvent).getData());
			
			if (streamCodec != null)
				streamCodec.setHasVideo(true);
			IEventListener source = event.getSource();
			if (sendStartNotification) {
				// Notify handler that stream starts publishing
				sendStartNotification = false;
				if (source instanceof IConnection) {
					IScope scope = ((IConnection) source).getScope();
					if (scope.hasHandler()) {
						Object handler = scope.getHandler();
						if (handler instanceof IStreamAwareScopeHandler)
							((IStreamAwareScopeHandler) handler).streamPublishStart(this);
					}
				}
			}
			lastTime = videoTime;
			if (rtmpEvent.getHeader().isTimerRelative())
				videoTime += rtmpEvent.getTimestamp();
			else
				videoTime = rtmpEvent.getTimestamp();
			thisTime = videoTime;
		} else if (rtmpEvent instanceof Notify) {
			lastTime = dataTime;
			if (rtmpEvent.getHeader().isTimerRelative())
				dataTime += rtmpEvent.getTimestamp();
			else
				dataTime = rtmpEvent.getTimestamp();
			thisTime = dataTime;
		}
		
		RTMPMessage msg = new RTMPMessage();
		msg.setBody(rtmpEvent);
		if (!rtmpEvent.getHeader().isTimerRelative() && lastTime != -1) {
			// Make sure we only sent relative timestamps to the subscribers
			int delta = (int) (thisTime - lastTime);
			if (delta < 0) {
				if (rtmpEvent instanceof VideoData)
					// We can't move back the video but advance the audio instead
					audioAdd += -delta;
				// XXX: is this also needed for AudioData? never occured though while testing...
				delta = 0;
			}
			msg.setTimerRelative(true);
			msg.getBody().getHeader().setTimerRelative(true);
			msg.getBody().setTimestamp(delta);
			msg.getBody().getHeader().setTimer(delta);
		} else
			msg.setTimerRelative(rtmpEvent.getHeader().isTimerRelative());
		if (livePipe != null) livePipe.pushMessage(msg);
		recordPipe.pushMessage(msg);
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
			if (event.getProvider() == this &&
					(event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
				this.livePipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (this.livePipe == event.getSource()) {
				sendStopNotify();
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
	
	private void sendStartNotify() {
		Status start = new Status(Status.NS_PUBLISH_START);
		start.setClientid(getStreamId());
		start.setDetails(getPublishedName());
		
		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		connMsgOut.pushMessage(startMsg);
	}
	
	private void sendStopNotify() {
		Status stop = new Status(Status.NS_UNPUBLISHED_SUCCESS);
		stop.setClientid(getStreamId());
		stop.setDetails(getPublishedName());
		
		StatusMessage stopMsg = new StatusMessage();
		stopMsg.setBody(stop);
		connMsgOut.pushMessage(stopMsg);
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
