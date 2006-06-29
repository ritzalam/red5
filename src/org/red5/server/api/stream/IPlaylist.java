package org.red5.server.api.stream;

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
