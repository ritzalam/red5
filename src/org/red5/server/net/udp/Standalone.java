package org.red5.server.net.udp;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.DefaultDatagramSessionConfig;

public class Standalone {

	public static final int PORT = 5150;

	public static void main(String[] args) throws Exception {
        IoAcceptor acceptor = new DatagramAcceptor();
        DatagramSessionConfig config = new DefaultDatagramSessionConfig();

        // Bind
        acceptor.setLocalAddress(new InetSocketAddress( PORT ));
        acceptor.setSessionConfig(config);
        acceptor.setHandler(new BasicHandler());
        acceptor.bind();

        System.out.println( "Listening on port " + PORT );

	}

}
