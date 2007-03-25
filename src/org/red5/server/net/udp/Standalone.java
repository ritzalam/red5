package org.red5.server.net.udp;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;

public class Standalone {

	public static final int PORT = 5150;

	public static void main(String[] args) throws Exception {
        IoAcceptor acceptor = new DatagramAcceptor();

        // Bind
        acceptor.bind(new InetSocketAddress( PORT ), new BasicHandler());

        System.out.println( "Listening on port " + PORT );

	}

}
