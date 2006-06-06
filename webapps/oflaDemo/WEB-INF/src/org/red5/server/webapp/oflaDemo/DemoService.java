package org.red5.server.webapp.oflaDemo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.springframework.core.io.Resource;

public class DemoService {

	protected static Log log = LogFactory.getLog(DemoService.class.getName());

	public Map getListOfAvailableFLVs() {
		IScope scope = Red5.getConnectionLocal().getScope();
		Map<String, Map> filesMap = new HashMap<String, Map>();
		Map<String, Object> fileInfo;
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
					
					fileInfo = new HashMap<String, Object>();
					fileInfo.put("name", flvName);
					fileInfo.put("lastModified", lastModified);
					fileInfo.put("size", flvBytes);
					filesMap.put(flvName, fileInfo);
				}
			}
			
			Resource[] mp3s = scope.getResources("streams/*.mp3");
			if(mp3s!=null){
				for(int i=0; i<mp3s.length; i++){
					Resource mp3 = mp3s[i];
					File file = mp3.getFile();
					Date lastModifiedDate = new Date(file.lastModified());
					String lastModified = formatDate(lastModifiedDate);
					String flvName = mp3.getFile().getName();
					String flvBytes = new Long(file.length()).toString();
		
					log.debug("flvName: " + flvName);
					log.debug("lastModified date: " + lastModified);
					log.debug("flvBytes: " + flvBytes);
					log.debug("-------");
					
					fileInfo = new HashMap<String, Object>();
					fileInfo.put("name", flvName);
					fileInfo.put("lastModified", lastModified);
					fileInfo.put("size", flvBytes);
					filesMap.put(flvName, fileInfo);
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
