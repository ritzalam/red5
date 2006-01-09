package org.red5.server.io.rtmp;

import org.apache.mina.common.ByteBuffer;

/**
 * Output class that holds a bytebuffer
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class Output extends org.red5.server.io.amf.Output {

	/**
	 * Output Constructor
	 * @param buf
	 */
	public Output(ByteBuffer buf){
		super(buf);
	}
	
}
