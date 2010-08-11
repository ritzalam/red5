package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors. All rights reserved.
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

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Chris Allen (mrchrisallen@gmail.com)
 */
public class EchoServiceTest extends TestCase {

	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private EchoService echoService;

	/** {@inheritDoc} */
    @Override
	protected void setUp() throws Exception {
		super.setUp();
		echoService = new EchoService();
	}

	/** {@inheritDoc} */
    @Override
	protected void tearDown() throws Exception {
		super.tearDown();
		echoService = null;
	}

	public void testEchoArray() {
		Object[] startArray = { "first", "second", "third" };
		Object[] resultArray = echoService.echoArray(startArray);
		assertEquals(startArray[0], resultArray[0]);
		assertEquals(startArray[1], resultArray[1]);
		assertEquals(startArray[2], resultArray[2]);
	}

	public void testEchoBoolean() {
		boolean b = true;
		assertTrue(echoService.echoBoolean(b));
	}

	public void testEchoDate() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date startDate = dateFormat.parse("01-26-1974");
		Date returnDate = echoService.echoDate(startDate);
		assertEquals(startDate.getTime(), returnDate.getTime());
	}

	@SuppressWarnings("unchecked")
	public void testEchoList() {
		List<String> startList = new ArrayList<String>();
		startList.add(0, "first");
		startList.add(1, "second");
		List<String> resultList = echoService.echoList(startList);
		assertEquals(startList.get(0), resultList.get(0));
		assertEquals(startList.get(1), resultList.get(1));
	}

	public void testEchoNumber() {
		double num = 100;
		assertEquals(200, echoService.echoNumber(num), echoService
				.echoNumber(num));
	}

	@SuppressWarnings("unchecked")
	public void testEchoObject() {
		String str = "entry one";
		Date date = new Date();
		Map<String, Comparable<?>> startMap = new HashMap<String, Comparable<?>>();
		startMap.put("string", str);
		startMap.put("date", date);
		Map<String, Comparable<?>> resultMap = (Map<String, Comparable<?>>) echoService.echoObject(startMap);
		assertEquals(startMap.get("string"), resultMap.get("string"));
		assertEquals(startMap.get("date"), resultMap.get("date"));
	}

	public void testEchoString() {
		String str = "This is a test.";
		assertEquals("This is a test.", echoService.echoString(str));
	}

	public void testEchoXML() throws SAXException, IOException,
			ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String xmlStr = "<root testAttribute=\"test value\">this is a test</root>";
		StringReader reader = new StringReader(xmlStr);
		InputSource source = new InputSource(reader);
		Document xml = builder.parse(source);
		Document resultXML = echoService.echoXML(xml);
		assertEquals(xml.getFirstChild().getNodeValue(), resultXML
				.getFirstChild().getNodeValue());
	}
	
	public void testEchoMultibyteStrings()
	{
		java.nio.ByteBuffer buf = ByteBuffer.allocate(7);
		buf.put((byte)0xE4);
		buf.put((byte)0xF6);
		buf.put((byte)0xFC);
		buf.put((byte)0xC4);
		buf.put((byte)0xD6);
		buf.put((byte)0xDC);
		buf.put((byte)0xDF);
		buf.flip();
		
		final Charset cs = Charset.forName("iso-8859-1");
		assertNotNull(cs);
		final String inputStr = cs.decode(buf).toString();
		log.debug("passing input string: {}", inputStr);
		final String outputStr = echoService.echoString(inputStr);
		assertEquals("unequal strings", inputStr, outputStr);
		log.debug("got output string: {}", outputStr);
		
		java.nio.ByteBuffer outputBuf = cs.encode(outputStr);
		
		for(int i = 0; i < 7; i++)
			assertEquals("unexpected byte",
					buf.get(i),
					outputBuf.get(i));
	}
}
