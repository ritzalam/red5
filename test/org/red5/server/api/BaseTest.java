package org.red5.server.api;

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
