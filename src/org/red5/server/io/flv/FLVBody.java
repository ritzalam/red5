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
 */

import java.nio.MappedByteBuffer;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.protocol.remoting.RemotingService;

/**
 * A FLVBody represents the contents of a FLV Video file.  The  flv file consists of
 * a HEADER, BODY, and the body consists of 1,.,.,n FLVTags. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @version 0.3
 */
public class FLVBody {
	private int previousTagSize = 0;
	private FLVTag tag;
	private MappedByteBuffer mappedFile;
	int packetSize = 0;
	
	/**
	 * FLVBody Constructor
	 * @param mappedFile
	 */
	public FLVBody(MappedByteBuffer mappedFile) {
		// TODO Auto-generated constructor stub
		this.mappedFile = mappedFile;
	}
	
	/**
	 * Gets the previousTagSize
	 * @return int
	 */
	public int getPreviousTagSize() {
		return previousTagSize;
	}
	
	/**
	 * Sets the previousTagSize
	 * @param previousTagSize
	 */
	public void setPreviousTagSize(int previousTagSize) {
		this.previousTagSize = previousTagSize;
	}
	
	/**
	 * Gets the FLVTag
	 * @return FLVTag
	 */
	public FLVTag getTag() {
		return tag;
	}
	
	/**
	 * Sets the FLVTag
	 * @param tag
	 */
	public void setTag(FLVTag tag) {
		this.tag = tag;
	}
	
	/**
	 * Gets the complete contents of the FLVBody
	 * @return void
	 */
	public void getTags() {
		// TODO Auto-generated method stub
		int packetSize = 0;
		while(mappedFile.hasRemaining()) {
			
			packetSize++;
		
			//System.out.println("packet #: " + packetSize);
		
			// PREVIOUS_TAG_SIZE
			this.setPreviousTagSize(mappedFile.getInt());
			System.out.println("PreviousTagSize: " + this.getPreviousTagSize());
			
			// guard against additional reads
			if(!mappedFile.hasRemaining()) {
				System.out.println("FLV has reached end of stream");
				break;
			}
			
			System.out.println("FLVTAG " + packetSize);
			// Create FLVTag
		
			FLVTag tag = new FLVTag(mappedFile);
		
			
			// TAG TYPE
			//System.out.println("ho: " + unsignedByteToInt(mappedFile.get()));
			
			tag.setTagType((byte) mappedFile.get());
			
			//System.out.println("test: " + mappedFile.get());
			// DATA SIZE
			
			tag.setDataSize(this.readDataSize());
			
			// TIME STAMP
			tag.setTimeStamp(this.readTimeStamp());
			
			// RESERVED
			tag.setReserved(mappedFile.getInt());
			
			int tmp = tag.getDataSize();
			
			//System.out.println("ah: " + unsignedByteToInt(tmp[2]));
			tag.setData(this.readData(tmp));
			
			//System.out.println("test: " + tag.getDataSize()[0] +tag.getDataSize()[1] + tag.getDataSize()[2] );
			//byte[] n = this.readData(tag.getDataSize());
			System.out.println("------------\n" + tag + "\n");
			
			//onTag(tag);
			// DATA
			
		}
		
	}
	
	/**
	 * Callback method that is called when a tag is received.  The tag
	 * is then passed into this method
	 * @param tag2
	 */
	private void onTag(FLVTag tag2) {
		// TODO Auto-generated method stub
		if(tag2.getTagType() == 0x12) {
			System.out.println("---MetaData---");
			byte[] bb = tag2.getData();
			ByteBuffer bb1 = ByteBuffer.allocate(tag2.getDataSize());
			bb1.put(bb);
			RemotingService rs = new RemotingService();
			rs.handleRequest(bb1);
			System.out.println("\n\n");
		}
	}
	
	/**
	 * Read the data
	 * @param dataSize
	 * @return byte[]
	 */
	private byte[] readData(int dataSize) {
		/*
		int tmp = unsignedByteToInt(dataSize[0]);
		tmp += unsignedByteToInt(dataSize[1]);
		tmp += unsignedByteToInt(dataSize[2]);
		*/
		//int tmp = dataSize[0] + dataSize[1] + dataSize[2];
		
		//System.out.println("data[0]: " + data[0]);
		byte b[] = new byte[dataSize];
		for(int i=0; i<dataSize; i++) {
			b[i] = mappedFile.get();
		}
		
		return b;
	}
	
	/** 
	 * Read the time stamp
	 * @return byte[]
	 */
	private byte[] readTimeStamp() {
		int timeStampBytes = 3;
		byte b[] = new byte[3];
		for(int i=0; i<timeStampBytes; i++) {
			b[i] = (byte) mappedFile.get();
		}	
		
		return b;
	}
	
	/**
	 * Read the data size
	 * @return byte[]
	 */
	private byte[] readDataSize() {
		int dataSizeBytes = 3;
		byte b[] = new byte[3];
		for(int i=0; i<dataSizeBytes; i++) {
			b[i] = (byte) mappedFile.get();
			//System.out.println("wow: " + b[i]);
		}	
		
		return b;
	}
	
	/**
	 * Read unsigned byte and return an int
	 * @param b
	 * @return int
	 */
	public static int unsignedByteToInt(byte b) {
	    return (int) b & 0xFF;
	}
	
	/**
	 * Get next tag
	 * @return FLVTag
	 */
	public FLVTag getNextTag() {
				
		// PREVIOUS_TAG_SIZE
		this.setPreviousTagSize(mappedFile.getInt());
		System.out.println("PreviousTagSize: " + this.getPreviousTagSize());
				
		// guard against additional reads
		if(!mappedFile.hasRemaining()) {
			System.out.println("FLV has reached end of stream");
			return null;
		}		
		
		packetSize++;
		
		System.out.println("FLVTAG " + packetSize);
		// Create FLVTag
	
		FLVTag tag = new FLVTag(mappedFile);
	
		
		// TAG TYPE
		//System.out.println("ho: " + unsignedByteToInt(mappedFile.get()));
		
		tag.setTagType((byte) mappedFile.get());
		
		//System.out.println("test: " + mappedFile.get());
		// DATA SIZE
		
		tag.setDataSize(this.readDataSize());
		
		// TIME STAMP
		tag.setTimeStamp(this.readTimeStamp());
		
		// RESERVED
		tag.setReserved(mappedFile.getInt());
		
		int tmp = tag.getDataSize();
		
		//System.out.println("ah: " + unsignedByteToInt(tmp[2]));
		tag.setData(this.readData(tmp));
		
		//System.out.println("test: " + tag.getDataSize()[0] +tag.getDataSize()[1] + tag.getDataSize()[2] );
		//byte[] n = this.readData(tag.getDataSize());
		System.out.println("------------\n" + tag + "\n");
		
		return tag;
		
	}

	/**
	 * Returns a boolean stating whether there is more data
	 * @return boolean
	 */
	public boolean hasRemaining() {
		return mappedFile.hasRemaining();
	}
}
