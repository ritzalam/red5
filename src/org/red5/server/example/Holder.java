package org.red5.server.example; 
import java.util.HashMap;

public class Holder { 
    private static HashMap userList; 
 
	private static void init() { 
		if (userList == null) userList = new HashMap();
	}
	public synchronized static HashMap getUserList() { 
		init();
		return userList; 
	} 
 
	public synchronized static void setUserList(HashMap userList) { 
		Holder.userList = userList; 
	}      
}
