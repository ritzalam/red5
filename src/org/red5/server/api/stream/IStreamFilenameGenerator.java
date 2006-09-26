package org.red5.server.api.stream;

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

import org.red5.server.api.IScope;

/**
 * A class that can generate filenames for streams.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (bauch@struktur.de)
 */
public interface IStreamFilenameGenerator {

	/** Name of the bean to setup a custom filename generator in an application. */
	public static final String KEY = "streamFilenameGenerator";

	/**
	 * Generate a filename without an extension.
	 * 
	 * @param scope
	 * @param name
	 * @return
	 */
	public String generateFilename(IScope scope, String name);

	/**
	 * Generate a filename with an extension.
	 * 
	 * @param scope
	 * @param name
	 * @param extension
	 * @return
	 */
	public String generateFilename(IScope scope, String name, String extension);

}
