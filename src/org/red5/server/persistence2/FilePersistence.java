package org.red5.server.persistence2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.object.Deserializer;
import org.red5.server.zcontext.AppContext;
import org.springframework.core.io.Resource;


/**
 * Simple file-based persistence for objects.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */

public class FilePersistence extends RamPersistence implements IPersistentStorage {

	private Log log = LogFactory.getLog(FilePersistence.class.getName());
	private String path = "persistence";
	private String extension = ".red5";

	public void setPath(String path) {
		this.path = path;
	}
	
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	/*
	 * Load existing objects.
	 */
	private void loadObjects() {
		Resource[] objects;
		try {
			objects = this.appCtx.getResources(path + "/*" + extension);
		} catch (IllegalArgumentException e) {
			// This is raised if the directory exists.
			return;
		} catch (IOException e) {
			// No existing objects...
			return;
		}

		for(int i=0; i < objects.length; i++){
			Resource ob = objects[i];
			FileInputStream input;
			String filename = this.appCtx.getBaseDir() + "/" + path + "/" + ob.getFilename();
			try {
				input = new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				log.error("The file at " + filename + " does not exist.");
				continue;
			}
			
			try {
				if (input.available() == 0) {
					input.close();
					log.error("The file at " + filename + " is empty.");
					continue;
				}
				ByteBuffer buf = ByteBuffer.allocate(input.available());
				byte[] data = new byte[input.available()];
				input.read(data);
				buf.put(data);
				buf.flip();
				Input in = new Input(buf);
				Deserializer deserializer = new Deserializer();
				String className = (String) deserializer.deserialize(in);
				IPersistable object;
				try {
					Class theClass  = Class.forName(className);
					object = (IPersistable) theClass.newInstance();
				} catch (ClassNotFoundException cnfe) {
					log.error("Unknown class " + className);
					continue;
				} catch (IllegalAccessException iae) {
					log.error("Illegal access.", iae);
					continue;
				} catch (InstantiationException ie) {
					log.error("Could not instantiate class " + className);
					continue;
				}
				object.deserialize(buf);
				object.setStorage(this);
				super.storeObject(object);
				log.info("Loaded persistent object " + object.getPersistentId() + " from " + filename);
			} catch (IOException e) {
				log.error("Could not load file at " + filename);
				continue;
			}
		}
	}
	
	private void saveObject(IPersistable object) throws IOException {
		File file = new File(this.appCtx.getBaseDir() + "/" + path);
		if (!file.isDirectory() && !file.mkdir()) {
			log.error("Could not create directory " + file.getAbsolutePath());
			return;
		}
		
		String filename = path + "/" + object.getPersistentId() + extension;
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString(object.getClass().getName());
		object.serialize(buf);
		buf.flip();
		
		try {
			FileOutputStream output = new FileOutputStream(this.appCtx.getBaseDir() + "/" + filename);
			byte[] data = new byte[buf.limit()];
			buf.get(data);
			output.write(data);
			output.close();
			log.debug("Stored persistent object " + object.getPersistentId() + " at " + filename);
		} catch (IOException e) {
			log.error("Could not create / write file " + filename);
		}
	}
	
	public void setApplicationContext(AppContext context) {
		super.setApplicationContext(context);
		this.loadObjects();
	}
	
	public void storeObject(IPersistable object) throws IOException {
		super.storeObject(object);
		this.saveObject(object);
	}

	public void removeObject(String name) throws IOException {
		super.removeObject(name);
		// TODO: delete file containing shared object
		log.error("Deleting of shared objects is not implemented yet.");
	}

}
