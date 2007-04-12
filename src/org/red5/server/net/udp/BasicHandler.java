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

import sun.misc.HexDumpEncoder;

/**
 *
 * => client send to server
 * <= server send to client
 * << server broadcast
 *
 * Connecting to the server
 *
 * => byte(join)
 * << byte(join) int(id)
 * => byte(list)
 * <= byte(list) int(count) int(id) int(id) ...
 *
 * Sending a message to all
 *
 * => byte(send) [..anything..]
 * << byte(send) [..anything..]
 *
 * Server ping client to keep alive, every second
 *
 * <= byte(noop)
 * => byte(noop)
 *
 * Timeouts (after 10s no reply)
 *
 * << byte(exit) int(id)
 *
 * @author luke
 */
public class BasicHandler extends IoHandlerAdapter {

	protected static Log log = LogFactory.getLog(BasicHandler.class.getName());

	static final int TICK = 1000;
	static final int TIMEOUT = 10000;

	static final byte NOOP = 0x00;
	static final byte JOIN = 0x01;
	static final byte LIST = 0x02;
	static final byte SEND = 0x03;
	static final byte EXIT = 0x04;

	static final HexDumpEncoder dumper = new HexDumpEncoder();

	final ByteBuffer NOOP_MSG = ByteBuffer.wrap(new byte[]{NOOP}).asReadOnlyBuffer();

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

		// IN HEX DUMP
		log.info(dumper.encode(data.asReadOnlyBuffer().buf()));

		final byte type = data.get();
		data.position(0);

		switch(type){
		case NOOP:
			// drop
			break;
		case JOIN:
			if(!sessions.contains(session)){
				sessions.add(session);
				join(session);
			}
			break;
		case LIST:
			list(session);
			break;
		case SEND:
			broadcast(session, data);
			break;
		case EXIT:
			if(sessions.contains(session)){
				sessions.remove(session);
				session.close();
				leave(session);
			}
			break;
		default:
			if(showInfo) log.info("Unknown (play echo): "+session.getRemoteAddress().toString());
			echo(session, data);
			break;
		}
	}

	protected void echo(IoSession session, ByteBuffer data){
		session.write(data);
	}

	protected void broadcast(IoSession exclude, ByteBuffer data){
		for(IoSession session : sessions){
			if(exclude != null && exclude.equals(session)) continue;
			if(showInfo) log.info("Sending: "+session.getRemoteAddress().toString());
			data.acquire();
			session.write(data);
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
					session.write(NOOP_MSG.asReadOnlyBuffer());
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
