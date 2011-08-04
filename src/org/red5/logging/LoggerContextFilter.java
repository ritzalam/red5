package org.red5.logging;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

/**
 * A servlet filter that puts this contexts LoggerContext into a Threadlocal variable.
 * 
 * It removes it after the request is processed.
 *
 * To use it, add the following lines to a web.xml file
 *<pre>
	&lt;filter&gt;
		&lt;filter-name&gt;LoggerContextFilter&lt;/filter-name&gt;
		&lt;filter-class&gt;org.red5.logging.LoggerContextFilter&lt;/filter-class&gt;
	&lt;/filter&gt;
	&lt;filter-mapping&gt;
		&lt;filter-name&gt;LoggerContextFilter&lt;/filter-name&gt;
		&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	&lt;/filter-mapping&gt;
 *</pre>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class LoggerContextFilter implements Filter {

	private String contextName;

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		//System.out.printf("Context name: %s\n", contextName);
		LoggingContextSelector selector = (LoggingContextSelector) Red5LoggerFactory.getContextSelector();
		//System.out.printf("Context select type: %s\n", selector.getClass().getName());
		LoggerContext ctx = selector.getLoggerContext(contextName);
		//load default logger context if its null
		if (ctx == null) {
			//System.out.println("Logger context was null, getting default");
			ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
		}
		//evaluate context name against logger context name
		if (!contextName.equals(ctx.getName())) {
			System.err.printf("Logger context name and context name dont match (%s != %s)\n", contextName, ctx.getName());
		}
		//System.out.printf("Logger context name: %s\n", ctx.getName());
		selector.setLocalContext(ctx);
		try {
			chain.doFilter(request, response);
		} finally {
			selector.removeLocalContext();
		}
	}

	public void init(FilterConfig config) throws ServletException {
		contextName = config.getServletContext().getContextPath().replaceAll("/", "");
	}
}
