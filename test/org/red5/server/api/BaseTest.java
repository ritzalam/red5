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

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class BaseTest {

	static final String red5root = "dist";
	static final String red5conf = red5root + "/conf";
	static final String config = red5conf+"/org/red5/server/api/context.xml";

	static IContext context = null;

	static final String host = "localhost";

	protected Logger log = LoggerFactory.getLogger(BaseTest.class);

	static final String path_app = "default";

	static final String path_room = "default/test";

	static ApplicationContext spring = null;

	@BeforeClass
	public static void setup() {
		// Get the full path name
		System.setProperty("red5.root", red5root);
		System.setProperty("red5.config_root", red5conf);

		spring = new FileSystemXmlApplicationContext(config);
		context = (IContext) spring.getBean("red5.context");
	}
	
	@Test
	public void testCreation()
	{
		// Doesn't do anything except make sure initialization works
	}

}
