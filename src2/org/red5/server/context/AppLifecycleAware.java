package org.red5.server.context;

public interface AppLifecycleAware {
	
	public void onAppStart();

	public void onAppStop();
	
	public boolean onConnect(Client client);

	public void onDisconnect(Client client);

}
