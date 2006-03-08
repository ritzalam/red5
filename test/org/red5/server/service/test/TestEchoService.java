package org.red5.server.service.test;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Chris Allen (mrchrisallen@gmail.com)
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.red5.server.service.EchoService;
import org.red5.server.service.IEchoService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class TestEchoService extends TestCase {

	private IEchoService echoService;
	protected void setUp() throws Exception {
		super.setUp();
		echoService = new EchoService();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		echoService = null;
	}
	
	public void testEchoBoolean() {
		boolean b = true;
		assertTrue(echoService.echoBoolean(b));
	}
	
	public void testEchoNumber() {
		double num = 100;
		assertEquals(200, echoService.echoNumber(num), echoService.echoNumber(num));
	}
	
	public void testEchoString() {
		String str = "This is a test.";
		assertEquals("This is a test.", echoService.echoString(str));
	}
	
	public void testEchoDate() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date startDate = dateFormat.parse("01-26-1974");
		Date returnDate = echoService.echoDate(startDate);
		assertEquals(startDate.getTime(), returnDate.getTime());
	}
	
	public void testEchoObject() {
		String str = "entry one";
		Date date = new Date();
		Map startMap = new HashMap();
		startMap.put("string", str);
		startMap.put("date", date);
		Map resultMap = echoService.echoObject(startMap);
		assertEquals(startMap.get("string"), resultMap.get("string"));
		assertEquals(startMap.get("date"), resultMap.get("date"));	
	}
	
	public void testEchoArray() {
		Object[] startArray = {"first", "second", "third"};
		Object[] resultArray = echoService.echoArray(startArray);
		assertEquals(startArray[0], resultArray[0]);
		assertEquals(startArray[1], resultArray[1]);
		assertEquals(startArray[2], resultArray[2]);
	}
	
	public void testEchoList() {
		List startList = new ArrayList();
		startList.add(0, "first");
		startList.add(1, "second");
		List resultList = echoService.echoList(startList);
		assertEquals(startList.get(0), resultList.get(0));
		assertEquals(startList.get(1), resultList.get(1));
	}	
	
	public void testEchoXML() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String xmlStr = "<root testAttribute=\"test value\">this is a test</root>";
		StringReader reader = new StringReader(xmlStr);
		InputSource source = new InputSource(reader);
		Document xml = builder.parse(source);	
		Document resultXML = echoService.echoXML(xml);
		assertEquals(xml.getFirstChild().getNodeValue(), resultXML.getFirstChild().getNodeValue());
	}
}
