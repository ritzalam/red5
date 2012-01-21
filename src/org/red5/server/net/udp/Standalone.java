/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.udp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

public class Standalone {

	public static final int PORT = 5150;

	public static void main(String[] args) throws Exception {
        IoAcceptor acceptor = new NioDatagramAcceptor();
        //set our handler
        acceptor.setHandler(new BasicHandler());
        //bind
		Set<SocketAddress> addresses = new HashSet<SocketAddress>();			
		addresses.add(new InetSocketAddress(PORT));	
		acceptor.bind(addresses);
		
        System.out.println("Listening on port " + PORT);
	}

}
