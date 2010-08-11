package org.red5.server.api;

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

import static junit.framework.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.so.SharedObjectService;

public class SharedObjectTest extends BaseTest implements IEventListener {

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SharedObjectTest.class);
	}

	protected String name = "testso";

	/** {@inheritDoc} */
    public void notifyEvent(IEvent event) {
		log.debug("Event: {}", event);
	}

	@Test
	public void sharedObjectService() {
		IScope scope = context.resolveScope(path_app);
		ISharedObjectService service = new SharedObjectService();
		assertTrue("should be empty", !service.hasSharedObject(scope, "blah"));
		assertTrue("create so", service.createSharedObject(scope, name, false));
		assertTrue("so exists?", service.hasSharedObject(scope, name));
		ISharedObject so = service.getSharedObject(scope, name);
		assertTrue("so not null", so != null);
		assertTrue("name same", so.getName().equals(name));
		//assertTrue("persistent",!so.isPersistent());
		so.addEventListener(this);
		so.setAttribute("this", "that");
	}

}
