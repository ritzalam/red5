package org.red5.io.flv.meta;

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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.mina.common.ByteBuffer;

/**
 * IMetaService Defines the MetaData Service API
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface IMetaService {

	// Get FLV from FLVService
	// grab a reader from FLV	
	// Set up CuePoints
	// Set up MetaData
	// Pass CuePoint array into MetaData
	// read in current MetaData if there is MetaData
	// if there isn't MetaData, write new MetaData
	// Call writeMetaData method on MetaService
	// that in turn will write the current metadata
	// and the cuepoint data
	// after that, call writeMetaCue()
	// this will loop through all the tags making
	// sure that the cuepoints are inserted

	/**
	 * Initiates writing of the MetaData
	 * 
	 * @param meta 
	 * @throws IOException 
	 */
	public void write(IMetaData meta) throws IOException;

	/**
	 * Writes the MetaData
	 * 
	 * @param metaData
	 */
	public void writeMetaData(IMetaData metaData);

	/**
	 * Writes the Meta Cue Points
	 */
	public void writeMetaCue();

	/**
	 * Read the MetaData
	 * 
	 * @return metaData
	 */
	public MetaData readMetaData(ByteBuffer buffer);

	/**
	 * Read the Meta Cue Points
	 * 
	 * @return IMetaCue[] metaCue[]
	 */
	public IMetaCue[] readMetaCue();

	public void setInStream(FileInputStream fis);

	public void setOutStream(FileOutputStream fos);

}
