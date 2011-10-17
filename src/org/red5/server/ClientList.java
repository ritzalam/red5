package org.red5.server;

import java.beans.ConstructorProperties;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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
 * Client list, implemented using weak references to prevent memory leaks.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 * @param <E>
 *            type of class
 */
public class ClientList<E> extends AbstractList<E> {

	private CopyOnWriteArrayList<WeakReference<E>> items = new CopyOnWriteArrayList<WeakReference<E>>();

	@ConstructorProperties(value = { "" })
	public ClientList() {
	}

	@ConstructorProperties({"c"})
	public ClientList(Collection<E> c) {
		addAll(0, c);
	}

	public boolean add(E element) {
		return items.add(new WeakReference<E>(element));
	}

	public void add(int index, E element) {
		items.add(index, new WeakReference<E>(element));
	}

	@Override
	public E remove(int index) {
		WeakReference<E> ref = items.remove(index);
		return ref.get();
	}

	@Override
	public boolean remove(Object o) {
		boolean removed = false;
		E element = null;
		for (WeakReference<E> ref : items) {
			element = ref.get();
			if (element != null && element.equals(o)) {
				ref.clear();
				removed = true;
				break;
			}
		}
		return removed;
	}

	@Override
	public boolean contains(Object o) {
		List<E> list = new ArrayList<E>();
		for (WeakReference<E> ref : items) {
			if (ref.get() != null) {
				list.add(ref.get());
			}
		}
		boolean contains = list.contains(o);
		list.clear();
		list = null;
		return contains;
	}

	public int size() {
		removeReleased();
		return items.size();
	}

	public E get(int index) {
		return (items.get(index)).get();
	}

	private void removeReleased() {
		for (WeakReference<E> ref : items) {
			if (ref.get() == null) {
				items.remove(ref);
			}
		}
	}

}
