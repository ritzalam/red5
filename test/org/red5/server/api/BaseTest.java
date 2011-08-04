package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

//http://static.springframework.org/spring/docs/2.5.x/reference/testing.html
@ContextConfiguration(locations={"context.xml"})
public class BaseTest extends AbstractJUnit4SpringContextTests {

	protected Logger log = LoggerFactory.getLogger(BaseTest.class);
	
	static final String red5root = "dist";
	static final String red5conf = red5root + "/conf";
	static IContext context = null;

	static final String host = "localhost";

	static final String path_app = "default";

	static final String path_room = "default/test";

	static {
		System.setProperty("red5.deployment.type", "junit");
		// Get the full path name
		System.setProperty("red5.root", red5root);
		System.setProperty("red5.config_root", red5conf);
		
		System.setProperty("sun.lang.ClassLoader.allowArraySyntax", "true");
    	System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}
	
	@Before
	public void setUp() throws Exception {
		context = (IContext) applicationContext.getBean("red5.context");
	}
	
	@Test
	public void testCreation() {
		// Doesn't do anything except make sure initialization works
	}

}
