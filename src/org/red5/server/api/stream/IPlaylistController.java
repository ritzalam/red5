package org.red5.server.api.stream;

/**
 * A play list controller that controls the order of play items.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPlaylistController {
	/**
	 * Get next item to play.
	 * @param playlist The related play list.
	 * @param itemIndex The current item index. <tt>-1</tt> indicates
	 * to retrieve the first item for play.
	 * @return The next item index to play. <tt>-1</tt> reaches the end.
	 */
	int nextItem(IPlaylist playlist, int itemIndex);
	
	/**
	 * Get previous item to play.
	 * @param playlist The related play list.
	 * @param itemIndex The current item index. <tt>IPlaylist.itemSize</tt> indicated
	 * to retrieve the last item for play.
	 * @return The previous item index to play. <tt>-1</tt> reaches the beginning.
	 */
	int previousItem(IPlaylist playlist, int itemIndex);
}
