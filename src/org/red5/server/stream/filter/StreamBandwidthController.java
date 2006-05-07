package org.red5.server.stream.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.PipeConnectionEvent;

public class StreamBandwidthController
implements IFilter, IPipeConnectionListener, Runnable {
	private static final Log log = LogFactory.getLog(StreamBandwidthController.class);
	
	private IPipe providerPipe;
	private IPipe consumerPipe;
	private Thread puller;
	private boolean isStarted;
	
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_CONNECT_PULL:
			if (event.getProvider() != this && providerPipe == null) {
				providerPipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (event.getSource() == providerPipe) {
				providerPipe = null;
			}
			break;
		case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
			if (event.getConsumer() != this && consumerPipe == null) {
				consumerPipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.CONSUMER_DISCONNECT:
			if (event.getSource() == consumerPipe) {
				consumerPipe = null;
			}
			break;
		default:
			break;
		}
	}

	public void run() {
		while (isStarted && providerPipe != null && consumerPipe != null) {
			try {
				IMessage message = providerPipe.pullMessage();
				log.debug("got message: " + message);
				consumerPipe.pushMessage(message);
			} catch (Exception e) {
				break;
			}
		}
		isStarted = false;
	}
	
	public void start() {
		startThread();
	}
	
	public void close() {
		isStarted = false;
	}
	
	synchronized private void startThread() {
		if (!isStarted && providerPipe != null && consumerPipe != null) {
			puller = new Thread(this);
			puller.setDaemon(true);
			isStarted = true;
			puller.start();
		}
	}
}
