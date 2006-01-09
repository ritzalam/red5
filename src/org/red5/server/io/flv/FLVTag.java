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

import java.nio.ByteBuffer;

/**
 * A FLVTag represents the contents of a FLV Video file.  The  flv file consists of
 * a HEADER, BODY, and the body consists of 1,.,.,n FLVTags. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class FLVTag {
	
	private byte tagType = 0x00; //audio=8, video=9
	private int dataSize;
	private byte[] timeStamp;
	private int reserved = 0x00;
	private byte[] data; // audio or video data
	private ByteBuffer buf;

	/**
	 * FLVTag Constructor
	 * @param buf
	 */
	public FLVTag(ByteBuffer buf) {
		// TODO Auto-generated constructor stub
		this.buf = buf;
	}

	/**
	 * Gets the data
	 * @return byte[]
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Sets the data
	 * @param data
	 */
	public void setData(byte[] data) {		
		this.data = data;
	}

	/**
	 * Gets the data's size
	 * @return int
	 */
	public int getDataSize() {
		return dataSize;
	}

	/**
	 * Sets the data's size
	 * @param dataSize
	 * @return void
	 */
	public void setDataSize(byte[] dataSize) {
		int n = 0;
	//	System.out.println("dataSize[0]: " + dataSize[1]);
		//System.out.println("dataSize[0]: " + (unsignedByteToInt(dataSize[1]) << 16));
		
		//System.out.println("((dataSize[0]) << 16): " + unsignedByteToInt(dataSize[2]));
		int tmpD0 = unsignedByteToInt(dataSize[0]);
		int tmpD1 = unsignedByteToInt(dataSize[1]);
		int tmpD2 = unsignedByteToInt(dataSize[2]);
		
		/*
		System.out.println("tmpD0 : "  + tmpD0);
		System.out.println("tmpD1 : "  + tmpD1);
		System.out.println("tmpD2 : "  + tmpD2);
		*/
		
		int tmpD0Shift = tmpD0 << 16;
		int tmpD1Shift = tmpD1 << 8;
		int tmpD2Shift = tmpD2;
		
		int ret = tmpD0Shift + tmpD1Shift + tmpD2Shift;
		
		/*
		System.out.println("tmpD0Shift : "  + tmpD0Shift);
		System.out.println("tmpD1Shift : "  + tmpD1Shift);
		System.out.println("tmpD2Shift : "  + tmpD2Shift);
		System.out.println("test : "  + (tmpD0 << 8));		
		*/
			
		this.dataSize = ret;
	}

	/**
	 * Gets the reserved bytes
	 * @return int
	 */
	public int getReserved() {
		return reserved;
	}

	/**
	 * Sets the reserved bytes
	 * @param reserved
	 * @return void
	 */
	public void setReserved(int reserved) {
		this.reserved = reserved;
	}

	/**
	 * Gets the tag type
	 * @return byte
	 */
	public byte getTagType() {
		return tagType;
	}

	/**
	 * Sets the tag type
	 * @param tagType
	 * @return void
	 */
	public void setTagType(byte tagType) {
		this.tagType = tagType;
	}

	/**
	 * Gets the time stamp
	 * @return byte[]
	 */
	public byte[] getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Sets the time stamp
	 * @param timeStamp
	 * @return void
	 */
	public void setTimeStamp(byte[] timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * Overrides the toString method
	 */
	public String toString() {
		String ret = "";
		//ret += "SIGNATURE: \t" + getSIGNATURE() + "\n";
		//ret += "previousTagSize: \t\t" + 
		ret += "tagType: \t\t" + this.getTagType() +  "\n";  
		ret += "dataSize: \t\t" + dataSize + "\n";
		ret += "timeStamp: \t\t" + (unsignedByteToInt(timeStamp[0]) + unsignedByteToInt(timeStamp[1]) + unsignedByteToInt(timeStamp[2])) + "\n";
		ret += "reserved: \t\t" + reserved + "\n";
		ret += "data: \t\t\t" + data.length + "\n";
		//byte b = 0x01;
		
		return ret;
	}
	
	/**
	 * Converts an unsigned byte to an int
	 * @param b
	 * @return int
	 */
	public static int unsignedByteToInt(byte b) {
	    return (int) b & 0xFF;
	}
	
	/**
	 * entry point for testing FLVTag
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	/**
	 * Returns a boolean stating whether there are remaining bytes
	 * @return boolean
	 */
	public boolean hasRemaining() {
		// TODO Auto-generated method stub
		return false;
	}
	

}
