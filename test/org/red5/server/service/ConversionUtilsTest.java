package org.red5.server.service;

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
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.util.ArrayList;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionUtilsTest extends TestCase {

	class TestJavaBean {

	}

	private static final Logger log = LoggerFactory.getLogger(ConversionUtilsTest.class);

	public void testBasic() {
		Object result = ConversionUtils.convert(new Integer(42), String.class);
		if (!(result instanceof String)) {
			Assert.fail("Should be a string");
		}
		String str = (String) result;
		Assert.assertEquals("42", str);
	}

	public void testConvertListToStringArray() {
		ArrayList source = new ArrayList();

		source.add("Testing 1");
		source.add("Testing 2");
		source.add("Testing 3");

		Class target = (new String[0]).getClass();

		Object result = ConversionUtils.convert(source, target);
		if (!(result.getClass().isArray() && result.getClass()
				.getComponentType().equals(String.class))) {
			Assert.fail("Should be String[]");
		}
		String[] results = (String[]) result;

		Assert.assertEquals(results.length, source.size());
		Assert.assertEquals(results[2], source.get(2));

	}

	public void testConvertObjectArrayToStringArray() {
		Object[] source = new Object[3];

		source[0] = new Integer(21);
		source[1] = Boolean.FALSE;
		source[2] = "Woot";

		Class target = (new String[0]).getClass();

		Object result = ConversionUtils.convert(source, target);
		if (!(result.getClass().isArray() && result.getClass()
				.getComponentType().equals(String.class))) {
			Assert.fail("Should be String[]");
		}
		String[] results = (String[]) result;

		Assert.assertEquals(results.length, source.length);
		Assert.assertEquals(results[2], source[2]);

	}

	public void testConvertToSet() {
		Object[] source = new Object[3];
		source[0] = new Integer(21);
		source[1] = Boolean.FALSE;
		source[2] = "Woot";
		Object result = ConversionUtils.convert(source, Set.class);
		if (!(result instanceof Set)) {
			Assert.fail("Should be a set");
		}
		Set results = (Set) result;
		Assert.assertEquals(results.size(), source.length);

	}

	public void testNoOppConvert() {
		TestJavaBean source = new TestJavaBean();
		Object result = ConversionUtils.convert(source, TestJavaBean.class);
		Assert.assertEquals(result, source);
	}

	public void testNullConvert() {
		Object result = ConversionUtils.convert(null, TestJavaBean.class);
		Assert.assertNull(result);
		result = ConversionUtils.convert(new TestJavaBean(), null);
		Assert.assertNull(result);
	}

}
