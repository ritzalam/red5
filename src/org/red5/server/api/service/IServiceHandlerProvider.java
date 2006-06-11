package org.red5.server.api.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

import java.util.Set;
import org.red5.server.api.IScope;

/**
 * Supports registration and lookup of service handlers.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public interface IServiceHandlerProvider {

	/**
	 * Register an object that provides methods which can be called from a client.
	 * 
	 * <p>Example:<br>
	 * If you registered a handler with the name "<code>one.two</code>" that
	 * provides a method "<code>callMe</code>", you can call a method
	 * "<code>one.two.callMe</code>" from the client.</p>
	 * 
	 * @param scope
	 * 			the scope this handler applies to
	 * @param name
	 * 			the name of the handler
	 * @param handler
	 * 			the handler object
	 */
	public void registerServiceHandler(IScope scope, String name, Object handler);
	
	/**
	 * Unregister service handler.
	 * 
	 * @param scope
	 * 			the scope this handler was registered for
	 * @param name
	 * 			the name of the handler
	 */
	public void unregisterServiceHandler(IScope scope, String name);
	
	/**
	 * Return a previously registered service handler.
	 * 
	 * @param scope
	 * 			the scope to lookup the handler for
	 * @param name
	 * 			the name of the handler to return
	 * @return the previously registered handler
	 */
	public Object getServiceHandler(IScope scope, String name);

	/**
	 * Get list of registered service handler names.
	 * 
	 * @param scope
	 * 			the scope to return the handler names for
	 * @return the names of the registered handlers
	 */
	public Set<String> getServiceHandlerNames(IScope scope);

}
