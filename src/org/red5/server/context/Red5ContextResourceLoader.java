package org.red5.server.context;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;

public class Red5ContextResourceLoader extends FileSystemResourceLoader { 
	
	private String baseDir = null;
	
	public Red5ContextResourceLoader(String baseDir, ClassLoader classLoader){
		super();
		this.setClassLoader(classLoader);
		this.baseDir = baseDir;
	}

	protected Resource getResourceByPath(String path) {
		return super.getResourceByPath(baseDir + path);
	}
	
	
	
}
