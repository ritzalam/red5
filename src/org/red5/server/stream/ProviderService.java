package org.red5.server.stream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IScope;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.stream.pipe.RefCountPushPushPipe;
import org.red5.server.stream.provider.FileProvider;

public class ProviderService implements IProviderService {
	private Map<String, IPipe> pipeMap = new HashMap<String, IPipe>();
	
	public IMessageInput getProviderInput(IScope scope, String name) {
		IMessageInput msgIn = getLiveProviderInput(scope, name, false);
		if (msgIn == null) return getVODProviderInput(scope, name);
		return msgIn;
	}

	public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
		IPipe pipe = null;
		synchronized (pipeMap) {
			pipe = pipeMap.get(name);
			if (pipe == null && needCreate) {
				pipe = new RefCountPushPushPipe();
				// TODO remove the pipe when no provider/consumer left
				pipeMap.put(name, pipe);
			}
		}
		return pipe;
	}

	public IMessageInput getVODProviderInput(IScope scope, String name) {
		File file = null;
		try {
			file = scope.getResources(getStreamFilename(name))[0].getFile();
		} catch (IOException e) {}
		if (file == null) {
			return null;
		}
		IPipe pipe = new InMemoryPullPullPipe();
		pipe.subscribe(new FileProvider(scope, file), null);
		return pipe;
	}
	
	public void registerLiveProvider(IScope scope, String name, IProvider provider) {
		IPipe pipe = null;
		synchronized (pipeMap) {
			pipe = pipeMap.get(name);
			if (pipe == null) {
				pipe = new RefCountPushPushPipe();
				// TODO remove the pipe when no provider/consumer left
				pipeMap.put(name, pipe);
			}
		}
		pipe.subscribe(provider, null);
	}

	private String getStreamDirectory() {
		return "streams/";
	}
	
	private String getStreamFilename(String name) {
		return getStreamFilename(name, null);
	}
	
	private String getStreamFilename(String name, String extension) {
		String result = getStreamDirectory() + name;
		if (extension != null && !extension.equals(""))
			result += extension;
		return result;
	}
}
