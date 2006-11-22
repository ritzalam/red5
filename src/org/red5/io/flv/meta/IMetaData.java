package org.red5.io.flv.meta;

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

/**
 * MetaData interface
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * 
 *  Sample Data:
 *  private boolean canSeekToEnd = true;
 *  private int videocodecid = 4;
 *  private int framerate = 15;
 *  private int videodatarate = 400;
 *  private int height = 215;
 *  private int width = 320;
 *  private int duration = 7.347;
 */
public interface IMetaData<K, V> extends IMeta {

	/**
	 * Returns a boolean depending on whether the video can
	 * seek to end
	 * @return canSeekToEnd
	 */
	public boolean getCanSeekToEnd();

	/**
	 * Sets whether a video can seek to end
	 * @param b
	 */
	public void setCanSeekToEnd(boolean b);

	/**
	 * Returns the video codec id
	 * @return videocodecid
	 */
	public int getVideoCodecId();

	/**
	 * Sets the video codec id
	 * @param id
	 */
	public void setVideoCodecId(int id);

	/**
	 * Returns the framerate
	 * @return framerate
	 */
	public int getframeRate();

	/**
	 * Sets the framerate
	 * @param rate
	 */
	public void setframeRate(int rate);

	/**
	 * Returns the videodatarate
	 * @return framerate
	 */
	public int getVideoDataRate();

	/**
	 * Sets the videodatarate
	 * @param rate
	 */
	public void setVideoDataRate(int rate);

	/**
	 * Returns the height
	 * @return height
	 */
	public int getHeight();

	/**
	 * Sets the height
	 * @param h height
	 */
	public void setHeight(int h);

	/**
	 * Returns the width
	 * @return width
	 */
	public int getWidth();

	/**
	 * Sets the width
	 * @param w width
	 */
	public void setWidth(int w);

	/**
	 * Returns the duration
	 * @return duration
	 */
	public double getDuration();

	/**
	 * Sets the duration
	 * @param d duration
	 */
	public void setDuration(double d);

	/**
	 * Sets the cue points
	 * @param metaCue
	 */
	public void setMetaCue(IMetaCue[] metaCue);

	/**
	 * Gets the cue points
	 * @return IMetaCue[]
	 */
	public IMetaCue[] getMetaCue();
}
