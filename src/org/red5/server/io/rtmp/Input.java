package org.red5.server.io.rtmp;

import org.apache.mina.common.ByteBuffer;

/**
 * Input class that holds a bytebuffer
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class Input extends org.red5.server.io.amf.Input {

	/**
	 * Input Constructor
	 * @param buf
	 */
	public Input(ByteBuffer buf){
		super(buf);
	}
	
}
