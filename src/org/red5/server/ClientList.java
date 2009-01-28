package org.red5.server;

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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
 */
public class ClientList<E> extends AbstractList<E> implements ListMBean {

	private static final long serialVersionUID = -3127064371410565215L;

    private ArrayList<WeakReference<E>> items;
	
    public ClientList() {
        items = new ArrayList<WeakReference<E>>();
    }
    
    public ClientList(Collection<E> c) {
        items = new ArrayList<WeakReference<E>>();
        addAll(0, c);
    }
    
	public boolean add(E element) {
		return items.add(new WeakReference<E>(element));		
	}

	public void add(int index, E element) {
		items.add(index, new WeakReference<E>(element));
    }
        
    public int size() {
        removeReleased();
        return items.size();
    }    
    
    public E get(int index) {
        return ((WeakReference<E>) items.get(index)).get();
    }
    
    private void removeReleased() {
    	for (WeakReference<E> ref : items) {
            if (ref.get() == null) {
            	items.remove(ref);
            }
    	}
    }	
	
}
