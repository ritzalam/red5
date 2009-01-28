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
package org.red5.logging;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author aclarke
 *
 */
public class Red5LoggerFactoryTest {

	/**
	 * Test method for {@link org.red5.logging.Red5LoggerFactory#getLogger(java.lang.Class)}.
	 */
	@Test
	public void testGetLoggerClass() {
		  final Logger log = Red5LoggerFactory.getLogger(this.getClass());
		  assertNotNull(log);
	}

	/**
	 * Test method for {@link org.red5.logging.Red5LoggerFactory#getLogger(java.lang.Class, java.lang.String)}.
	 * 
	 * This test will fail before http://jira.red5.org/browse/APPSERVER-341 is fixed
	 * with a NullPointerException
	 */
	@Test
	public void testGetLoggerClassString() {
		  final Logger log = Red5LoggerFactory.getLogger(this.getClass(), "doesnotexist");
		  assertNotNull("should fall back to default logger", log);
	}

}
