package org.red5.server.war;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class RemotableList extends ArrayList<WebSettings> implements
		IRemotableList {

	private final static long serialVersionUID = 419197182007L;

	public boolean addChild(WebSettings settings) throws RemoteException {
		return super.add(settings);
	}

	public void clearList() throws RemoteException {
		super.clear();
	}

	public WebSettings getAt(int index) throws RemoteException {
		return super.get(index);
	}

	public int indexOf(WebSettings settings) throws RemoteException {
		return super.indexOf(settings);
	}

	public boolean hasChildren() throws RemoteException {
		return !super.isEmpty();
	}

	public WebSettings removeAt(int index) throws RemoteException {
		return super.remove(index);
	}

	public int numChildren() throws RemoteException {
		return super.size();
	}

}