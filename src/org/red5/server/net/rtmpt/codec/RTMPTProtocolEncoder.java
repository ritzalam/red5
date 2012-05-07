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

package org.red5.server.net.rtmpt.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.Status;

/**
 * RTMPT protocol encoder. To be implemented.
 */
public class RTMPTProtocolEncoder extends RTMPProtocolEncoder {

	@Override
	protected void encodeNotifyOrInvoke(IoBuffer out, Notify invoke, RTMP rtmp) {
		// if we get an InsufficientBW message for the client, we'll reduce the
		// base tolerance and set drop live to true
		final IServiceCall call = invoke.getCall();
		if ("onStatus".equals(call.getServiceMethodName()) && call.getArguments().length >= 1) {
			Object arg0 = call.getArguments()[0];
			if ("NetStream.Play.InsufficientBW".equals(((Status) arg0).getCode())) {
				long baseT = getBaseTolerance();
				try {
					// drop the tolerances by half but not less than 500
					setBaseTolerance(Math.max(baseT / 2, 500));
				} catch (Exception e) {
					log.debug("Problem setting base tolerance: {}", e.getMessage());
				}
				setDropLiveFuture(true);
			}
		}
		super.encodeNotifyOrInvoke(out, invoke, rtmp);
	}
	
}
