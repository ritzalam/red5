package org.red5.server.midi;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.demos.midi.Application;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.midi.Test.MyReceiver;

public class SharedMidiObject {

	private static final Log log = LogFactory.getLog(SharedMidiObject.class);
	
	protected String deviceName;
	protected ISharedObject so;
	protected MidiDevice dev; 
	
	public SharedMidiObject(String deviceName, ISharedObject so){
		this.deviceName = deviceName;
		this.so = so;
	}
	
	public boolean connect(){
		try {
			dev = getMidiDevice(deviceName);
			if(!dev.isOpen()) dev.open();
			dev.getTransmitter().setReceiver(new MidiReceiver());
			return true;
		} catch (MidiUnavailableException e) {
			log.error("Error connecting to midi device", e);
		}
		return false;
	}
	
	public void close(){
		if(dev != null && dev.isOpen()) dev.close();
	}
	
	public static MidiDevice getMidiDevice(String name){
		
		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
		
		for (int i = 0; i < info.length; i++) {
			if(info[i].getName().equals(name)) {
				try {
					return MidiSystem.getMidiDevice(info[i]);
				} catch (MidiUnavailableException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
		
	}
	
	public class MidiReceiver extends Object implements Receiver {

		public void send(MidiMessage midi, long time) {
			
			byte[] msg = midi.getMessage();
			int len = midi.getLength();
			if(len <= 1) return; 
			
			List list = new ArrayList();
			list.add(time);
			list.add(len);
			list.add(msg);
			so.beginUpdate();
			so.sendMessage("midi", list);
			so.endUpdate();
			
			String out = "Midi >> Status: "+msg[0]+" Data: [";
			for(int i=1; i<len; i++){
				out += msg[i] + ((i==len-1) ? "" : ","); 
			}
			out += "]";
			
			log.debug(out);
		}

		public void close() {
			log.debug("Midi device closed");
		}

	}
	
}
