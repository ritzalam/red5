package org.red5.classloading;

import java.io.InputStream;
import java.net.URL;

import org.quartz.spi.ClassLoadHelper;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * A <code>ClassLoadHelper</code> that determines the correct class loader to
 * use for a scheduler.
 * 
 * @see org.quartz.spi.ClassLoadHelper
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class QuartzClassLoadHelper implements ClassLoadHelper {

	private static Logger log = Red5LoggerFactory.getLogger(QuartzClassLoadHelper.class);
	
    private ClassLoader initClassLoader;

/*
ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
 if (classLoader == null) {
  	classLoader = this.getClass().getClassLoader();
  	result= classLoader.getResourceAsStream( name );
 } else {
  	result= classLoader.getResourceAsStream( name );
  	if (result == null) {
  	 classLoader = this.getClass().getClassLoader();
  	 result= classLoader.getResourceAsStream( name );
  	}
 }
*/

    /**
     * Called to give the ClassLoadHelper a chance to initialize itself,
     * including the opportunity to "steal" the class loader off of the calling
     * thread, which is the thread that is initializing Quartz.
     */
    public void initialize() {
        initClassLoader = Thread.currentThread().getContextClassLoader();
        log.debug("Initialized with classloader: {}", initClassLoader);
    }

    /**
     * Return the class with the given name.
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return getClassLoader().loadClass(name);
    }

    /**
     * Finds a resource with a given name. This method returns null if no
     * resource with this name is found.
     * @param name name of the desired resource
     * @return a java.net.URL object
     */
    public URL getResource(String name) {
        return getClassLoader().getResource(name);
    }

    /**
     * Finds a resource with a given name. This method returns null if no
     * resource with this name is found.
     * @param name name of the desired resource
     * @return a java.io.InputStream object
     */
    public InputStream getResourceAsStream(String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    /**
     * Enable sharing of the class-loader with 3rd party (e.g. digester).
     *
     * @return the class-loader user be the helper.
     */
    public ClassLoader getClassLoader() {
    	log.debug("Class classloader: {} Thread classloader: {}", this.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
        return Thread.currentThread().getContextClassLoader();
    }
}
