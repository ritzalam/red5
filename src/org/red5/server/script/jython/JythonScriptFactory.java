package org.red5.server.script.jython;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.scripting.ScriptFactory} implementation for a
 * Python script.
 * 
 * @author Yan Weng
 * @see http://yanweng.blogspot.com/2006/02/prototype-of-jython-scriptfactory-for.html
 */
public class JythonScriptFactory implements ScriptFactory {

	private static Logger logger = Logger.getLogger(JythonScriptFactory.class);
	
	private final String scriptSourceLocator;
	private final Class[] scriptInterfaces;
	private final Class extendedClass;

	public JythonScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator);
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = new Class[]{};
		this.extendedClass = null;
	}
	
	public JythonScriptFactory(String scriptSourceLocator, Class[] scriptInterfaces) {
		Assert.hasText(scriptSourceLocator);
		Assert.notEmpty(scriptInterfaces);
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
		this.extendedClass = null;
	}
	
	public JythonScriptFactory(String scriptSourceLocator, Class[] scriptInterfaces, Class extendedClass) {
		Assert.hasText(scriptSourceLocator);
		Assert.notEmpty(scriptInterfaces);
		Assert.notNull(extendedClass);
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
		this.extendedClass = extendedClass;
	}
	
	public String getScriptSourceLocator() {
		return scriptSourceLocator;
	}

	public Class[] getScriptInterfaces() {
		return scriptInterfaces;
	}

	public boolean requiresConfigInterface() {
		return true;
	}

	public Object getScriptedObject(ScriptSource scriptSourceLocator, Class[] scriptInterfaces)
			throws IOException, ScriptCompilationException {
		String strScript = scriptSourceLocator.getScriptAsString();
		
		if (scriptInterfaces.length > 0) {   
			try {
				PythonInterpreter interp = new PythonInterpreter();
				interp.exec(strScript);
				interp.exec("this = getInstance()");    
				return interp.get("this", scriptInterfaces[0]);    
			} catch (Exception ex) {
				throw new ScriptCompilationException(ex.getMessage());
			}
		}
		logger.error("No scriptInterfaces provided.");
		return null;
	}

}
