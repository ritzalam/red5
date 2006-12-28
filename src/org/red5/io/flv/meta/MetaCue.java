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

import java.util.HashMap;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class MetaCue<K, V> extends HashMap<String, Object> implements IMetaCue {

	/**
	 * SerialVersionUID = -1769771340654996861L;
	 */
	private static final long serialVersionUID = -1769771340654996861L;

	/**
	 * CuePoint constructor
	 */
	public MetaCue() {

	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.put("name", name);
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#getName()
	 */
	public String getName() {
		return (String) this.get("name");
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#setType(java.lang.String)
	 */
	public void setType(String type) {
		this.put("type", type);
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#getType()
	 */
	public String getType() {
		return (String) this.get("type");
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#setTime(double)
	 */
	public void setTime(double d) {
		this.put("time", d);
	}

	/** {@inheritDoc} */ /*
	 * (non-Javadoc)
	 * 
	 * @see org.red5.io.flv.ICuePoint#getTime()
	 */
	public double getTime() {
		return (Double) this.get("time");
	}

	/** {@inheritDoc} */
    public int compareTo(Object arg0) {
		MetaCue cp = (MetaCue) arg0;
		double cpTime = cp.getTime();
		double thisTime = this.getTime();

		if (cpTime > thisTime) {
			return -1;
		} else if (cpTime < thisTime) {
			return 1;
		}

		return 0;
	}

	/** {@inheritDoc} */
    @Override
	public String toString() {
		return "MetaCue{" + this + '}';
	}
}
