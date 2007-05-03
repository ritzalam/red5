/*
 * Copyright 2004-2005 the original author.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.red5.server.pooling;

/**
 * JMX interface for TheadPool instrumentation.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ThreadPoolMBean {

	public int getMaxActive();

	public int getMaxIdle();

	public long getMaxWait();

	public long getMinEvictableIdleTimeMillis();

	public int getMinIdle();

	public int getNumActive();

	public int getNumIdle();

	public int getNumTestsPerEvictionRun();

	public long getSoftMinEvictableIdleTimeMillis();

	public boolean getTestOnBorrow();

	public boolean getTestOnReturn();

	public boolean getTestWhileIdle();

	public long getTimeBetweenEvictionRunsMillis();

	public byte getWhenExhaustedAction();

	//setters for active modification
	public void setMaxActive(int max);

	public void setMaxIdle(int max);

	public void setMaxWait(long max);
}
