package org.red5.logging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerbyLogInterceptor {

	protected static Logger log = LoggerFactory.getLogger(DerbyLogInterceptor.class);
	
	public static java.io.OutputStream handleDerbyLogFile(){
	    return new java.io.OutputStream() {
	        @Override
			public void write(byte[] b) throws IOException {
	        	log.info("Derby log: {}", b);
			}

			public void write(int b) throws IOException {
	            log.info("Derby log: {}", b);
	        }
	    };
	}
	
}
