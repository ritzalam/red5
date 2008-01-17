package org.red5.server.api;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class BaseTest {

	static final String config = "test/org/red5/server/api/test/context.xml";

	static IContext context = null;

	static final String host = "localhost";

	protected Logger log = LoggerFactory.getLogger(BaseTest.class);

	static final String path_app = "test";

	static final String path_room = "test/room";

	static ApplicationContext spring = null;

	@BeforeClass
	public static void setup() {
		spring = new FileSystemXmlApplicationContext(config);
		context = (IContext) spring.getBean("red5.context");
	}

}