package org.red5.server.api.test;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.red5.server.api.IContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import static junit.framework.Assert.assertTrue;

public class BaseTest {

	protected static Log log =
        LogFactory.getLog(ScopeTest.class.getName());
	
	static final String config = "test/org/red5/server/api/test/context.xml";
	static final String host = "localhost";
	static final String path_app = "test";
	static final String path_room = "test/room";
	
	static ApplicationContext spring = null;
	static IContext context = null;

	@BeforeClass public static void setup(){
		spring = new FileSystemXmlApplicationContext(config);
		context = (IContext) spring.getBean("red5.context");
	}
	
}