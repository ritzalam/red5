package org.red5.io.amf3;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.nio.ByteOrder;

public interface IDataInput {

	public ByteOrder getEndian();
	
	public void setEndian(ByteOrder endian);
	
	public boolean readBoolean();
	
	public byte readByte();
	
	public void readBytes(byte[] bytes);
	
	public void readBytes(byte[] bytes, int offset);
	
	public void readBytes(byte[] bytes, int offset, int length);

	public double readDouble();
	
	public float readFloat();
	
	public int readInt();
	
	public String readMultiByte(int length, String charSet);
	
	public Object readObject();
	
	public short readShort();

	public int readUnsignedByte();

	public long readUnsignedInt();

	public int readUnsignedShort();

	public String readUTF();

	public String readUTFBytes(int length);

}
