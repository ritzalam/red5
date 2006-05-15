package org.red5.server.stream.provider;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.StreamableFileFactory;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPullableProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.message.VideoData;
import org.red5.server.stream.ISeekableProvider;
import org.red5.server.stream.message.RTMPMessage;
import org.springframework.context.ApplicationContext;

public class FileProvider
implements IPassive, ISeekableProvider, IPullableProvider, IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(FileProvider.class);
	private static final String FILE_FACTORY = "streamableFileFactory";
	
	public static final String KEY = FileProvider.class.getName();
	
	private IScope scope;
	private File file;
	private IPipe pipe;
	private ITagReader reader;
	private KeyFrameMeta keyFrameMeta = null;
	private int start = 0;
	
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
		Message msg = null;
		switch(tag.getDataType()){
		case Constants.TYPE_AUDIO_DATA:
			msg = new AudioData();
			break;
		case Constants.TYPE_VIDEO_DATA:
			msg = new VideoData();
			break;
		case Constants.TYPE_INVOKE:
			msg = new Invoke();
			break;
		case Constants.TYPE_NOTIFY:
			msg = new Notify();
			break;
		default:
			log.warn("Unexpected type? "+tag.getDataType());
			msg = new Unknown(tag.getDataType());
			break;
		}
		msg.setData(tag.getBody());
		msg.setTimestamp(tag.getTimestamp());
		msg.setSealed(true);
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
		IStreamableFileService service = getFileFactory(scope).getService(file);
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

	private StreamableFileFactory getFileFactory(IScope scope) {
		final IContext context = scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		if (!appCtx.containsBean(FILE_FACTORY))
			return new StreamableFileFactory();
		else
			return (StreamableFileFactory) appCtx.getBean(FILE_FACTORY);
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
		return keyFrameMeta.timestamps[frame];
	}
}
