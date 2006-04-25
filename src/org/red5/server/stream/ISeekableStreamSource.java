package org.red5.server.stream;

/**
 * Stream source that can be seeked in timeline
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ISeekableStreamSource extends IStreamSource {
	/**
	 * Seek the stream source to timestamp ts (in milliseconds).
	 * @param ts Timestamp to seek to
	 * @return Actual timestamp seeked to
	 */
	int seek(int ts);
}
