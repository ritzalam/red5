package org.slf4j.impl;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 * Modified for use in Red5.
 * 
 * @author <a href="http://www.qos.ch/shop/products/log4jManual">Ceki
 *         G&uuml;lc&uuml;</a>
 * @author Paul Gregoire
 */
public class StaticLoggerBinder implements LoggerFactoryBinder, LoggerContextSelectorProvider {

	/**
	 * Declare the version of the SLF4J API this implementation is compiled
	 * against. The value of this field is usually modified with each release.
	 */
	// to avoid constant folding by the compiler, this field must *not* be final
	public static String REQUESTED_API_VERSION = "1.6"; // !final

	final static String NULL_CS_URL = CoreConstants.CODES_URL + "#null_CS";

	/**
	 * The unique instance of this class.
	 */
	private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	private static Object KEY = new Object();

	static {
		SINGLETON.init();
	}

	private boolean initialized = false;

	private LoggerContext defaultLoggerContext = new LoggerContext();

	private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();

	private StaticLoggerBinder() {
		defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
	}

	public static StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	/**
	 * Package access for testing purposes.
	 */
	static void reset() {
		SINGLETON = new StaticLoggerBinder();
		SINGLETON.init();
	}

	/**
	 * Package access for testing purposes.
	 */
	void init() {
		try {
			try {
				new ContextInitializer(defaultLoggerContext).autoConfig();
			} catch (JoranException je) {
				Util.report("Failed to auto configure default logger context", je);
			}
			StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
			contextSelectorBinder.init(defaultLoggerContext, KEY);
			initialized = true;
		} catch (Throwable t) {
			// we should never get here
			Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
		}
	}

	public ILoggerFactory getLoggerFactory() {
		if (!initialized) {
			return defaultLoggerContext;
		}

		if (contextSelectorBinder.getContextSelector() == null) {
			throw new IllegalStateException("contextSelector cannot be null. See also " + NULL_CS_URL);
		}
		return contextSelectorBinder.getContextSelector().getLoggerContext();
	}

	public String getLoggerFactoryClassStr() {
		return contextSelectorBinder.getContextSelector().getClass().getName();
	}
	
	public ContextSelector getContextSelector() {
		return contextSelectorBinder.getContextSelector();
	}

}
