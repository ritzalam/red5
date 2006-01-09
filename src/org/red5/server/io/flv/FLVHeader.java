package org.red5.server.io.flv;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

/**
 * FLVHeader parses out the contents of a FLV video file and returns
 * the Header data 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class FLVHeader {

	// Signature
	public byte[] signature = null;
	public byte version = 0x00; //version 1
	 
	// TYPES
	public byte flagReserved01 = 0x00;
	public boolean flagAudio = false;
	public byte flagReserved02 = 0x00;
	public boolean flagVideo = false;
	
	// DATA OFFSET
	// reserved for data up to 4,294,967,295
	public int dataOffset = 0x00;

	/**
	 * Returns the data offset bytes
	 * @return int
	 */
	public int getDataOffset() {
		return dataOffset;
	}

	/**
	 * Sets the data offset bytes
	 * @param data_offset
	 */
	public void setDataOffset(int data_offset) {
		dataOffset = data_offset;
	}

	/**
	 * Returns the signature bytes
	 * @return byte[]
	 */
	public byte[] getSignature() {		
		return signature;
	}
	
	/**
	 * Overrides the toString method so that a FLVHeader can
	 * be represented by its datatypes
	 * @return String
	 */
	public String toString() {
		String ret = "";
		//ret += "SIGNATURE: \t" + getSIGNATURE() + "\n";
		//ret += "SIGNATURE: \t\t" + new String(signature) + "\n";  
		ret += "VERSION: \t\t" + ((byte) getVersion()) + "\n";
		ret += "TYPE FLAGS VIDEO: \t" + getFlagVideo() + "\n";
		ret += "TYPE FLAGS AUDIO: \t" + getFlagAudio() + "\n";
		ret += "DATA OFFSET: \t\t" + getDataOffset() + "\n";
		//byte b = 0x01;
		
		return ret;
		
	}

	/**
	 * Sets the signature bytes
	 * @param signature
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	/**
	 * Returns a boolean on whether this data contains audio
	 * @return boolean
	 */
	public boolean getFlagAudio() {
		return flagAudio;
	}

	/**
	 * Sets the audioflag on whether this data contains audio
	 * @param flagAudio
	 */
	public void setFlagAudio(boolean flagAudio) {
		this.flagAudio = flagAudio;
	}

	/**
	 * Sets the type flags on whether this data is audio or video
	 * @param typeFlags
	 * @return void
	 */
	public void setTypeFlags(byte typeFlags) {
		flagVideo = (((typeFlags << 7) >> 7) > 0x00) ? true : false;
		flagAudio = (((typeFlags << 5) >> 7) > 0x00) ? true : false;
	}
	
	/**
	 * Gets the FlagReserved01 which is a datatype specified in the Flash Specification
	 * @return byte
	 */
	public byte getFlagReserved01() {
		return flagReserved01;
	}

	/**
	 * Sets the FlagReserved01 which is a datatype specified in the Flash Specification
	 * @param flagReserved01
	 * @return void
	 */
	public void setFlagReserved01(byte flagReserved01) {
		this.flagReserved01 = flagReserved01;
	}

	/**
	 * Gets the FlagReserved02 which is a datatype specified in the Flash Specification
	 * @return byte
	 */
	public byte getFlagReserved02() {
		return flagReserved02;
	}

	/**
	 * Sets the Flag Reserved02 which is a datatype specified in the Flash Specification
	 * @param flagReserved02
	 * @return void
	 */
	public void setFlagReserved02(byte flagReserved02) {
		this.flagReserved02 = flagReserved02;
	}

	/**
	 * Returns a boolean on whether this data contains video
	 * @return boolean
	 */
	public boolean getFlagVideo() {
		return flagVideo;
	}

	/**
	 * Sets the audioflag on whether this data contains audio
	 * @param type_flags_video
	 * @return void
	 */
	public void setFlagVideo(boolean type_flags_video) {
		flagVideo = type_flags_video;
	}

	/**
	 * Gets the version byte
	 * @return byte
	 */
	public byte getVersion() {
		return version;
	}

	/**
	 * Sets the version byte
	 * @param version
	 */
	public void setVersion(byte version) {
		this.version = version;
	}

}
