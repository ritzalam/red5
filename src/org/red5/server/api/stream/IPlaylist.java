package org.red5.server.api.stream;

public interface IPlaylist {
	/**
	 * Add an item to the list.
	 * @param item
	 */
	void addItem(IPlayItem item);
	
	/**
	 * Add an item to specific index.
	 * @param item
	 * @param index
	 */
	void addItem(IPlayItem item, int index);
	
	/**
	 * Remove an item from list.
	 * @param index
	 */
	void removeItem(int index);
	
	/**
	 * Remove all items.
	 */
	void removeAllItems();
	
	int getItemSize();
	
	/**
	 * Go for the previous played item.
	 */
	void previousItem();
	
	/**
	 * Go for next item decided by controller logic.
	 */
	void nextItem();
	
	/**
	 * Set the current item for playing.
	 * @param index
	 */
	void setItem(int index);
	
	/**
	 * Whether items are randomly played.
	 * @return
	 */
	boolean isRandom();

	/**
	 * Set whether items should be randomly played.
	 * @param random
	 */
	void setRandom(boolean random);
	
	/**
	 * Whether rewind the list.
	 * @return
	 */
	boolean isRewind();
	
	/**
	 * Set whether rewind the list.
	 * @param rewind
	 */
	void setRewind(boolean rewind);
	
	/**
	 * Whether repeat playing an item.
	 * @return
	 */
	boolean isRepeat();
	
	/**
	 * Set whether repeat playing an item.
	 * @param repeat
	 */
	void setRepeat(boolean repeat);
	
	/**
	 * Set list controller.
	 * @param controller
	 */
	void setPlaylistController(IPlaylistController controller);
}
