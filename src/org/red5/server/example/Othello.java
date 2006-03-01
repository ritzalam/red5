package org.red5.server.example;
/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Chris Allen (mrchrisallen@gmail.com)
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import org.red5.server.example.UserListManager;
import org.red5.server.context.Client;
import org.red5.server.context.BaseApplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Othello extends BaseApplication
{

	protected static Log log = LogFactory.getLog(Othello.class.getName());

	private UserListManager userListManager;
	private List clients;
	
	public Othello(){};

	public void startUp() 
	{
		log.info("starting the Othello service...");
	}
	
	public List getUserList()
	{
		return userListManager.getUserList();
	}
	
	public void onAppStart()
	{
		log.info("!!!!!!!!!!!!!!!!!! Othello.onAppStart...");
	}

	public void onAppStop()
	{
		log.debug("!!!!!!!!!!!!!!!!!! Othello.onAppStop...");
	}

	public boolean onConnect(Client client, List params)
	{
		// initialize the userList if not done already
		init();
		
		// get username from the params
		if(params.size() > 0) 
		{
			Object userName = params.get(0);
			// add to the userList
			userListManager.addUser(client, userName.toString());
		}
		
		clients.add(client.toString());
		
		log.debug("!!!!!!!!!!!!!!!!!! onConnect");
		return true;
	}

	public void onDisconnect(Client client) 
	{
		userListManager.removeUser(client);
		//if(clients.size() > 1) removeClient(client);
		log.debug("!!!!!!!!!!!!!!!!!! onDisconnect..." + client);
	}
		
	private void removeClient(Client client)
	{		
		// Removing for now since line 95 causes an error.  Don't know why
		for(Iterator it=clients.iterator(); it.hasNext();)
		{
			Object connectedClient = it.next();
			if(connectedClient.toString().equals(client.toString()))
			{
				clients.remove(connectedClient);
				log.info("removing connectedClient: " + connectedClient + " - " + clients.size());
			}
		}
	}
	
	private void init()
	{
		if(userListManager == null) userListManager = new UserListManager();
		if(clients == null) clients = Collections.synchronizedList(new LinkedList());
	}
}