package org.red5.server.api;

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
