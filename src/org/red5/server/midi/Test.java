package org.red5.server.midi;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiDevice.Info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {

	// Initialize Logging
	protected static Logger log = LoggerFactory.getLogger(Test.class);

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused") Test t = new Test();
	}

	public static MidiDevice getMidiDevice(String name) {

		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

		for (Info element : info) {
			if (element.getName().equals(name)) {
				try {
					return MidiSystem.getMidiDevice(element);
				} catch (MidiUnavailableException e) {
					log.error("{}", e);
				}
			}
		}

		return null;

	}

	/** Constructs a new Test. */
    public Test() throws Exception {

		String MIDI_NAME = "USB Uno MIDI  In";
		MidiDevice dev = getMidiDevice(MIDI_NAME);
		dev.open();
		MyReceiver rec = new MyReceiver();
		dev.getTransmitter().setReceiver(rec);
		Thread.sleep(20000);
		dev.close();

	}

	public class MyReceiver extends Object implements Receiver {

		/** {@inheritDoc} */
        public void send(MidiMessage midi, long time) {
			byte[] msg = midi.getMessage();
			int len = midi.getLength();
			if (len <= 1) {
				return;
			}
			String out = "Status: " + msg[0] + " Data: [";
			for (int i = 1; i < len; i++) {
				out += msg[i] + ((i == len - 1) ? "" : ",");
			}
			out += ']';
			log.debug(out);
		}

		/** {@inheritDoc} */
        public void close() {
			log.debug("Closing");
		}
	}

}