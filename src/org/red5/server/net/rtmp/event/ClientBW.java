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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Client bandwidth event. Also known as a Peer Bandwidth message.
 */
public class ClientBW extends BaseEvent {

	private static final long serialVersionUID = 5848656135751336839L;

	/**
	 * Bandwidth
	 */
	private int bandwidth;

	/**
	 * Enforcement level or limit type of the bandwidth value based on three values.
	 * <pre>
	 * TYPE_HARD 0
	 * TYPE_SOFT 1
	 * TYPE_DYNAMIC 2
	 * </pre>
	 */
	private byte value2;

	public ClientBW() {
		super(Type.STREAM_CONTROL);
	}

	public ClientBW(int bandwidth, byte limitType) {
		this();
		this.bandwidth = bandwidth;
		this.value2 = limitType;
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return TYPE_CLIENT_BANDWIDTH;
	}

	/**
	 * Getter for property 'bandwidth'.
	 *
	 * @return Value for property 'bandwidth'.
	 */
	public int getBandwidth() {
		return bandwidth;
	}

	/**
	 * Setter for bandwidth
	 *
	 * @param bandwidth  New bandwidth
	 */
	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/**
	 * Getter for value2
	 *
	 * @return Value for property 'value2'.
	 */
	public byte getValue2() {
		return value2;
	}

	/**
	 * Setter for property 'value2'.
	 *
	 * @param value2 Value to set for property 'value2'.
	 */
	public void setValue2(byte value2) {
		this.value2 = value2;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "ClientBW: " + bandwidth + " enforcementLevel: " + value2;
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {

	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		bandwidth = in.readInt();
		value2 = in.readByte();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(bandwidth);
		out.writeByte(value2);
	}
}
