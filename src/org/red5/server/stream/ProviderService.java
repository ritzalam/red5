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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.red5.io.IStreamableFileFactory;
import org.red5.io.IStreamableFileService;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.stream.provider.FileProvider;

public class ProviderService implements IProviderService {
	
	public IMessageInput getProviderInput(IScope scope, String name) {
		IMessageInput msgIn = getLiveProviderInput(scope, name, false);
		if (msgIn == null) return getVODProviderInput(scope, name);
		return msgIn;
	}

	public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
		synchronized (scope) {
			IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
			if (basicScope == null) {
				if (needCreate) {
					basicScope = new BroadcastScope(scope, name);
					scope.addChildScope(basicScope);
				} else return null;
			}
			if (!(basicScope instanceof IBroadcastScope)) return null;
			return (IBroadcastScope) basicScope;
		}
	}

	public IMessageInput getVODProviderInput(IScope scope, String name) {
		File file = null;
		try {
			file = scope.getResources(getStreamFilename(scope, name))[0].getFile();
		} catch (IOException e) {}
		if (file == null || !file.exists()) {
			return null;
		}
		IPipe pipe = new InMemoryPullPullPipe();
		pipe.subscribe(new FileProvider(scope, file), null);
		return pipe;
	}

	public boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
		synchronized (scope) {
			IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
			if (basicScope == null) {
				basicScope = new BroadcastScope(scope, name);
				scope.addChildScope(basicScope);
				((IBroadcastScope) basicScope).subscribe(bs.getProvider(), null);
				return true;
			} else if (!(basicScope instanceof IBroadcastScope)) {
				return false;
			} else {
				((IBroadcastScope) basicScope).subscribe(bs.getProvider(), null);
				return true;
			}
		}
	}

	public List<String> getBroadcastStreamNames(IScope scope) {
		synchronized (scope) {
			// TODO: return result of "getBasicScopeNames" when the api has changed
			//       to not return iterators
			List<String> result = new ArrayList<String>();
			Iterator<String> it = scope.getBasicScopeNames(IBroadcastScope.TYPE);
			while (it.hasNext()) {
				result.add(it.next());
			}
			return result;
		}
	}

	public boolean unregisterBroadcastStream(IScope scope, String name) {
		synchronized (scope) {
			IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
			if (basicScope instanceof IBroadcastScope) {
				scope.removeChildScope(basicScope);
				return true;
			}
			return false;
		}
	}

	private String getStreamDirectory() {
		return "streams/";
	}
	
	private String getStreamFilename(IScope scope, String name) {
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.KEY);
		if (!name.contains(":") && !name.contains("."))
			// Default to .flv files if no prefix and no extension is given.
			name = "flv:" + name;
		
		for (IStreamableFileService service: factory.getServices()) {
			if (name.startsWith(service.getPrefix() + ":")) {
				name = service.prepareFilename(name);
				break;
			}
		}
		
		return getStreamDirectory() + name;
	}
	
}
