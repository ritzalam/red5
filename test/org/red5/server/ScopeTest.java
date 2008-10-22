package org.red5.server;

import org.junit.Ignore;
import org.junit.Test;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * This is for testing Scope issues. First created to address:
 * http://jira.red5.org/browse/APPSERVER-278
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ScopeTest extends AbstractDependencyInjectionSpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(ScopeTest.class);
	
	private static WebScope appScope;
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "bin");
		System.setProperty("red5.config_root", "bin/conf");
	}
	
	@Override
	protected String[] getConfigLocations() {
		return new String[] { "org/red5/server/ScopeTest.xml" };
	}

	@Override
	protected void onSetUp() throws Exception {
		System.out.println("onSetUp");
		super.onSetUp();
			
		if (appScope == null) {
			
			log.debug("Property - user.dir: {}", System.getProperty("user.dir"));
			log.debug("Property - red5.root: {}", System.getProperty("red5.root"));
			log.debug("Property - red5.config_root: {}", System.getProperty("red5.config_root"));
			
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
		}
	}

	@Override
	protected void onTearDown() throws Exception {
		System.out.println("onTearDown");
		super.onTearDown();
	}	
	
	@Ignore
	@Test
	public void testScope() {
		
		//Room 1
		assertTrue(appScope.createChildScope("room1"));
		IScope room1 = appScope.getScope("room1");
		log.debug("Room 1: {}", room1);
		
		IContext rmCtx1 = room1.getContext();
		log.debug("Context 1: {}", rmCtx1);
		
		//Room 2
		assertTrue(room1.createChildScope("room2"));
		IScope room2 = room1.getScope("room2");
		log.debug("Room 2: {}", room2);
		
		IContext rmCtx2 = room2.getContext();
		log.debug("Context 2: {}", rmCtx2);
		
		//Room 3
		assertTrue(room2.createChildScope("room3"));
		IScope room3 = room2.getScope("room3");
		log.debug("Room 3: {}", room3);
		
		IContext rmCtx3 = room3.getContext();
		log.debug("Context 3: {}", rmCtx3);		
		
		//Room 4 attaches at Room 1 (per bug example)
		assertTrue(room1.createChildScope("room4"));
		IScope room4 = room1.getScope("room4");
		log.debug("Room 4: {}", room4);
		
		IContext rmCtx4 = room4.getContext();
		log.debug("Context 4: {}", rmCtx4);		
		
		//Room 5
		assertTrue(room4.createChildScope("room5"));
		IScope room5 = room4.getScope("room5");
		log.debug("Room 5: {}", room5);
		
		IContext rmCtx5 = room5.getContext();
		log.debug("Context 5: {}", rmCtx5);			
		
		//Context ctx = new Context();
		//ctx.setApplicationContext(applicationContext);
		
		//Scope scope = new DummyScope();
		//scope.setName("");
		//scope.setContext(ctx);
		
	}

	private class DummyScope extends Scope {

	}	
	
}
