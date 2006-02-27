package org.red5.server.context;

import java.util.List;

public interface AppLifecycleAware {
	
	public void onAppStart();

	public void onAppStop();
	
	public boolean onConnect(Client client, List params);
	
	public void onDisconnect(Client client);

}
