package org.red5.server.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class BaseTest {

	static final String config = "test/org/red5/server/api/test/context.xml";

	static IContext context = null;

	static final String host = "localhost";

	protected static Log log = LogFactory.getLog(ScopeTest.class.getName());

	static final String path_app = "test";

	static final String path_room = "test/room";

	static ApplicationContext spring = null;

	@BeforeClass
	public static void setup() {
		spring = new FileSystemXmlApplicationContext(config);
		context = (IContext) spring.getBean("red5.context");
	}

}