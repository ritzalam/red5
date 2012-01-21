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

package org.red5.compatibility.flex.messaging.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * Flex <code>ArrayCollection</code> compatibility class.
 * 
 * @see <a href="http://livedocs.adobe.com/flex/2/langref/mx/collections/ArrayCollection.html">Adobe Livedocs (external)</a>
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @param <T> type of collection
 */
public class ArrayCollection<T> extends ArrayList<T> implements Collection<T>, IExternalizable {

	/** Serial number */
	private static final long serialVersionUID = -9086041828446362637L;
	
	/** {@inheritDoc} */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void readExternal(IDataInput input) {
		clear();
		Object tmp = input.readObject();
		if (tmp != null) {
			addAll((List) tmp);
		}
	}

	/** {@inheritDoc} */
	public void writeExternal(IDataOutput output) {
		output.writeObject(this.toArray());
	}

}
