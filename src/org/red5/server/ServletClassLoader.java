package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Class used to get the Servlet Class loader. The class loader returned is a
 * child first class loader. 
 * 
 * <br />
 * <i>This class is based on original code from the XINS project, by 
 * Anthony Goubard (anthony.goubard@japplis.com)</i>
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class ServletClassLoader {

	private static Logger log = Red5LoggerFactory.getLogger(ServletClassLoader.class);
	
	/**
	 * Use the current class loader to load the servlet and the libraries.
	 */
	public static final int USE_CURRENT_CLASSPATH = 1;

	/**
	 * Load the Servlet code from the WAR file and use the current classpath for
	 * the libraries.
	 */
	public static final int USE_CLASSPATH_LIB = 2;

	/**
	 * Load the servlet code from the WAR file and try to find the libraries in
	 * the common red5 lib directory.
	 */
	public static final int USE_RED5_LIB = 3;

	/**
	 * Load the servlet code and the libraries from the WAR file. This may take
	 * some time as the libraries need to be extracted from the WAR file.
	 */
	public static final int USE_WAR_LIB = 4;

	/**
	 * Load the servlet code and the standard libraries from the CLASSPATH. Load
	 * the included external libraries from the WAR file.
	 */
	public static final int USE_WAR_EXTERNAL_LIB = 5;

	/**
	 * Gest the class loader that will loader the servlet.
	 * 
	 * @param warFile
	 *            the WAR file containing the Servlet.
	 * 
	 * @param mode
	 *            the mode in which the servlet should be loaded. The possible
	 *            values are <code>USE_CURRENT_CLASSPATH</code>,
	 *            <code>USE_CLASSPATH_LIB</code>, <code>USE_XINS_LIB</code>,
	 *            <code>USE_WAR_LIB</code>, <code>USE_WAR_EXTERNAL_LIB</code>.
	 * 
	 * @return the Class loader to use to load the Servlet.
	 * 
	 * @throws IOException
	 *             if the file cannot be read or is incorrect.
	 */
	public static ClassLoader getServletClassLoader(File warFile, int mode)
			throws IOException {
		
		if (mode == USE_CURRENT_CLASSPATH) {
			return ServletClassLoader.class.getClassLoader();
		}

		List<URL> urlList = new ArrayList<URL>(13);

		if (warFile != null) {
    		// Add the WAR file so that it can locate web pages included in the WAR
    		// file
    		urlList.add(warFile.toURI().toURL());
    
    		if (mode != USE_WAR_EXTERNAL_LIB) {
    			URL classesURL = new URL("jar:file:"
    					+ warFile.getAbsolutePath().replace(File.separatorChar, '/')
    					+ "!/WEB-INF/classes/");
    			urlList.add(classesURL);
    		}
		}

		List<String> standardLibs = new ArrayList<String>(7);
		if (mode == USE_RED5_LIB) {
			//get red5 lib system property, if not found build it	
			String libPath = System.getProperty("red5.lib_root");
			if (libPath == null) {
    			// look for red5 home as a system property
    			String home = System.getProperty("red5.root");
    			// if home is null check environmental
    			if (home == null) {
    				//check for env variable
    				home = System.getenv("RED5_HOME");
    			}
    			//if home is null or equal to "current" directory
    			if (home == null || ".".equals(home)) {
    				//if home is still null look it up via this classes loader
    				String classLocation = ServletClassLoader.class
    					.getProtectionDomain().getCodeSource().getLocation()
    					.toString();
    				System.out.printf("Classloader location: %s\n", classLocation);
    				//snip off anything beyond the last slash
    				home = classLocation.substring(0, classLocation.lastIndexOf('/'));
    			}
    			//construct the lib path
    			libPath = home + File.separatorChar + "lib";
			}
			System.out.printf("Library path: %s\n", libPath);	
			
			//grab the urls for all the jars in "lib"
			File libDir = new File(libPath);
			File[] libFiles = libDir.listFiles();
			for (int i = 0; i < libFiles.length; i++) {
				if (libFiles[i].getName().endsWith(".jar")) {
					urlList.add(libFiles[i].toURI().toURL());
				}
			}
			
		}
		if (mode == USE_CLASSPATH_LIB || mode == USE_WAR_EXTERNAL_LIB) {
			String classPath = System.getProperty("java.class.path");
			StringTokenizer stClassPath = new StringTokenizer(classPath,
					File.pathSeparator);
			while (stClassPath.hasMoreTokens()) {
				String nextPath = stClassPath.nextToken();
				if (nextPath.toLowerCase().endsWith(".jar")) {
					standardLibs.add(nextPath.substring(nextPath
							.lastIndexOf(File.separatorChar) + 1));
				}
				urlList.add(new File(nextPath).toURI().toURL());
			}
		}
		if (mode == USE_WAR_LIB || mode == USE_WAR_EXTERNAL_LIB) {
			if (warFile.isDirectory()) {
				File libDir = new File(warFile, "WEB-INF/lib");
				//this should not be null but it can happen
				if (libDir != null && libDir.canRead()) {
    				File[] libs = libDir.listFiles();
    				log.debug("Webapp lib count: {}", libs.length);
    				for (File lib : libs) {
    					urlList.add(lib.toURI().toURL());
    				}
				}
			} else {
    			JarInputStream jarStream = new JarInputStream(new FileInputStream(warFile));
    			JarEntry entry = jarStream.getNextJarEntry();
    			while (entry != null) {
    				String entryName = entry.getName();
    				if (entryName.startsWith("WEB-INF/lib/")
    						&& entryName.endsWith(".jar")
    						&& !standardLibs.contains(entryName.substring(12))) {
    					File tempJarFile = unpack(jarStream, entryName);
    					urlList.add(tempJarFile.toURI().toURL());
    				}
    				entry = jarStream.getNextJarEntry();
    			}
    			jarStream.close();
			}
		}
		URL[] urls = new URL[urlList.size()];
		for (int i = 0; i < urlList.size(); i++) {
			urls[i] = (URL) urlList.get(i);
		}

		ClassLoader loader = new ChildFirstClassLoader(urls, ServletClassLoader.class.getClassLoader());
		//Thread.currentThread().setContextClassLoader(loader);
		
		return loader;
	}

	/**
	 * Unpack the specified entry from the JAR file.
	 * 
	 * @param jarStream
	 *            The input stream of the JAR file positioned at the entry.
	 * @param entryName
	 *            The name of the entry to extract.
	 * 
	 * @return The extracted file. The created file is a temporary file in the
	 *         temporary directory.
	 * 
	 * @throws IOException
	 *             if the JAR file cannot be read or is incorrect.
	 */
	private static File unpack(JarInputStream jarStream, String entryName)
			throws IOException {
		String libName = entryName.substring(entryName.lastIndexOf('/') + 1,
				entryName.length() - 4);
		File tempJarFile = File.createTempFile("tmp_" + libName, ".jar");
		tempJarFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempJarFile);

		// Transfer bytes from the JAR file to the output file
		byte[] buf = new byte[8192];
		int len;
		while ((len = jarStream.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		return tempJarFile;
	}

}
