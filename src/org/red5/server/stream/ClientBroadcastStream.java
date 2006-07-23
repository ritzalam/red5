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
	
	private long startTime;
	private long lastAudio;
	private long lastVideo;
	private long lastData;
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
		startTime = System.currentTimeMillis();
		setCodecInfo(new StreamCodecInfo());
		sendStartNotify();
		lastAudio = lastVideo = lastData = startTime;
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
	
	private int videoCounter = 0;
	private int audioCounter = 0;
	private long totalAudio = 0;
	private long totalVideo = 0;
	private int timerAdd = 0;
	
	public void dispatchEvent(IEvent event) {
		if (!(event instanceof IRTMPEvent) && (event.getType() != IEvent.Type.STREAM_CONTROL) && (event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		IStreamCodecInfo codecInfo = getCodecInfo();
		StreamCodecInfo streamCodec = null;
		if (codecInfo instanceof StreamCodecInfo)
			streamCodec = (StreamCodecInfo) codecInfo;
		
		IRTMPEvent rtmpEvent = (IRTMPEvent) event;
		long delta = 0;
		long now = System.currentTimeMillis();
		if (rtmpEvent instanceof AudioData) {
			if (streamCodec != null)
				streamCodec.setHasAudio(true);
			delta = now - lastAudio;
			lastAudio = now;
			IEventListener source = event.getSource();
			if (rtmpEvent.getHeader().isTimerRelative())
				totalAudio += rtmpEvent.getTimestamp();
			else
				totalAudio = rtmpEvent.getTimestamp();
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
			delta = now - lastVideo;
			lastVideo = now;
			if (rtmpEvent.getHeader().isTimerRelative())
				totalVideo += rtmpEvent.getTimestamp();
			else
				totalVideo = rtmpEvent.getTimestamp();
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
		} else {
			delta = now - lastData;
			lastData = now;
		}
		
		// XXX: deltas for the different tag types don't seem to work, investigate!
		delta = now - startTime;
		startTime = now;
		
		//System.err.println("Input:  Audio " + totalAudio + ", Video " + totalVideo + ", Diff " + (totalAudio - totalVideo));
		//rtmpEvent.setTimestamp((int) delta);
		RTMPMessage msg = new RTMPMessage();
		msg.setBody(rtmpEvent);
		if (livePipe != null) {
			// XXX probable race condition here
			boolean send = true;
			if (rtmpEvent instanceof VideoData) {
				// Drop 1 in every 20 disposable interframe video packets, low tech lag fix.
				VideoData.FrameType frameType = ((VideoData) rtmpEvent).getFrameType();
				if (frameType == VideoData.FrameType.DISPOSABLE_INTERFRAME) {
					send = (videoCounter++ % 20) != 0;
					timerAdd += rtmpEvent.getTimestamp();
				}
				
				/*
				if (send && timerAdd > 0) {
					// Adjust timestamp with previously skipped frame
					// TODO: check if this increases lag again...
					rtmpEvent.setTimestamp(rtmpEvent.getTimestamp() + timerAdd);
					timerAdd = 0;
				}
				*/
			} else if (rtmpEvent instanceof AudioData) {
				// Low-tech audio lag fix, decrement timestamp by 1 in 4 of 5 audio packets
				if (audioCounter > 0 && audioCounter % 5 != 0 && rtmpEvent.getTimestamp() > 0) {
					rtmpEvent.setTimestamp(rtmpEvent.getTimestamp() - 1);
				}
				audioCounter++;
			}
			if (send)
				livePipe.pushMessage(msg);
		}
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
