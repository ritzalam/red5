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
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.springframework.beans.BeansException;
//import org.red5.server.Client;
import org.red5.server.context.AppLifecycleAware;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.core.io.Resource;

/**
 * This service is to be used for John Grden's deomnstration of the Red5
 * prototype presented on Friday October 21st, 2005 at the OFLA Online: The
 * First Online Open Source Flash Conference.
 * 
 * @author Chris Allen mrchrisallen@gmail.com
 * 
 */


public class Othello implements AppLifecycleAware
{

	protected static Log log = LogFactory.getLog(Othello.class.getName());
	
	//protected ApplicationContext appCtx = null;
	//List players = new ArrayList();
	
	public Othello()
	{
		// constructor
		
	}

	public void startUp() {
		log.debug("starting the Othello service...");
	}
	
	public void onAppStart()
	{
		log.debug("!!!!!!!!!!!!!!!!!! Othello.onAppStart...");
	}

	public void onAppStop()
	{
		log.debug("!!!!!!!!!!!!!!!!!! Othello.onAppStop...");
	}

	/*
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		appCtx = context;
	}
	*/
	 
	 /*
	 public List addUser(String p_username)
	 {
		 players.set(0,p_username);
		 return players;
	 }
	 
	 public String getHello()
	 {
		 String msg = "hello from Othello!";
		 return msg;
	 }
	 */

	public boolean onConnect(org.red5.server.context.Client client, List params) 
	{
		log.debug("!!!!!!!!!!!!!!!!!! onConnect..." + client + " Params: " + params);
		return true;
	}

	public void onDisconnect(org.red5.server.context.Client client) 
	{
		log.debug("!!!!!!!!!!!!!!!!!! onDisconnect..." + client);
	}
}