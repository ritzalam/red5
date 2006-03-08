package org.red5.server.example;
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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * This service is to be used for John Grden's deomnstration of the Red5
 * prototype presented on Friday October 21st, 2005 at the OFLA Online: The
 * First Online Open Source Flash Conference.
 * 
 * @author Chris Allen mrchrisallen@gmail.com
 * 
 */


public class DemoService implements IDemoService, ApplicationContextAware {

	protected static Log log = LogFactory.getLog(DemoService.class.getName());
	
	protected ApplicationContext appCtx = null;
	
	public DemoService(){
		
	}

	public void startUp() {
		log.debug("starting the deom service...");
	}

	public void setApplicationContext(ApplicationContext context) throws BeansException {
		appCtx = context;
	}
	
	/**
	 * @see org.red5.server.example.IDemoService#getListOfAvailableFLVs()
	 */
	public Map getListOfAvailableFLVs() {
		Map filesMap = new HashMap();
		Map fileInfo;
		try {
			log.debug("getting the FLV files");
			//Resource[] flvs = appCtx.getResources("../../../../flvs/*.flv");
			Resource[] flvs = appCtx.getResources("streams/*.flv");
			if(flvs!=null){
				for(int i=0; i<flvs.length; i++){
					Resource flv = flvs[i];
					File file = flv.getFile();
					Date lastModifiedDate = new Date(file.lastModified());
					String lastModified = formatDate(lastModifiedDate);
					String flvName = flv.getFile().getName();
					String flvBytes = new Long(file.length()).toString();
		
					log.debug("flvName: " + flvName);
					log.debug("lastModified date: " + lastModified);
					log.debug("flvBytes: " + flvBytes);
					log.debug("-------");
					
					fileInfo = new HashMap();
					fileInfo.put("name", flvName);
					fileInfo.put("lastModified", lastModified);
					fileInfo.put("size", flvBytes);
					filesMap.put(flvName, fileInfo);
					fileInfo = null;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filesMap;
	}	
	
	private String formatDate(Date date) {
		SimpleDateFormat formatter;
		String pattern = "dd/MM/yy H:mm:ss";
		Locale locale= new Locale("en","US");
		formatter = new SimpleDateFormat(pattern, locale);
		return formatter.format(date);
	}
	
}
