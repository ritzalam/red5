package org.red5.server.example; 
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.List;
import org.red5.server.context.Client;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserListManager { 
    private List userList; 
    private int userCount;
    
    protected static Log log = LogFactory.getLog(UserListManager.class.getName());
    
	private void init() 
	{ 
		if(userList == null) userList = Collections.synchronizedList(new LinkedList());
	}
	
	public synchronized void addUser(Client p_clientID, String p_userName)
	{
		init();
		userCount++;
		
		// create new person
		Person person = new Person(p_clientID.toString(), p_userName);
		userList.add(person);
		
		log.info("User added ::" + p_userName);
		
		// list all people now in the userList
		/*
		for(Iterator it=userList.iterator(); it.hasNext();)
		{
			Person per = (Person) it.next();
			log.info("Person: " + per.getUserName() + " :: " + per.getClientID());
		}
		*/
	}
	
	public synchronized boolean removeUser(Client client)
	{
		
		for(Iterator it=userList.iterator(); it.hasNext();)
		{
			Person per = (Person) it.next();
			if(per.getClientID().equals(client.toString()))
			{
				log.info("Removing User: " + per.getUserName() + " :: " + per.getClientID());
				userList.remove(per);
				break;
			}
			
		}
		return true;
	}
	
	public synchronized List getUserList() 
	{ 
		init();
		return userList; 
	}     
}
