package org.red5.server.api;

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
 * Interface for setting/getting bandwidth configure.
 * If overallBandwidth is <tt>-1</tt>, A/V bandwidth settings are valid.
 * Or overallBandwidth will be used, thus Audio and Video share the
 * bandwidth setting.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IBandwidthConfigure {
	void setAudioBandwidth(long bw);
	long getAudioBandwidth();
	void setVideoBandwidth(long bw);
	long getVideoBandwidth();
	void setOverallBandwidth(long bw);
	long getOverallBandwidth();
	
	/**
	 * Set the maximum amount of burst in Byte.
	 * This values controls the maximum amount of packets that
	 * can burst to the client at any time.
	 * @param maxBurst The maximum burst amount. If <tt>0</tt> or less
	 * is specified, the system default value will be used.
	 */
	void setMaxBurst(long maxBurst);
	long getMaxBurst();
	
	/**
	 * Set the burst amount in Byte. The burst amount controls
	 * the initial amount of packets that burst to the client.
	 * It should be no bigger than the maximum burst amount. If it is,
	 * the maximum burst value will be used instead.
	 * @param burst The burst amount. A value that is not bigger than
	 * <tt>0</tt> means don't use burst.
	 */
	void setBurst(long burst);
	long getBurst();
}
