package org.red5.server.io.flv2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.io.flv.FLVHeader;
import org.red5.server.rtmp.RTMPUtils;
import org.red5.server.utils.HexDump;

/**
 * A FLVReader reads a flv video file from disc, parses the file, 
 * and send the byte stream down to the client 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (Dominick@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @version 0.3
 */
public class FLVReader {
	
	protected static Log log =
        LogFactory.getLog(FLVReader.class.getName());
	
	protected String fileName = null;
	protected FileInputStream fis = null;
	protected FileChannel channel = null;
	protected MappedByteBuffer mappedFile;
	protected ByteBuffer in;
	protected FLVHeader header = new FLVHeader();
	protected int limit = 0;
	
	/**
	 * main entry point for testing the FLVReader
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		//TODO create a testcase for FLVReader.java
		//FLVReader reader = new FLVReader("flvs/nvnlogo1.flv");
	}
	
	/**
	 * FLVReader Constructor which takes a File Object as a parameter
	 * @param fileName
	 * @throws IOException
	 */
	public FLVReader(File fileName) throws IOException {	
		log.debug("Reading: "+ fileName.getName());
		
		try {
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		channel = fis.getChannel();
		mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		mappedFile.order(ByteOrder.BIG_ENDIAN);
		in = ByteBuffer.wrap(mappedFile);
		limit = in.limit();
		decodeHeader();		
	}
	
	/**
	 * FLVReader Constructor which takes a String as a parameter.  
	 * @param fileName
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @deprecated 
	 */
	public FLVReader(String fileName) throws FileNotFoundException, IOException {
		log.debug("Reading: "+fileName);
		
		this.fileName = fileName;
		fis = new FileInputStream(fileName);
		channel = fis.getChannel();
		mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
		mappedFile.order(ByteOrder.BIG_ENDIAN);
		in = ByteBuffer.wrap(mappedFile);
		limit = in.limit();
		decodeHeader();		
	}
	
	/**
	 * Gets the Header Tag
	 * @return FLVHeader
	 */
	public FLVHeader getHeader() {
		return header;
	}

	/**
	 * Decodes the Header bytes
	 * @return void
	 */
	protected void decodeHeader(){
		// SIGNATURE, lets just skip
		in.skip(3);
		header.setVersion((byte) in.get());
		header.setTypeFlags((byte) in.get());
		header.setDataOffset(in.getInt());
	}
	
	/**
	 * Decodes the body bytes
	 * @return void
	 */
	protected void decodeBody(){
		// SIGNATURE, lets just skip
		
		int counter = 0;
		
		while(in.remaining() > 0){
		
			log.debug("PREV TAG SIZE: "+ in.getInt());
			counter++;
			
			if(counter > 5) break;
			
			if(in.remaining() == 0) break;
			
			log.debug("TAG TYPE: " + in.get());
			
			int dataSize = RTMPUtils.readUnsignedMediumInt(in);
		
			log.debug("DATA SIZE: "+dataSize);
			log.debug("TIMESTAMP: "+RTMPUtils.readUnsignedMediumInt(in));
			log.debug("RESERVED: "+ in.getInt());
			
			int limit = in.limit();
			
			in.limit(in.position()+dataSize);
			log.debug(HexDump.formatHexDump(in.getHexDump()));
			in.limit(limit);
				
		}
	}
	
	/**
	 * Gets the next FLVTag for parsing
	 * @return FLVTag
	 */
	public FLVTag getNextTag(){		
		// skip the prev tag size.. 
		log.debug("PREV TAG SIZE: "+ in.getInt());
		
		byte dataType = in.get();
		int bodySize = RTMPUtils.readUnsignedMediumInt(in);
		int timestamp = RTMPUtils.readUnsignedMediumInt(in);
		
		log.debug("RESERVED: "+ in.getInt());
		
		ByteBuffer body = ByteBuffer.allocate(bodySize);
		in.limit(in.position()+bodySize);
		body.put(in);
		body.flip();
		in.limit(limit);
		
		FLVTag tag = new FLVTag(dataType, timestamp, bodySize, body);

		return tag;
	}
	
	/**
	 * Returns a boolean stating whether this has more tags to read.
	 * @return boolean
	 */
	public boolean hasMoreTags(){
		return in.remaining() > 4;
	}	
}
