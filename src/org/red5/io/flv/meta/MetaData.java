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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * MetaData Implementation
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 *
 *          Example:
 *
 *          //	private boolean canSeekToEnd = true;
 *          //	private int videocodecid = 4;
 *          //	private int framerate = 15;
 *          //	private int videodatarate = 600;
 *          //	private int height;
 *          //	private int width = 320;
 *          //	private double duration = 7.347;
 */
public class MetaData<K, V> extends HashMap<String, Object> implements
		IMetaData {

	/** serialVersionUID = -5681069577717669925L; */
	private static final long serialVersionUID = -5681069577717669925L;

	IMetaCue cuePoints[]; //CuePoint array

	/** MetaData constructor */
	public MetaData() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getCanSeekToEnd()
	 */
	public boolean getCanSeekToEnd() {
		return (Boolean) this.get("canSeekToEnd");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setCanSeekToEnd(boolean)
	 */
	public void setCanSeekToEnd(boolean b) {
		this.put("canSeekToEnd", b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getVideoCodecId()
	 */
	public int getVideoCodecId() {
		return (Integer) this.get("videocodecid");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setVideoCodecId(int)
	 */
	public void setVideoCodecId(int id) {
		this.put("videocodecid", id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getframeRate()
	 */
	public int getframeRate() {
		return (Integer) this.get("framerate");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setframeRate(int)
	 */
	public void setframeRate(int rate) {
		this.put("framerate", rate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getVideoDataRate()
	 */
	public int getVideoDataRate() {
		return (Integer) this.get("videodatarate");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setVideoDataRate(int)
	 */
	public void setVideoDataRate(int rate) {
		this.put("videodatarate", rate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getWidth()
	 */
	public int getWidth() {
		return (Integer) this.get("width");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setWidth(int)
	 */
	public void setWidth(int w) {
		this.put("width", w);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getDuration()
	 */
	public double getDuration() {
		return (Double) this.get("duration");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setDuration(int)
	 */
	public void setDuration(double d) {
		this.put("duration", d);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#getHeight()
	 */
	public int getHeight() {
		return (Integer) this.get("height");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.MetaData#setHeight(int)
	 */
	public void setHeight(int h) {
		this.put("height", h);
	}

	/**
	 * Sets the MetaCue Points
	 *
	 * @param cuePoints
	 *            The cuePoints to set.
	 */
	public void setMetaCue(IMetaCue[] cuePoints) {
		this.cuePoints = cuePoints;

		MetaCue<String, Object> cuePointData = new MetaCue<String, Object>();

		// Place in TreeSet for sorting
		TreeSet<IMetaCue> ts = new TreeSet<IMetaCue>();

		for (IMetaCue element : cuePoints) {
			ts.add(element);
		}

		int j = 0;
		while (!ts.isEmpty()) {
			cuePointData.put(String.valueOf(j), ts.first());
			j++;

			ts.remove(ts.first());
		}

		//		"CuePoints", cuePointData
		//					'0',	MetaCue
		//							name, "test"
		//							type, "event"
		//							time, "0.1"
		//					'1',	MetaCue
		//							name, "test1"
		//							type, "event1"
		//							time, "0.5"

		this.put("cuePoints", cuePointData);

	}

	/**
	 * Return array of MetaCue
	 *
	 * @return IMetaCue[] metaCue
	 */
	public IMetaCue[] getMetaCue() {
		IMetaCue ret[];
		MetaCue cue = (MetaCue) this.get("cuePoints");
		Set s = cue.keySet();

		Iterator i = s.iterator();
		ret = new MetaCue[s.size()];
		int counter = 0;
		while (i.hasNext()) {
			ret[counter++] = (IMetaCue) cue.get(i.next());
		}

		return ret;
	}

	@Override
	public String toString() {
		return "MetaData{" + "cuePoints="
				+ (cuePoints == null ? null : Arrays.asList(cuePoints)) + '}';
	}
}
