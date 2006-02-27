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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.red5.server.example.Holder;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.red5.server.context.BaseApplication;

public class Othello extends BaseApplication
{

	protected static Log log = LogFactory.getLog(Othello.class.getName());
	
	public Othello(){};
	
	public HashMap addUserName(String p_userName) 
	{ 
		HashMap userList = Holder.getUserList();
		userList.put("userName", p_userName);  
		Holder.setUserList(userList);

		log.debug("!!!!!!!!!name added ::" + p_userName);  

		Set set = userList.keySet(); 

		Iterator it = set.iterator();    

		while (it.hasNext())   {        
		   log.debug("name: " + (String) it.next());    
		} 

		return userList;   
     }

	public void startUp() {
		log.info("starting the Othello service...");
	}
	
	public void onAppStart()
	{
		log.info("!!!!!!!!!!!!!!!!!! Othello.onAppStart...");
	}

	public void onAppStop()
	{
		log.debug("!!!!!!!!!!!!!!!!!! Othello.onAppStop...");
	}

	public boolean onConnect(org.red5.server.context.Client client, List params)
	{
		log.debug("!!!!!!!!!!!!!!!!!! onConnect..." + params);
		return true;
	}

	public void onDisconnect(org.red5.server.context.Client client) 
	{
		log.debug("!!!!!!!!!!!!!!!!!! onDisconnect..." + client);
	}
}