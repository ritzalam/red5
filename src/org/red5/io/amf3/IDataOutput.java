package org.red5.io.amf3;

import java.nio.ByteOrder;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

public interface IDataOutput {

	public ByteOrder getEndian();
	
	public void setEndian(ByteOrder endian);
	
	public void writeBoolean(boolean value);
	
	public void writeByte(byte value);
	
	public void writeBytes(byte[] bytes);
	
	public void writeBytes(byte[] bytes, int offset);
	
	public void writeBytes(byte[] bytes, int offset, int length);
	
	public void writeDouble(double value);
	
	public void writeFloat(float value);
	
	public void writeInt(int value);
	
	public void writeMultiByte(String value, String encoding);
	
	public void writeObject(Object value);
	
	public void writeShort(short value);
	
	public void writeUnsignedInt(long value);
	
	public void writeUTF(String value);
	
	public void writeUTFBytes(String value);

}
