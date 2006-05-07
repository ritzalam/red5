package org.red5.server.stream;

import java.io.File;
import java.io.IOException;

import org.red5.server.api.IScope;
import org.red5.server.messaging.IProvider;
import org.red5.server.stream.provider.FileProvider;

public class ProviderService implements IProviderService {
	
	public IProvider getProvider(IScope scope, String name) {
		File file = null;
		try {
			file = scope.getResources(getStreamFilename(name))[0].getFile();
		} catch (IOException e) {}
		if (file == null) {
			return null;
		}
		return new FileProvider(scope, file);
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
