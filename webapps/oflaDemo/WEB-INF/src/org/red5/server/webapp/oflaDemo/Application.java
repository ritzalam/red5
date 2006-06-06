package org.red5.server.webapp.oflaDemo;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IScope;

public class Application extends ApplicationAdapter {

	public boolean appStart(IScope app) {
		if (!super.appStart(app))
			return false;
		
		registerServiceHandler("demoService", new DemoService());
		return true;
	}
	
}
