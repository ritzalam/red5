package org.red5.server.war;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemotableList extends Remote {

	public boolean addChild(WebSettings settings) throws RemoteException;

	public void clearList() throws RemoteException;

	public WebSettings getAt(int index) throws RemoteException;

	public int indexOf(WebSettings settings) throws RemoteException;

	public boolean hasChildren() throws RemoteException;

	public WebSettings removeAt(int index) throws RemoteException;

	public int numChildren() throws RemoteException;

}