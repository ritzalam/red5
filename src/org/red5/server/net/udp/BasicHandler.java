package org.red5.server.net.udp;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class BasicHandler extends IoHandlerAdapter {

	protected static Log log = LogFactory.getLog(BasicHandler.class.getName());

	static final int TICK = 1000;
	static final int TIMEOUT = 10000;

	static final byte NOOP = 0x01; // byte
	static final byte LIST = 0x02; // byte, int count, int id, int id, ..
	static final byte JOIN = 0x03; // byte, int id
	static final byte SEND = 0x04; // byte, anything
	static final byte EXIT = 0x05; // byte, int id

	final ByteBuffer NOOP_MSG = ByteBuffer.wrap(new byte[]{NOOP});

	protected Timer timer = new Timer("Timer", true);
	protected Set<IoSession> sessions = Collections.synchronizedSet(new HashSet<IoSession>());
	protected boolean showInfo = false;

	public BasicHandler(){
		timer.scheduleAtFixedRate(new TimeoutTask(), 0, TICK);
		showInfo = log.isInfoEnabled();
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable ex) throws Exception {
		if(showInfo) log.info("Exception: "+session.getRemoteAddress().toString(), ex);
		sessions.remove(session);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		if(showInfo) log.info("Incomming: "+session.getRemoteAddress().toString());
		ByteBuffer data = (ByteBuffer) message;
		final byte type = data.get();
		data.position(0);
		switch(type){
		case NOOP:
			echo(session, data);
			break;
		case LIST:
			list(session);
			break;
		case JOIN:
			sessions.add(session);
			join(session);
			break;
		case SEND:
			broadcast(session, data);
			break;
		case EXIT:
			sessions.remove(session);
			session.close();
			leave(session);
			break;
		default:
			echo(session, data);
			break;
		}
	}

	protected void echo(IoSession session, ByteBuffer data){
		session.write(data);
	}

	protected void broadcast(IoSession exclude, Object message){
		for(IoSession session : sessions){
			if(exclude != null && exclude.equals(session)) continue;
			if(showInfo) log.info("Sending: "+session.getRemoteAddress().toString());
			session.write(message);
		}
	}

	protected void list(IoSession to){
		final int size = 1 + 4 +  (sessions.size()*4);
		ByteBuffer msg = ByteBuffer.allocate(size);
		msg.put(LIST);
		msg.putInt(sessions.size());
		for(IoSession session : sessions){
			msg.putInt(session.getRemoteAddress().hashCode());
		}
		msg.flip();
		to.write(msg);
	}

	protected void leave(IoSession session){
		final int size = 5;
		ByteBuffer msg = ByteBuffer.allocate(size);
		msg.put(EXIT);
		msg.putInt(session.getRemoteAddress().hashCode());
		msg.flip();
		broadcast(null, msg);
	}

	protected void join(IoSession session){
		final int size = 5;
		ByteBuffer msg = ByteBuffer.allocate(size);
		msg.put(JOIN);
		msg.putInt(session.getRemoteAddress().hashCode());
		msg.flip();
		broadcast(null, msg);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		if(showInfo) log.info("Created: "+session.getRemoteAddress().toString());
		sessions.add(session);
		join(session);
		list(session);
	}

	protected class TimeoutTask extends TimerTask {

		public void run(){
			long kill = System.currentTimeMillis() - TIMEOUT;
			LinkedList<IoSession> remove = new LinkedList<IoSession>();
			for(IoSession session : sessions){
				if(session.getLastReadTime() < kill){
					if(showInfo) log.info("Timout: "+session.getRemoteAddress().toString());
					remove.add(session);
				} else {
					session.write(NOOP_MSG);
				}
			}
			if(remove.size() == 0) return;
			for(IoSession session : remove){
				sessions.remove(session);
				session.close();
			}
			for(IoSession session : remove){
				leave(session);
			}
		}

	}

}
