package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.BasicScope;
import org.red5.server.Scope;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.stream.provider.FileProvider;
import org.slf4j.Logger;

public class ProviderService implements IProviderService {

	private static final Logger log = Red5LoggerFactory.getLogger(ProviderService.class);

	/** {@inheritDoc} */
	public INPUT_TYPE lookupProviderInput(IScope scope, String name) {
		INPUT_TYPE result = INPUT_TYPE.NOT_FOUND;
		if (scope.getBasicScope(IBroadcastScope.TYPE, name) != null) {
			//we have live input
			result = INPUT_TYPE.LIVE;
		} else {
			try {
				File file = getStreamFile(scope, name);
				if (file != null) {
					//we have vod input
					result = INPUT_TYPE.VOD;
					//null it to prevent leak or file locking
					file = null;
				}
			} catch (IOException e) {
				log.warn("Exception attempting to lookup file: {}", name, e);
				e.printStackTrace();
			}
		}
		return result;
	}

	/** {@inheritDoc} */
	public IMessageInput getProviderInput(IScope scope, String name) {
		IMessageInput msgIn = getLiveProviderInput(scope, name, false);
		if (msgIn == null) {
			return getVODProviderInput(scope, name);
		}
		return msgIn;
	}

	/** {@inheritDoc} */
	public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
		log.debug("Get live provider input for {} scope: {}", name, scope);
		if (log.isDebugEnabled()) {
			((Scope) scope).dump();
		}
		//make sure the create is actually needed
		IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
		if (basicScope == null) {
			if (needCreate) {
				// Re-check if another thread already created the scope
				basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
				if (basicScope == null) {
					basicScope = new BroadcastScope(scope, name);
					scope.addChildScope(basicScope);
				}
			} else {
				return null;
			}
		}
		if (!(basicScope instanceof IBroadcastScope)) {
			return null;
		}
		return (IBroadcastScope) basicScope;
	}

	/** {@inheritDoc} */
	public IMessageInput getVODProviderInput(IScope scope, String name) {
		log.debug("getVODProviderInput - scope: {} name: {}", scope, name);
		File file = getVODProviderFile(scope, name);
		if (file == null) {
			return null;
		}
		IPipe pipe = new InMemoryPullPullPipe();
		pipe.subscribe(new FileProvider(scope, file), null);
		return pipe;
	}

	/** {@inheritDoc} */
	public File getVODProviderFile(IScope scope, String name) {
		log.debug("getVODProviderFile - scope: {} name: {}", scope, name);
		File file = null;
		try {
			log.trace("getVODProviderFile scope path: {} name: {}", scope.getContextPath(), name);
			file = getStreamFile(scope, name);
		} catch (IOException e) {
			log.error("Problem getting file: {}", name, e);
		}
		if (file == null || !file.exists()) {
			//if there is no file extension this is most likely a live stream
			if (name.indexOf('.') > 0) {
				log.info("File was null or did not exist: {}", name);
			} else {
				log.trace("VOD file {} was not found, may be live stream", name);
			}
			return null;
		}
		return file;
	}

	/** {@inheritDoc} */
	public boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
		log.debug("Registering - name: {} stream: {} scope: {}", new Object[] { name, bs, scope });
		if (log.isDebugEnabled()) {
			((Scope) scope).dump();
		}
		boolean status = false;
		IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
		if (basicScope == null) {
			log.debug("Creating a new scope");
			basicScope = new BroadcastScope(scope, name);
			if (scope.addChildScope(basicScope)) {
				log.debug("Broadcast scope added");
			} else {
				log.warn("Broadcast scope was not added to {}", scope);
			}
		}
		if (basicScope instanceof IBroadcastScope) {
			log.debug("Subscribing scope {} to provider {}", basicScope, bs.getProvider());
			status = ((IBroadcastScope) basicScope).subscribe(bs.getProvider(), null);
		}
		return status;
	}

	/** {@inheritDoc} */
	public List<String> getBroadcastStreamNames(IScope scope) {
		// TODO: return result of "getBasicScopeNames" when the api has
		// changed to not return iterators
		List<String> result = new ArrayList<String>();
		Iterator<String> it = scope.getBasicScopeNames(IBroadcastScope.TYPE);
		while (it.hasNext()) {
			result.add(it.next());
		}
		it = null;
		return result;
	}

	/** {@inheritDoc} */
	public boolean unregisterBroadcastStream(IScope scope, String name) {
		return unregisterBroadcastStream(scope, name, null);
	}

	/** {@inheritDoc} */
	public boolean unregisterBroadcastStream(IScope scope, String name, IBroadcastStream bs) {
		log.debug("Unregistering - name: {} stream: {} scope: {}", new Object[] { name, bs, scope });
		if (log.isDebugEnabled()) {
			((Scope) scope).dump();
		}
		boolean status = false;
		IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
		if (basicScope instanceof IBroadcastScope) {
			if (bs != null) {
				log.debug("Unsubscribing scope {} from provider {}", basicScope, bs.getProvider());
				((IBroadcastScope) basicScope).unsubscribe(bs.getProvider());
			}
			//if the scope has no listeners try to remove it
			if (!((BasicScope) basicScope).hasEventListeners()) {
				log.debug("Scope has no event listeners attempting removal");
				scope.removeChildScope(basicScope);
			}
			if (log.isDebugEnabled()) {
				//verify that scope was removed
				if (scope.getBasicScope(IBroadcastScope.TYPE, name) == null) {
					log.debug("Scope was removed");
				} else {
					log.debug("Scope was not removed");
				}
			}
			status = true;
		}
		return status;
	}

	private File getStreamFile(IScope scope, String name) throws IOException {
		IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope,
				IStreamableFileFactory.class);
		if (name.indexOf(':') == -1 && name.indexOf('.') == -1) {
			// Default to .flv files if no prefix and no extension is given.
			name = "flv:" + name;
		}
		log.debug("getStreamFile null check - factory: {} name: {}", factory, name);
		for (IStreamableFileService service : factory.getServices()) {
			if (name.startsWith(service.getPrefix() + ':')) {
				name = service.prepareFilename(name);
				break;
			}
		}

		IStreamFilenameGenerator filenameGenerator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope,
				IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);

		String filename = filenameGenerator.generateFilename(scope, name, GenerationType.PLAYBACK);
		File file;
		//most likely case first
		if (!filenameGenerator.resolvesToAbsolutePath()) {
			file = scope.getContext().getResource(filename).getFile();
		} else {
			file = new File(filename);
		}
		//check files existence
		if (file != null && !file.exists()) {
			//if it does not exist then null it out
			file = null;
		}
		return file;

	}

}
