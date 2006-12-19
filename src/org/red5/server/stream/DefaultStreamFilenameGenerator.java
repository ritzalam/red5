
package org.red5.server.stream;

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
import org.red5.server.api.stream.IStreamFilenameGenerator;

/**
 * Default filename generator for streams. The files will be stored in a
 * directory "streams" in the application folder. Option for changing directory
 * streams are saved to is investigated as of 0.6RC1.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (bauch@struktur.de)
 */
public class DefaultStreamFilenameGenerator implements IStreamFilenameGenerator {

    /**
     * For now, return "streams/" as streams directory path for each scope
     * @param scope            Scope
     * @return                 For now, "streams/" as streams directory path
     */
    private String getStreamDirectory(IScope scope) {
		return "streams/";
	}

	/** {@inheritDoc} */
    public String generateFilename(IScope scope, String name, GenerationType type) {
		return generateFilename(scope, name, null, type);
	}

	/** {@inheritDoc} */
    public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
		String result = getStreamDirectory(scope) + name;
		if (extension != null && !extension.equals("")) {
			result += extension;
		}
		return result;
	}

}