package org.red5.server.tomcat;

/**
 * This class loader provides a means to by-pass the class loader used
 * to launch an application as the system class loader.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class WebappClassLoader extends org.apache.catalina.loader.WebappClassLoader {
	
    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader
     */
    public WebappClassLoader() {
        super();
        this.parent = getParent();
        system = getParent().getParent();
    }

    /**
     * Construct a new ClassLoader with no defined repositories
     */
    public WebappClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = getParent();
        system = getParent().getParent();   
    }

}
