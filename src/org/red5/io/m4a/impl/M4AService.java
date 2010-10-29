package org.red5.io.m4a.impl;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
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

import java.io.File;
import java.io.IOException;

import org.red5.io.BaseStreamableFileService;
import org.red5.io.IStreamableFile;
import org.red5.io.m4a.IM4AService;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * A M4AServiceImpl sets up the service and hands out M4A objects to 
 * its callers.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class M4AService extends BaseStreamableFileService implements IM4AService {

    /**
     * Serializer
     */
    private Serializer serializer;

    /**
     * Deserializer
     */
    private Deserializer deserializer;

    /**
     * File extensions handled by this service. If there are more than one, they
     * are comma separated.
     */
    private static String extension = ".f4a,.m4a,.aac";
    
    private static String prefix = "f4a";
    
	/** {@inheritDoc} */
    @Override
    public void setPrefix(String prefix) {
    	M4AService.prefix = prefix;
	}    
    
	/** {@inheritDoc} */
    @Override
	public String getPrefix() {
		return prefix;
	}

	/** {@inheritDoc} */
    @Override
    public void setExtension(String extension) {
    	M4AService.extension = extension;
	}
	
	/** {@inheritDoc} */
    @Override
	public String getExtension() {
		return extension;
	}

	/** 
     * {@inheritDoc}
	 */
	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;

	}

	/** {@inheritDoc}
	 */
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;

	}

	/** {@inheritDoc}
	 */
	@Override
	public IStreamableFile getStreamableFile(File file) throws IOException {
		return new M4A(file);
	}

	/**
     * Getter for serializer
     *
     * @return  Serializer
     */
    public Serializer getSerializer() {
		return serializer;
	}

	/**
     * Getter for deserializer
     *
     * @return  Deserializer
     */
    public Deserializer getDeserializer() {
		return deserializer;
	}
}
