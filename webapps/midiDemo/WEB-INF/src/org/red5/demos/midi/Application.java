package org.red5.demos.midi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.midi.SharedMidiObject;

public class Application extends ApplicationAdapter {
	
	private static final Log log = LogFactory.getLog(Application.class);
	
	protected SharedMidiObject midiSo;
	
	@Override
	public boolean appStart(IScope app) {
		log.info("Midi demo app started");
		// TODO Auto-generated method stub
		return super.appStart(app);
	}

	public boolean connectMidiToSO(String deviceName, String soName){
		log.info("Connecting midi device: "+deviceName+" to SO: "+soName);
		ISharedObject so = getSharedObject(getScope(), soName, true);
		midiSo = new SharedMidiObject(deviceName, so);
		boolean success = midiSo.connect();
		if(success) log.info("It worked!");
		else log.error("Doh!");
		return success;
	}
	
}
