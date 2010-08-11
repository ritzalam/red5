package org.red5.io.m4a.impl;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.m4a.impl.M4AReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M4AReaderTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(M4AReaderTest.class);

	@Test
	public void testCtor() throws Exception {
		
		File file = new File("fixtures/sample.m4a");
		M4AReader reader = new M4AReader(file);
		
		ITag tag = reader.readTag();
		log.debug("Tag: {}", tag);
		tag = reader.readTag();		
		log.debug("Tag: {}", tag);

	}
}
