package org.red5.server.stream.provider;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileFactory;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.StreamableFileFactory;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPullableProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.ISeekableProvider;
import org.red5.server.stream.message.RTMPMessage;

public class FileProvider
implements IPassive, ISeekableProvider, IPullableProvider, IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(FileProvider.class);
	
	public static final String KEY = FileProvider.class.getName();
	
	private IScope scope;
	private File file;
	private IPipe pipe;
	private ITagReader reader;
	private KeyFrameMeta keyFrameMeta = null;
	private int start = 0;
	private int lastVideo = 0;
	private int lastAudio = 0;
	private int lastData = 0;
	private int lastUnknown = 0;
	
	public FileProvider(IScope scope, File file) {
		this.scope = scope;
		this.file = file;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public IMessage pullMessage(IPipe pipe) {
		if (this.pipe != pipe) return null;
		if (this.reader == null) init();
		if(!reader.hasMoreTags()) {
			// TODO send OOBCM to notify EOF
			this.pipe.unsubscribe(this);
			return null;
		}
		ITag tag = reader.readTag();
		IRTMPEvent msg = null;
		int delta;
		switch(tag.getDataType()){
		case Constants.TYPE_AUDIO_DATA:
			msg = new AudioData(tag.getBody());
			delta = tag.getTimestamp() - lastAudio;
			lastAudio = tag.getTimestamp();
			break;
		case Constants.TYPE_VIDEO_DATA:
			msg = new VideoData(tag.getBody());
			delta = tag.getTimestamp() - lastVideo;
			lastVideo = tag.getTimestamp();
			break;
		case Constants.TYPE_INVOKE:
			msg = new Invoke(tag.getBody());
			delta = tag.getTimestamp() - lastData;
			lastData = tag.getTimestamp();
			break;
		case Constants.TYPE_NOTIFY:
			msg = new Notify(tag.getBody());
			delta = tag.getTimestamp() - lastData;
			lastData = tag.getTimestamp();
			break;
		default:
			log.warn("Unexpected type? "+tag.getDataType());
			msg = new Unknown(tag.getDataType(), tag.getBody());
			delta = tag.getTimestamp() - lastUnknown;
			lastUnknown = tag.getTimestamp();
			break;
		}
		msg.setTimestamp(delta);
		RTMPMessage rtmpMsg = new RTMPMessage();
		rtmpMsg.setBody(msg);
		return rtmpMsg;
	}

	public IMessage pullMessage(IPipe pipe, long wait) {
		return pullMessage(pipe);
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_CONNECT_PULL:
			if (pipe == null) {
				pipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (pipe == event.getSource()) {
				this.pipe = null;
				uninit();
			}
			break;
		case PipeConnectionEvent.CONSUMER_DISCONNECT:
			if (pipe == event.getSource()) {
				uninit();
			}
		default:
			break;
		}
	}
	
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		if (IPassive.KEY.equals(oobCtrlMsg.getTarget())) {
			if (oobCtrlMsg.getServiceName().equals("init")) {
				Integer startTS = (Integer) oobCtrlMsg.getServiceParamMap().get("startTS");
				setStart(startTS.intValue());
			}
		}
		if (ISeekableProvider.KEY.equals(oobCtrlMsg.getTarget())) {
			if (oobCtrlMsg.getServiceName().equals("seek")) {
				Integer position = (Integer) oobCtrlMsg.getServiceParamMap().get("position");
				seek(position.intValue());
			}
		}
	}

	private void init() {
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.KEY, StreamableFileFactory.class);
		IStreamableFileService service = factory.getService(file);
		if (service == null) {
			log.error("No service found for " + file.getAbsolutePath());
			return;
		}
		try {
			IStreamableFile streamFile = service.getStreamableFile(file);
			reader = streamFile.getReader();
		} catch (IOException e) {
			log.error("error read stream file " + file.getAbsolutePath(), e);
		}
		if (start > 0) {
			seek(start);
		}
	}
	
	private void uninit() {
		if (this.reader != null) {
			this.reader.close();
			this.reader = null;
		}
	}

	synchronized public int seek(int ts) {
		if (keyFrameMeta == null) {
			if (!(reader instanceof IKeyFrameDataAnalyzer))
				// Seeking not supported
				return ts;
		
			keyFrameMeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
		}
		
		if (keyFrameMeta.positions.length == 0) {
			// no video keyframe metainfo, it's an audio-only FLV
			// we skip the seek for now.
			// TODO add audio-seek capability
			return ts;
		}
		int frame = 0;
		for (int i = 0; i < keyFrameMeta.positions.length; i++) {
			if (keyFrameMeta.timestamps[i] > ts) break;
			frame = i;
		}
		reader.position(keyFrameMeta.positions[frame]);
		// TODO: better use distinct timestamps for audio/video/data?
		lastAudio = lastVideo = lastData = lastUnknown = keyFrameMeta.timestamps[frame];
		return keyFrameMeta.timestamps[frame];
	}
}
