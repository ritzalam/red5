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

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

/** Plays a midi file provided on command line */
public class MidiPlayer {

	public static void main(String args[]) {
		// Argument check
		if (args.length == 0) {
			helpAndExit();
		}
		String file = args[0];
		if (!file.endsWith(".mid")) {
			helpAndExit();
		}
		File midiFile = new File(file);
		if (!midiFile.exists() || midiFile.isDirectory() || !midiFile.canRead()) {
			helpAndExit();
		}
	}

	public MidiPlayer(File midiFile) {

		// Play once
		try {
			Sequencer sequencer = MidiSystem.getSequencer();
			sequencer.setSequence(MidiSystem.getSequence(midiFile));
			sequencer.open();
			sequencer.start();
			/*
			 while(true) {
			 if(sequencer.isRunning()) {
			 try {
			 Thread.sleep(1000); // Check every second
			 } catch(InterruptedException ignore) {
			 break;
			 }
			 } else {
			 break;
			 }
			 }
			 // Close the MidiDevice & free resources
			 sequencer.stop();
			 sequencer.close();
			 */
		} catch (MidiUnavailableException mue) {
			System.out.println("Midi device unavailable!");
		} catch (InvalidMidiDataException imde) {
			System.out.println("Invalid Midi data!");
		} catch (IOException ioe) {
			System.out.println("I/O Error!");
		}

	}

	/** Provides help message and exits the program */
	private static void helpAndExit() {
		System.out.println("Usage: java MidiPlayer midifile.mid");
		//System.exit(1);
	}
}
