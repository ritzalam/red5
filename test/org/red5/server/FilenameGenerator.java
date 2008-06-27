package org.red5.server;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides custom playback and recording directories.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FilenameGenerator implements IStreamFilenameGenerator {

	private static Logger logger = LoggerFactory.getLogger(FilenameGenerator.class);	
	
    // Path that will store recorded videos
    public static String recordPath = "/";
    // Path that contains VOD streams
    public static String playbackPath = "/";
    // Create a random generator
	public static Random rnd = new Random();
    
	public String generateFilename(IScope scope, String name, GenerationType type) {
		return generateFilename(scope, name, null, type);
	}

	public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
    	logger.debug("Get stream directory: scope={}, name={}, type={}", new Object[]{scope, name, type.toString()});
    	//determine current dir
		File tmp = new File("");
		System.out.println("Location: " + tmp.getAbsolutePath());
    	
		StringBuilder filename = new StringBuilder(tmp.getAbsolutePath());
		if (type.equals(GenerationType.PLAYBACK)) {
			logger.debug("Playback path used");
			filename.append(playbackPath);
		} else {
			logger.debug("Record path used");
			filename.append(recordPath);
		}
		if (filename.charAt(filename.length() - 1) != '/') {
			filename.append('/');
		}
		filename.append(name);
        if (extension != null){
            // Add extension
            filename.append(extension);
        }
        logger.debug("Generated filename: {}", filename.toString());
        System.out.println("File name: " + filename.toString());
        return filename.toString();
	}
	
    public boolean resolvesToAbsolutePath() {
    	return true;
    }
    
    public void setPlaybackPath(String path) {
    	logger.debug("Set playback path: {}", path); 
    	playbackPath = path;
    }

    public void setRecordPath(String path) {
    	logger.debug("Set record path: {}", path); 
        recordPath = path;
    }

	public static String generateCustomName() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(System.currentTimeMillis() / 1000);
    	sb.append('_');
    	int i = rnd.nextInt(99999);
    	if (i < 10) {
    		sb.append("0000");
    	} else if (i < 100) {
       		sb.append("000");
    	} else if (i < 1000) {
       		sb.append("00");
    	} else if (i < 10000) {
       		sb.append("0");
    	}    	
    	sb.append(i);
    	return sb.toString();
    }
    
}
