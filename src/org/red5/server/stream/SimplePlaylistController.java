package org.red5.server.stream;

import org.red5.server.api.stream.IPlaylist;
import org.red5.server.api.stream.IPlaylistController;

public class SimplePlaylistController implements IPlaylistController {

	public int nextItem(IPlaylist playlist, int itemIndex) {
		if (itemIndex < 0) itemIndex = -1;
		int nextIndex = itemIndex + 1;
		if (nextIndex < playlist.getItemSize()) {
			return nextIndex;
		} else return -1;
	}

	public int previousItem(IPlaylist playlist, int itemIndex) {
		if (itemIndex > playlist.getItemSize()) {
			return playlist.getItemSize() - 1;
		}
		return itemIndex - 1;
	}

}
