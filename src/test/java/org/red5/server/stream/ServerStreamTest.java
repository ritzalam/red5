package org.red5.server.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.api.stream.support.StreamUtils;
import org.red5.server.scope.GlobalScope;
import org.red5.server.scope.Scope;

public class ServerStreamTest {

	private IServerStream serverStream;

	@Before
	public void setUp() throws Exception {
		IScope scope = new Scope.Builder((IScope) new GlobalScope(), ScopeType.APPLICATION, "testapp", false).build();
		serverStream = StreamUtils.createServerStream(scope, "test");
	}

	@After
	public void tearDown() throws Exception {
		serverStream.removeAllItems();
	}

	@Test
	public void testAddItemIPlayItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
		assertTrue(serverStream.getItemSize() == 2);
	}

	@Test
	public void testAddItemIPlayItemInt() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		SimplePlayItem item3 = SimplePlayItem.build("f3");
		serverStream.addItem(item3, 0);
		System.out.println("Items: " + ((ServerStream) serverStream).getItems());
		assertTrue(serverStream.getItemSize() == 3);
		assertTrue("f1".equals(serverStream.getItem(1).getName()));
	}

	@Test
	public void testRemoveItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		assertTrue(serverStream.getItemSize() == 2);
		serverStream.removeItem(0);
		assertTrue(serverStream.getItemSize() == 1);
	}

	@Test
	public void testRemoveAllItems() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue(serverStream.getItemSize() == 1);
		serverStream.removeAllItems();
		assertTrue(serverStream.getItemSize() == 0);
		assertTrue(serverStream.getCurrentItemIndex() == 0);
	}

	@Test
	public void testGetCurrentItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		serverStream.start();
		assertEquals(item, serverStream.getCurrentItem());
	}

	@Test
	public void testGetItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		assertTrue("f1".equals(serverStream.getItem(0).getName()));
	}

	@Test
	public void testNextItem() {
		SimplePlayItem item = SimplePlayItem.build("f1");
		serverStream.addItem(item);
		SimplePlayItem item2 = SimplePlayItem.build("f2");
		serverStream.addItem(item2);
		serverStream.start();
		serverStream.nextItem();
		assertEquals(1, serverStream.getCurrentItemIndex());
		assertEquals("f2", serverStream.getCurrentItem().getName());
	}

}
