/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.io.m4a;

import org.red5.io.IStreamableFileService;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * A M4AService sets up the service and hands out M4A objects to 
 * its callers
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IM4AService extends IStreamableFileService {

	/**
	 * Sets the serializer
	 * 
	 * @param serializer        Serializer object
	 */
	public void setSerializer(Serializer serializer);

	/**
	 * Sets the deserializer
	 * 
	 * @param deserializer      Deserializer object
	 */
	public void setDeserializer(Deserializer deserializer);

}
