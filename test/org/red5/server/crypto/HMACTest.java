package org.red5.server.crypto;

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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.Provider;
import java.security.Security;

import org.junit.Test;

public class HMACTest {

	@Test
	public void testHMAC() {
		HMAC h1 = new HMAC();
		assertNotNull(h1);
		
		try {
			Provider sp = new com.sun.crypto.provider.SunJCE();
			Security.addProvider(sp);
		} catch (Exception e) {
			HMAC.message("Problem loading crypto provider", e);
			fail("Problem loading crypto provider" + e);
		}

		//String[] args = new String[]{};
		//h1.processCommandLine(args);
		byte[] hmac = h1.computeMac();
		assertNull("Currently HMAC is broken since you can't actually " +
				"set the keyData or data elements.  This test will break once someone fixes that",
				hmac);
		//HMAC.message("Result: " + HMAC.byteArrayToHex(hmac));
	}
	
/*
  String [] usageLines = {
    "java hmac [options]",
    "",
    "where options are the following:",
    "\t-k keydata        key data as a hex string",
    "\t-kc               get key data as hex string from the clipboard",
    "\t-kf file          get key data from specified file",
    "\t-d data           data to hash as a hex string",
    "\t-dc               get data to hash as hex string from the clipboard",
    "\t-df file          get data to hash from specified file",
    "\t-a alg       use specified HMAC algorithm, {HMacMD5, HMacSHA1}",
    "\t                    (default is HMacMD5)",                 
    "\t-r       interpret hex strings in reverse",
    "\t                    (as strings rather than numbers)",
    "\t-B                treat ALL files as binary data",
    "\t-v                print verbose messages",
    "",
    "If a file name is given as '-' then read standard input.  If no -d",
    "options are given, then we read standard input.",
    "",
    "Options are interpreted in the order given, including -r and -B;",
    "use them with care.",
    "",
    "For files and standard input, the program will inspect",
    "the data and decide whether it seems to",
    "be binary data or hex data, unless -B is given.",
    "",
    "Here are some example command lines:",
    "     java hmac -kc -df capture1",
    "     java hmac -k 1f031b78a0993d42 -df - -a HMacSHA1 <myfile.dat",
    "",
    "For more information, contact Neal Ziring.",
  };
   
  protected void usage() {
    for(int i = 0; i < usageLines.length; i++) {
      System.out.println(usageLines[i]);
    }
  } 	
 	
  / **
   * Process the command line options for this hmac
   * object.  The command-line syntax is do*****ented in
   * the usage array; run the program with no command-line
   * arguments, and it will print the usage message.
   * /
  protected void processCommandLine(String [] args) {
    int i;
    String data = null;

    if (args.length == 0) {
      usage();
      System.exit(0);
    }

    try {
      for(i = 0; i < args.length; i++) {
   if (args[i].equalsIgnoreCase("-k")) {
     data = args[++i];
     if (isHex(data.getBytes(),4)) {
       if (verbose) message("Treating key bytes as hex.");
       keyBytes = hexToByteArray(data, reverse);
     }
     else {
       if (verbose) message("Treating key bytes as ASCII.");
       keyBytes = data.getBytes();
     }
   }
   else if (args[i].equalsIgnoreCase("-kc")) {
     keyBytes = readDataClipboard();
     if (keyBytes == null) {
       message("Clipboard is empty.  No key data.");
       System.exit(0);
     }
   }
   else if (args[i].equalsIgnoreCase("-kf")) {
     keyFile = new File(args[++i]);
     keyBytes = readDataFile(keyFile, MIN_LENGTH);
     if (keyBytes == null) {
       message("No key bytes available.");
       System.exit(0);
     }
   }
   else if (args[i].equalsIgnoreCase("-d")) {
     data = args[++i];
     if (isHex(data.getBytes(),4)) {
       if (verbose) message("Treating data bytes as hex.");
       dataBytes = hexToByteArray(data, reverse);
     }
     else {
       if (verbose) message("Treating data bytes as ASCII.");
       dataBytes = data.getBytes();
     }
   }
   else if (args[i].equalsIgnoreCase("-dc")) {
     dataBytes = readDataClipboard();
     if (dataBytes == null) {
       message("Clipboard is empty.  No data available to hash.");
       System.exit(0);
     }
   }
   else if (args[i].equalsIgnoreCase("-df")) {
     dataFile = new File(args[++i]);
     dataBytes = readDataFile(dataFile, MIN_LENGTH);
     if (dataBytes == null) {
       message("No data bytes available from file");
       System.exit(0);
     }
   }
   else if (args[i].equalsIgnoreCase("-a")) {
     alg = args[++i];
     if (verbose) message("Set algorithm to " + alg);
   }
   else if (args[i].equalsIgnoreCase("-B")) {
     noHex = true;
   }
   else if (args[i].equalsIgnoreCase("-v")) {
     verbose = true;
   }
   else if (args[i].equalsIgnoreCase("-r")) {
     reverse = true;
     if (verbose) message("Hex strings will be interpreted in reverse");
   }
   else {
     message("Bad option: " + args[i]);
     usage();
     System.exit(0);
   }
      }
    }
    catch (Exception e) {
      message("Problem processing options", e);
      usage();
      System.exit(0);
    }
  } 	

 */

}
