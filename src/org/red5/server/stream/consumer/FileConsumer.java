package org.red5.server.stream.consumer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.StreamableFileFactory;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.stream.Stream;
import org.red5.server.stream.message.RTMPMessage;
import org.springframework.context.ApplicationContext;

public class FileConsumer implements IPushableConsumer, IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(FileConsumer.class);
	protected static final String FILE_FACTORY = "streamableFileFactory";
	
	private IScope scope;
	private File file;
	private ITagWriter writer;
	private String mode;
	
	public FileConsumer(IScope scope, File file) {
		this.scope = scope;
		this.file = file;
	}
	
	public void pushMessage(IPipe pipe, IMessage message) {
		if (!(message instanceof RTMPMessage)) return;
		if (writer == null) {
			try {
				init();
			} catch (Exception e) {
				log.error("error init file consumer", e);
			}
		}
		RTMPMessage rtmpMsg = (RTMPMessage) message;
		final Message msg = rtmpMsg.getBody();
		ITag tag = new Tag();
		
		tag.setDataType(msg.getDataType());
		tag.setTimestamp(msg.getTimestamp());
		tag.setBodySize(msg.getData().limit());
		tag.setBody(msg.getData());
		
		try {
			writer.writeTag(tag);
		} catch (IOException e) {
			log.error("error writing tag", e);
		}
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub

	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
			Map paramMap = event.getParamMap();
			if (paramMap != null) mode = (String) paramMap.get("mode");
			break;
		case PipeConnectionEvent.CONSUMER_DISCONNECT:
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			// we only support on provider at a time
			// so do releasing when provider disconnects
			uninit();
			break;
		default:
			break;
		}
	}

	private void init() throws IOException {
		IStreamableFileService service = getFileFactory(scope).getService(file);
		IStreamableFile flv = service.getStreamableFile(file);
		if (mode == null || mode.equals(Stream.MODE_RECORD)) {
			writer = flv.getWriter();
		} else if (mode.equals(Stream.MODE_APPEND)) {
			writer = flv.getAppendWriter();
		} else throw new IllegalStateException("illegal mode type: " + mode);
	}
	
	private void uninit() {
		if (writer != null) {
			writer.close();
			writer = null;
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
}
