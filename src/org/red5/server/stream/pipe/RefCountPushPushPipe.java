package org.red5.server.stream.pipe;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.stream.message.RTMPMessage;

public class RefCountPushPushPipe extends InMemoryPushPushPipe {
	private static final Log log = LogFactory.getLog(RefCountPushPushPipe.class);

	@Override
	public void pushMessage(IMessage message) {
		synchronized (consumers) {
			for (Iterator iter = consumers.iterator(); iter.hasNext(); ) {
				IPushableConsumer consumer = (IPushableConsumer) iter.next();
				try {
					// XXX temporary hack for rtmp message reference counting
					if (message instanceof RTMPMessage) {
						((RTMPMessage) message).acquire();
					}
					consumer.pushMessage(this, message);
				} catch (Throwable t) {
					log.error("exception when pushing message to consumer", t);
				}
			}
		}
	}

}
