package org.red5.server.stream;

import org.red5.server.messaging.IProvider;

public interface ISeekableProvider extends IProvider {
	public static final String KEY = ISeekableProvider.class.getName();
	
	/**
	 * Seek the provider to timestamp ts (in milliseconds).
	 * @param ts Timestamp to seek to
	 * @return Actual timestamp seeked to
	 */
	int seek(int ts);
}
