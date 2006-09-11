package org.red5.server.persistence;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.object.Deserializer;
import org.red5.server.api.IScope;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.net.servlet.ServletUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Simple file-based persistence for objects.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class FilePersistence extends RamPersistence implements IPersistenceStore {

	private Log log = LogFactory.getLog(FilePersistence.class.getName());
	private String path = "persistence";
	private String extension = ".red5";

	public FilePersistence(ResourcePatternResolver resolver) {
		super(resolver);
	}
	
	public FilePersistence(IScope scope) {
		super(scope);
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setExtension(String extension) {
		this.extension = extension;
	}

	private String getObjectFilepath(IPersistable object) {
		return getObjectFilepath(object, false);
	}
	
	private String getObjectFilepath(IPersistable object, boolean completePath) {
		String result = path + "/" + object.getType() + "/" + object.getPath();
		if (!result.endsWith("/"))
			result += "/";
		
		if (completePath) {
			String name = object.getName();
			int pos = name.lastIndexOf("/");
			if (pos >= 0)
				result += name.substring(0, pos);
		}
		return result;
	}

	protected String getObjectPath(String id, String name) {
		if (id.startsWith(path))
			id = id.substring(path.length()+1);
		
		return super.getObjectPath(id, name);
	}

	private String getObjectFilename(IPersistable object) {
		String path = getObjectFilepath(object);
		String name = object.getName();
		if (name == null)
			name = PERSISTENCE_NO_NAME;
		return path + name + extension;
	}
	
	private IPersistable doLoad(String name) {
		return doLoad(name, null);
	}
	
	private IPersistable doLoad(String name, IPersistable object) {
		IPersistable result = object;
		Resource data = resources.getResource(name);
		if (data == null || !data.exists())
			// No such file
			return null;
		
		FileInputStream input;
		String filename;
		try {
			File fp = data.getFile();
			if (fp.length() == 0) {
				// File is empty
				log.error("The file at " + data.getFilename() + " is empty.");
				return null;
			}
			
			filename = fp.getAbsolutePath();
			input = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			log.error("The file at " + data.getFilename() + " does not exist.");
			return null;
		} catch (IOException e) {
			log.error("Could not load file from " + data.getFilename() + ".", e);
			return null;
		}
		
		try {
			ByteBuffer buf = ByteBuffer.allocate(input.available());
			try {
				ServletUtils.copy(input, buf.asOutputStream());
				buf.flip();
				Input in = new Input(buf);
				Deserializer deserializer = new Deserializer();
				String className = (String) deserializer.deserialize(in);
				if (result == null) {
					// We need to create the object first
					try {
						Class theClass  = Class.forName(className);
						Constructor constructor = null;
						try {
							// Try to create object by calling constructor with Input stream as
							// parameter.
							for (Class interfaceClass : in.getClass().getInterfaces()) {
								constructor = theClass.getConstructor(new Class[]{interfaceClass});
								if (constructor != null)
									break;
							}
							if (constructor == null)
								throw new NoSuchMethodException();
							
							result = (IPersistable) constructor.newInstance(new Object[]{in});
						} catch (NoSuchMethodException err) {
							// No valid constructor found, use empty constructor.
							result = (IPersistable) theClass.newInstance(); 
							result.deserialize(in);
						} catch (InvocationTargetException err) {
							// Error while invoking found constructor, use empty constructor.
							result = (IPersistable) theClass.newInstance(); 
							result.deserialize(in);
						}
					} catch (ClassNotFoundException cnfe) {
						log.error("Unknown class " + className);
						return null;
					} catch (IllegalAccessException iae) {
						log.error("Illegal access.", iae);
						return null;
					} catch (InstantiationException ie) {
						log.error("Could not instantiate class " + className);
						return null;
					}
					
					// Set object's properties
					result.setName(getObjectName(name));
					result.setPath(getObjectPath(name, result.getName()));
				} else {
					// Initialize existing object
					String resultClass = result.getClass().getName(); 
					if (!resultClass.equals(className)) {
						log.error("The classes differ: " + resultClass + " != " + className);
						return null;
					}
					
					result.deserialize(in);
				}
			} finally {
				buf.release();
			}
			if (result.getStore() != this)
				result.setStore(this);
			super.save(result);
			log.debug("Loaded persistent object " + result + " from " + filename);
		} catch (IOException e) {
			log.error("Could not load file at " + filename);
			return null;
		}
		
		return result;
	}
	
	public IPersistable load(String name) {
		IPersistable result = super.load(name);
		if (result != null)
			// Object has already been loaded
			return result;
		
		return doLoad(path + "/" + name + extension);
	}
	
	public boolean load(IPersistable object) {
		if (object.isPersistent())
			// Already loaded
			return true;
		
		return (doLoad(getObjectFilename(object), object) != null);
	}
	
	private boolean saveObject(IPersistable object) {
		String path = getObjectFilepath(object, true);
		Resource resPath = resources.getResource(path);
		File file;
		try {
			file = resPath.getFile();
		} catch (IOException err) {
			log.error("Could not create resource file for path " + path, err);
			return false;
		}
		
		if (!file.isDirectory() && !file.mkdirs()) {
			log.error("Could not create directory " + file.getAbsolutePath());
			return false;
		}
		
		String filename = getObjectFilename(object);
		Resource resFile = resources.getResource(filename);
		try {
			ByteBuffer buf = ByteBuffer.allocate(1024);
			try {
				buf.setAutoExpand(true);
				Output out = new Output(buf);
				out.writeString(object.getClass().getName());
				object.serialize(out);
				buf.flip();
				
				FileOutputStream output = new FileOutputStream(resFile.getFile().getAbsolutePath());
				ServletUtils.copy(buf.asInputStream(), output);
				output.close();
			} finally {
				buf.release();
			}
			log.debug("Stored persistent object " + object + " at " + filename);
			return true;
		} catch (IOException e) {
			log.error("Could not create / write file " + filename);
			return false;
		}
	}
	
	public boolean save(IPersistable object) {
		if (!super.save(object))
			return false;
		
		boolean persistent = this.saveObject(object);
		object.setPersistent(persistent);
		return persistent;
	}

	public boolean remove(String name) {
		if (!super.remove(name))
			return false;
		
		String filename = path + "/" + name + extension;
		Resource resFile = resources.getResource(filename);
		if (!resFile.exists())
			// File already deleted
			return true;
		
		try {
			return resFile.getFile().delete();
		} catch (IOException err) {
			return false;
		}
	}

	public boolean remove(IPersistable object) {
		return remove(getObjectId(object));
	}

}
