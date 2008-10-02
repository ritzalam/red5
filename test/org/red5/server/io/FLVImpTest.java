package org.red5.server.io;

import org.junit.Test;
import org.red5.io.flv.impl.FLV;

import junit.framework.TestCase;

public class FLVImpTest extends TestCase {
	@Test
	public void testCreation()
	{
		FLV exampleObj = new FLV();
		assertNotNull(exampleObj);
	}

}
