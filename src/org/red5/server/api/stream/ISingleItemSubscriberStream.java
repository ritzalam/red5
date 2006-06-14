package org.red5.server.api.stream;

/**
 * A subscriber stream that has only one item for play.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ISingleItemSubscriberStream extends ISubscriberStream {
	void setPlayItem(IPlayItem item);
}
