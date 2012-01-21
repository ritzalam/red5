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

package org.red5.server.net.rtmp.event;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IPendingServiceCall;

/**
 * Remote invocation event
 */
public class Invoke extends Notify {
	
	private static final long serialVersionUID = -769677790148010729L;

	/** Constructs a new Invoke. */
    public Invoke() {
		super();
	}

	/** {@inheritDoc} */
    @Override
	public byte getDataType() {
		return TYPE_INVOKE;
	}

    /**
     * Create new invocation event with given data
     * @param data        Event data
     */
    public Invoke(IoBuffer data) {
		super(data);
	}

    /**
     * Create new invocation event with given pending service call
     * @param call         Pending call
     */
    public Invoke(IPendingServiceCall call) {
		super(call);
	}

	/** {@inheritDoc} */
    @Override
	public IPendingServiceCall getCall() {
		return (IPendingServiceCall) call;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Invoke: ").append(call);
		return sb.toString();
	}

	/** {@inheritDoc} */
    @Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Invoke)) {
			return false;
		}
		return super.equals(obj);
	}

}
