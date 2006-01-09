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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * FLVDecoder parses out the contents of a FLV video file and returns
 * the data in chunks
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @version 0.3
 */
public class FLVDecoder {

	private String fileName = null;
	FileInputStream fis = null;
	FileChannel channel = null;
	private MappedByteBuffer mappedFile;
	
	/**
	 * main entry point for testing purposes
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length < 1) {
			args = new String[1];
			args[0] = "flvs/nvnlogo1.flv";
			//usage();
			//return;
		}
		
		FLVDecoder decoder = new FLVDecoder(args[0]);
		decoder.decode();
	}
	
	/**
	 * Show usage of command line
	 * @return void
	 */
	private static void usage() {
		// TODO Auto-generated method stub
		System.out.println("java FLVDecoder.java [filename]");
	}

	/**
	 * FLVDecoder Constructor
	 * @param s
	 */
	public FLVDecoder(String s) {
		this.fileName = s;
		
		if(fileName == null) {
			System.out.println("Please enter a filename");
			usage();
			return;
		} 
		
		try {
			fis = new FileInputStream(fileName);
			channel = fis.getChannel();
			mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			mappedFile.order(ByteOrder.BIG_ENDIAN);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		
		//decode();
	}
	
	/**
	 * decode method decodes the different types inside of a FLV
	 *
	 */
	public void decode() {
		
		// Instantiate FLVHeader
		FLVHeader header = new FLVHeader();
		
		// SIGNATURE	
		header.setSignature(readSignatureBytes());
		
		// VERSION
		header.setVersion((byte) mappedFile.get());
		
		// TYPE FLAGS
		header.setTypeFlags((byte) mappedFile.get());
		
		// DATA OFFSET
		header.setDataOffset(mappedFile.getInt());
		
		// Print FLVHeader
		System.out.println("HEADER: (pos)" + mappedFile.position() + "\n---------\n" + header.toString());
		
		// Decode FLVBody
		
		
		System.out.println("BODY: (pos)" + mappedFile.position() + "\n---------\t");
		FLVBody body = new FLVBody(mappedFile);
		body.getTags();
		
		
	
	}

	/**
	 * Reads the signature bytes of a FLV
	 * @return byte[]
	 */
	private byte[] readSignatureBytes() {
		int signatureBytes = 3;
		byte b[] = new byte[3];
		for(int i=0; i<signatureBytes; i++) {
			b[i] = mappedFile.get();
		}	
		
		return b;
	}

	/**
	 * Gets the file name
	 * @return String
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the filename
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Gets the FileInputStream
	 * @return FileInputStream
	 */
	public FileInputStream getFis() {
		return fis;
	}

	/**
	 * Sets the FileInputStream
	 * @param fis
	 */
	public void setFis(FileInputStream fis) {
		this.fis = fis;
	}

	/**
	 * Decodes the header bytes
	 * @return FLVHeader
	 */
	public FLVHeader decodeHeader() {
		// Instantiate FLVHeader
		FLVHeader header = new FLVHeader();
		
		// SIGNATURE	
		header.setSignature(readSignatureBytes());
		
		// VERSION
		header.setVersion((byte) mappedFile.get());
		
		// TYPE FLAGS
		header.setTypeFlags((byte) mappedFile.get());
		
		// DATA OFFSET
		header.setDataOffset(mappedFile.getInt());
		
		// Print FLVHeader
		System.out.println("HEADER: (pos)" + mappedFile.position() + "\n---------\n" + header.toString());
		
		return header;
	}

	/**
	 * Decodes the body of the files bytes
	 * @return FLVBody
	 */
	public FLVBody decodeBody() {
		// Instantiate FLVBody
		FLVBody body = new FLVBody(mappedFile);
		return body;
	}

}
