package org.red5.server.midi;

import java.io.File;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Test {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(Test.class.getName());

	public static void main(String[] args) throws Exception {
		Test t = new Test();
	}

	public Test() throws Exception {

		// MidiPlayer player = new MidiPlayer(new
		// File("/Users/luke/Desktop/Loops_Of_Fury.mid"));

		// Trying to get the events..
		// MidiSystem.getSequencer().getTransmitter().setReceiver(new
		// MyReceiver());
		// MidiSystem.getTransmitter().setReceiver(new MyReceiver());

		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

		MidiDevice[] devices = new MidiDevice[info.length];

		for (int i = 0; i < info.length; i++) {

			try {

				log.debug(info[i].getName());
				MidiDevice device = MidiSystem.getMidiDevice(info[i]);
				log.debug(device);
				if (!device.isOpen())
					device.open();
				// Hook up a receiver to the transmitter
				if (device.isOpen()) {
					device.getTransmitter().setReceiver(new MyReceiver());
					devices[i] = device;
				}

			} catch (MidiUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// Wait long enough to play a few notes
		// on the keyboard
		Thread.sleep(30000);

		// Close the device (at program exit)
		for (MidiDevice device : devices)
			if (device != null)
				device.close();

	}

	public class MyReceiver extends Object implements Receiver {

		public void send(MidiMessage msg, long time) {
			log.debug("Received message " + msg);
		}

		public void close() {
			log.debug("Closing");
		}
	}

}