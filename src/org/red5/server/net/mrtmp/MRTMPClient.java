package org.red5.server.net.mrtmp;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

public class MRTMPClient implements Runnable {
	private IoHandler ioHandler;
	private IoHandler ioHandlerWrapper;
	private String server;
	private int port;
	private Thread connectThread;
	private boolean needReconnect;
	
	public String getServer() {
		return server;
	}
	public void setServer(String address) {
		this.server = address;
	}
	public IoHandler getIoHandler() {
		return ioHandler;
	}
	public void setIoHandler(IoHandler ioHandler) {
		this.ioHandler = ioHandler;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public void start() {
		needReconnect = true;
		ioHandlerWrapper = new IoHandlerWrapper(ioHandler);
		connectThread = new Thread(this, "MRTMPClient");
		connectThread.setDaemon(true);
		connectThread.start();
	}
	
	public void run() {
		while (true) {
			synchronized (ioHandlerWrapper) {
				if (needReconnect) {
					doConnect();
					needReconnect = false;
				}
				try {
					ioHandlerWrapper.wait();
				} catch (Exception e) {}
			}
		}
	}
	
	private void doConnect() {
		SocketConnector connector = new SocketConnector();
		SocketConnectorConfig config = new SocketConnectorConfig();
		SocketSessionConfig sessionConf =
			(SocketSessionConfig) config.getSessionConfig();
		sessionConf.setTcpNoDelay(true);
		while (true) {
			ConnectFuture future = connector.connect(new InetSocketAddress(server, port), ioHandlerWrapper, config);
			future.join();
			if (future.isConnected()) {
				break;
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {}
		}
	}
	
	private void reconnect() {
		synchronized (ioHandlerWrapper) {
			needReconnect = true;
			ioHandlerWrapper.notifyAll();
		}
	}
	
	private class IoHandlerWrapper implements IoHandler {
		private IoHandler wrapped;
		
		public IoHandlerWrapper(IoHandler wrapped) {
			this.wrapped = wrapped;
		}

		public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
			wrapped.exceptionCaught(session, cause);
			MRTMPClient.this.reconnect();
		}

		public void messageReceived(IoSession session, Object message) throws Exception {
			wrapped.messageReceived(session, message);
		}

		public void messageSent(IoSession session, Object message) throws Exception {
			wrapped.messageSent(session, message);
		}

		public void sessionClosed(IoSession session) throws Exception {
			wrapped.sessionClosed(session);
		}

		public void sessionCreated(IoSession session) throws Exception {
			wrapped.sessionCreated(session);
		}

		public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
			wrapped.sessionIdle(session, status);
		}

		public void sessionOpened(IoSession session) throws Exception {
			wrapped.sessionOpened(session);
		}
	}
}
