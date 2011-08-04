package org.red5.server.net.rtmps;

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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Testing RTMPS client.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations={"../../service/testcontext.xml"})
public class RTMPSClientTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(RTMPSClientTest.class);
		
	static {
		String userDir = System.getProperty("user.dir");
		System.out.println("User dir: " + userDir);
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "file:" + userDir + "/bin");
		System.setProperty("red5.config_root", System.getProperty("red5.root") + "/conf");
	}
	
	{
		log.debug("Property - user.dir: {}", System.getProperty("user.dir"));
		log.debug("Property - red5.root: {}", System.getProperty("red5.root"));
		log.debug("Property - red5.config_root: {}", System.getProperty("red5.config_root"));
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testStartup() {
		Assert.assertTrue(true);
	}
	
}
