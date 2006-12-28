package org.red5.io.object;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.red5.server.net.remoting.RemotingClient;

/**
 * Readonly RecordSet object that might be received through remoting.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @see <a href="http://www.osflash.org/amf/recordset">osflash.org documentation</a>
 */
public class RecordSet {

	private static final String MODE_ONDEMAND = "ondemand";

	private static final String MODE_FETCHALL = "fetchall";

	private static final String MODE_PAGE = "page";

	private int totalCount;

	private List<List<Object>> data;

	private int cursor;

	private String serviceName;

	private List<String> columns;

	private int version;

	private Object id;

	private RemotingClient client = null;

	private String mode = MODE_ONDEMAND;

	private int pageSize = 25;

	public RecordSet(Input input) {
		Deserializer deserializer = new Deserializer();
		Map<String, Object> dataMap = new HashMap<String, Object>();
		while (input.hasMoreProperties()) {
			String key = input.readPropertyName();
			Object value = deserializer.deserialize(input);
			dataMap.put(key, value);
		}
		input.skipEndObject();

		Map<String, Object> serverInfo = (Map<String, Object>) dataMap
				.get("serverinfo");
		if (serverInfo == null) {
			// This is right according to the specs on osflash.org
			serverInfo = (Map<String, Object>) dataMap.get("serverInfo");
		}

		if (!(serverInfo instanceof Map)) {
			throw new RuntimeException("Expected Map but got " + serverInfo);
		}

		totalCount = (Integer) serverInfo.get("totalCount");
		List<List<Object>> initialData = (List<List<Object>>) serverInfo
				.get("initialData");
		cursor = (Integer) serverInfo.get("cursor");
		serviceName = (String) serverInfo.get("serviceName");
		columns = (List<String>) serverInfo.get("columnNames");
		version = (Integer) serverInfo.get("version");
		id = serverInfo.get("id");

		this.data = new ArrayList<List<Object>>(totalCount);
		for (int i = 0; i < initialData.size(); i++) {
			this.data.add(i + cursor - 1, initialData.get(i));
		}
	}

	/**
	 * Set the remoting client to use for retrieving of paged results.
	 * 
	 * @param client
	 */
	public void setRemotingClient(RemotingClient client) {
		this.client = client;
	}

	/**
	 * Set the mode for fetching paged results.
	 * 
	 * @param mode
	 */
	public void setDeliveryMode(String mode) {
		setDeliveryMode(mode, 25, 0);
	}

	/**
	 * Set the mode for fetching paged results.
	 * 
	 * @param mode
	 * @param pageSize
	 */
	public void setDeliveryMode(String mode, int pageSize) {
		setDeliveryMode(mode, pageSize, 0);
	}

	/**
	 * Set the mode for fetching paged results.
	 * 
	 * @param mode
	 * @param pageSize
	 * @param prefetchCount
	 */
	public void setDeliveryMode(String mode, int pageSize, int prefetchCount) {
		this.mode = mode;
		this.pageSize = pageSize;
	}

	/**
	 * Return a list containing the names of the columns in the recordset.
	 * 
	 * @return column names
	 */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Make sure the passed item has been fetched from the server.
	 * 
	 * @param index
	 */
	private void ensureAvailable(int index) {
		if (data.get(index) != null) {
			// Already have this item.
			return;
		}

		if (client == null) {
			throw new RuntimeException("no remoting client configured");
		}

		Object result;
		int start = index;
		int count;
		if (mode.equals(MODE_ONDEMAND)) {
			// Only get requested item
			count = 1;
		} else if (mode.equals(MODE_FETCHALL)) {
			// Get remaining items
			count = totalCount - cursor;
		} else if (mode.equals(MODE_PAGE)) {
			// Get next page
			// TODO: implement prefetching of multiple pages
			count = 1;
			for (int i = 1; i < pageSize; i++) {
				if (this.data.get(start + i) == null) {
					count += 1;
				}
			}
		} else {
			// Default to "ondemand"
			count = 1;
		}

		result = client.invokeMethod(serviceName + ".getRecords", new Object[] {
				id, start + 1, count });
		if (!(result instanceof RecordSetPage)) {
			throw new RuntimeException("expected RecordSetPage but got "
					+ result);
		}

		RecordSetPage page = (RecordSetPage) result;
		if (page.getCursor() != start + 1) {
			throw new RuntimeException("expected offset " + (start + 1)
					+ " but got " + page.getCursor());
		}

		List<List<Object>> data = page.getData();
		if (data.size() != count) {
			throw new RuntimeException("expected " + count
					+ " results but got " + data.size());
		}

		// Store received items
		for (int i = 0; i < count; i++) {
			this.data.add(start + i, data.get(i));
		}
	}

	/**
	 * Return a specified item from the recordset.  If the item is not
	 * available yet, it will be received from the server.
	 * 
	 * @param index
     * @return
	 */
	public List<Object> getItemAt(int index) {
		if (index < 0 || index >= totalCount) {
			// Out of range
			return null;
		}

		ensureAvailable(index);
		return data.get(index);
	}

	/**
	 * Get the total number of items.
	 * 
	 * @return number of items
	 */
	public int getLength() {
		return totalCount;
	}

	/**
	 * Get the number of items already received from the server.
	 * 
	 * @return number of received items
	 */
	public int getNumberAvailable() {
		int result = 0;
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) != null) {
				result += 1;
			}
		}
		return result;
	}

	/**
	 * Check if all items are available on the client. 
	 * 
	 * @return number of available items
	 */
	public boolean isFullyPopulated() {
		return getNumberAvailable() == getLength();
	}

	/**
	 * Return Map that can be serialized as result.
	 * 
	 * @return serializable informations
	 */
	public Map<String,Object> serialize() {
		Map<String, Object> serverInfo = new HashMap<String, Object>();
		serverInfo.put("totalCount", totalCount);
		serverInfo.put("cursor", cursor);
		serverInfo.put("serviceName", serviceName);
		serverInfo.put("columnNames", columns);
		serverInfo.put("version", version);
		serverInfo.put("id", id);
		serverInfo.put("initialData", data);
		
		return serverInfo;
	}
}
