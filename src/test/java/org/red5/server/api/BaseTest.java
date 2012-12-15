/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.api;

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
