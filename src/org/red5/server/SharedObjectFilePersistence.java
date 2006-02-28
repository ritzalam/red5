package org.red5.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.api.SharedObject;
import org.red5.server.context.AppContext;
import org.red5.server.context.PersistentSharedObject;
import org.springframework.core.io.Resource;


/**
 * Simple file-based persistence for shared objects.  Stores similar format
 * to shared objects of FCS. 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */

public class SharedObjectFilePersistence
	extends SharedObjectRamPersistence
	implements SharedObjectPersistence {

	private Log log = LogFactory.getLog(SharedObjectFilePersistence.class.getName());

	/*
	 * Load existing shared objects.
	 */
	private void loadSharedObjects() {
		Resource[] objects;
		try {
			objects = this.appCtx.getResources("sharedObjects/*.so");
		} catch (IllegalArgumentException e) {
			// This is raised if no directory "sharedObjects" exists.
			return;
		} catch (IOException e) {
			// No existing shared objects...
			return;
		}

		for(int i=0; i < objects.length; i++){
			Resource so = objects[i];
			FileInputStream input;
			String filename = this.appCtx.getBaseDir() + "/sharedObjects/" + so.getFilename();
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
				String name = (String) deserializer.deserialize(in);
				Map attributes = (Map) deserializer.deserialize(in);
				PersistentSharedObject sharedObject = new PersistentSharedObject(name, true, this);
				sharedObject.setData(attributes);
				super.storeSharedObject(sharedObject);
				log.info("Loaded shared object " + sharedObject.getName() + " from " + filename);
			} catch (IOException e) {
				log.error("Could not load file at " + filename);
				continue;
			}
		}
	}
	
	private void saveSharedObject(SharedObject object) {
		File file = new File(this.appCtx.getBaseDir() + "/sharedObjects");
		if (!file.isDirectory() && !file.mkdir()) {
			log.error("Could not create directory " + file.getAbsolutePath());
			return;
		}
		
		String filename = "sharedObjects/" + object.getName() + ".so";
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		Serializer serializer = new Serializer();
		// TODO: add header so we can check the filetype later
		out.writeString(object.getName());
		serializer.writeMap(out, object.getData());
		buf.flip();
		
		try {
			FileOutputStream output = new FileOutputStream(this.appCtx.getBaseDir() + "/" + filename);
			byte[] data = new byte[buf.limit()];
			buf.get(data);
			output.write(data);
			output.close();
			log.debug("Stored shared object " + object.getName() + " at " + filename);
		} catch (IOException e) {
			log.error("Could not create / write file " + filename);
		}
	}
	
	public void setApplicationContext(AppContext context) {
		super.setApplicationContext(context);
		this.loadSharedObjects();
	}
	
	public void storeSharedObject(SharedObject object) {
		super.storeSharedObject(object);
		this.saveSharedObject(object);
	}

	public void deleteSharedObject(String name) {
		super.deleteSharedObject(name);
		// TODO: delete file containing shared object
		log.error("Deleting of shared objects is not implemented yet.");
	}

}
