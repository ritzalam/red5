package org.red5.server.webapp.oflaDemo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.red5.server.adapter.ApplicationAdapter;
import org.springframework.core.io.Resource;

public class Application extends ApplicationAdapter {
	
	public Map getListOfAvailableFLVs() {
		Map filesMap = new HashMap();
		Map fileInfo;
		try {
			log.debug("getting the FLV files");
			//Resource[] flvs = appCtx.getResources("../../../../flvs/*.flv");
			Resource[] flvs = scope.getResources("streams/*.flv");
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
