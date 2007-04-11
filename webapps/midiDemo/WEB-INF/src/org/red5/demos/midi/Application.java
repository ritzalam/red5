package org.red5.demos.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiDevice.Info;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;

public class Application extends ApplicationAdapter {

	private static final Log log = LogFactory.getLog(Application.class);

	/** {@inheritDoc} */
    @Override
	public boolean appStart(IScope app) {
		log.info("Midi demo app started");
		return super.appStart(app);
	}

	/** {@inheritDoc} */
    @Override
	public void appDisconnect(IConnection conn) {
		if (conn.hasAttribute("midi")) {
			MidiDevice dev = (MidiDevice) conn.getAttribute("midi");
			if (dev.isOpen()) {
				dev.close();
			}
		}
		super.appDisconnect(conn);
	}

	public boolean connectToMidi(String deviceName) {
		IServiceCapableConnection conn = (IServiceCapableConnection) Red5
				.getConnectionLocal();
		log.info("Connecting midi device: " + deviceName);
		try {
			MidiDevice dev = null;
			// Close any existing device
			if (conn.hasAttribute("midi")) {
				dev = (MidiDevice) conn.getAttribute("midi");
				if (dev.isOpen()) {
					dev.close();
				}
			}
			// Lookup the current device
			dev = getMidiDevice(deviceName);
			if (dev == null) {
				log.error("Midi device not found: " + deviceName);
				return false;
			}
			// Open if needed
			if (!dev.isOpen()) {
				dev.open();
			}
			dev.getTransmitter().setReceiver(new MidiReceiver(conn));
			log.info("It worked!");
			// Save for later
			conn.setAttribute("midi", dev);
			return true;
		} catch (MidiUnavailableException e) {
			log.error("Error connecting to midi device", e);
		} catch (RuntimeException e) {
			log.error("Error connecting to midi device", e);
		}
		log.error("Doh!");
		return false;
	}

	public boolean sendMidiShortMessage(int[] args, long time) 
		throws InvalidMidiDataException, MidiUnavailableException {
		try {
			MidiDevice dev = getCurrentMidiDevice();
			if(dev == null){
				log.error("Midi device is null, call connectToMidi first");
				return false;
			}

			final ShortMessage msg = new ShortMessage();
			switch(args.length){
				case 1:
					msg.setMessage(args[0]);
					break;
				case 3:
					msg.setMessage(args[0], args[1], args[2]);
					break;
				case 4:
					msg.setMessage(args[0], args[1], args[2], args[3]);
					break;
				default:
					log.error("Args array must have length 1, 3, or 4");
				return false;
			}

			dev.getReceiver().send(msg, time);}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return true;
	}
	
	private MidiDevice getCurrentMidiDevice(){
		IServiceCapableConnection conn = (IServiceCapableConnection) Red5.getConnectionLocal();
		if (conn.hasAttribute("midi")) {
			return (MidiDevice) conn.getAttribute("midi");
		}
		return null;
	}
		
	/**
     * Getter for property 'midiDeviceNames'.
     *
     * @return Value for property 'midiDeviceNames'.
     */
    public String[] getMidiDeviceNames() {
		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
		String[] names = new String[info.length];
		for (int i = 0; i < info.length; i++) {
			names[i] = info[i].getName();
		}
		return names;
	}

	public static MidiDevice getMidiDevice(String name) {

		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

		for (Info element : info) {
			if (element.getName().equals(name)) {
				try {
					return MidiSystem.getMidiDevice(element);
				} catch (MidiUnavailableException e) {
					log.error(e);
				}
			}
		}

		return null;

	}

	public class MidiReceiver extends Object implements Receiver {

		protected IServiceCapableConnection conn;

		public MidiReceiver(IServiceCapableConnection conn) {
			this.conn = conn;
		}

		/** {@inheritDoc} */
        public void send(MidiMessage midi, long time) {

			byte[] msg = midi.getMessage();
			int len = midi.getLength();
			if (len <= 1) {
				return;
			}

			conn.invoke("midi", new Object[] { time, msg });

			/*
			 String out = "Midi >> Status: "+msg[0]+" Data: [";
			 for(int i=1; i<len; i++){
			 out += msg[i] + ((i==len-1) ? "" : ','); 
			 }
			 out += ']';
			 
			 log.debug(out);
			 */
		}

		/** {@inheritDoc} */
        public void close() {
			log.debug("Midi device closed");
		}

	}
	
}
