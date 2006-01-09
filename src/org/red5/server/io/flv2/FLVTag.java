package org.red5.server.io.flv2;

import org.apache.mina.common.ByteBuffer;

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
	
	public static final byte TYPE_VIDEO = 0x09;
	public static final byte TYPE_AUDIO = 0x08;
	public static final byte TYPE_METADATA = 0x12;

	protected byte dataType;
	protected int timestamp;
	protected int bodySize;
	protected ByteBuffer body;
	
	/**
	 * FLVTag Constructor
	 * 
	 * @param dataType
	 * @param timestamp
	 * @param bodySize
	 * @param body
	 */
	public FLVTag(byte dataType, int timestamp, int bodySize, ByteBuffer body){
		this.dataType = dataType;
		this.timestamp = timestamp;
		this.bodySize = bodySize;
		this.body = body;
	}
	
	/**
	 * Return the body ByteBuffer
	 * @return ByteBuffer
	 */
	public ByteBuffer getBody() {
		return body;
	}
	
	/**
	 * Return the size of the body
	 * @return int
	 */
	public int getBodySize() {
		return bodySize;
	}
	
	/**
	 * Get the data type
	 * @return byte
	 */
	public byte getDataType() {
		return dataType;
	}
	
	/**
	 * Return the timestamp
	 * @return int
	 */
	public int getTimestamp() {
		return timestamp;
	}

}
