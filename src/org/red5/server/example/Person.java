package org.red5.server.example;
//import org.red5.server.context.Client;
public class Person 
{
	private String clientID;
	private String userName;
	
	public Person(String p_clientID, String p_userName)
	{
		setClientID(p_clientID);
		setUserName(p_userName);
	}
	
	public String getUserName()
	{
		return userName;
	}
	
	public void setUserName(String p_userName)
	{
		userName = p_userName;
	}
	
	public String getClientID()
	{
		return clientID;
	}
	
	public void setClientID(String p_clientID)
	{
		clientID = p_clientID;
	}
}