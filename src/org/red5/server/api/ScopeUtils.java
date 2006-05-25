package org.red5.server.api;

import java.lang.reflect.Constructor;

import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.springframework.context.ApplicationContext;

/**
 * Collection of utils for working with scopes
 */
public class ScopeUtils {
	
	private static final int GLOBAL = 0x00;
	private static final int APPLICATION = 0x01;
	private static final int ROOM = 0x02;
	private static final String SERVICE_CACHE_PREFIX = "__service_cache:";
	
	private static final String SLASH = "/";
	
	public static IScope resolveScope(IScope from, String path){
		IScope current = from;
		if(path.startsWith(SLASH)){
			current = ScopeUtils.findRoot(current);
			path = path.substring(1,path.length());
		}
		if(path.endsWith(SLASH)){
			path = path.substring(0,path.length()-1);
		}
		String[] parts = path.split(SLASH);
		for(int i=0; i<parts.length; i++){
			String part = parts[i];
			if(part.equals(".")) continue;
			if(part.equals("..")){
				if(!current.hasParent()) return null;
				current = current.getParent();
				continue;
			}
			if(!current.hasChildScope(part)) return null;
			current = current.getScope(part);
		}
		return current;
	}

	public static IScope findRoot(IScope from){
		IScope current = from;
		while(current.hasParent()){
			current = current.getParent();
		}
		return current;
	}
	
	public static IScope findApplication(IScope from){
		IScope current = from;
		while(current.hasParent() && current.getDepth() != APPLICATION){
			current = current.getParent();
		}
		return current;
	}
	
	public static boolean isAncestor(IBasicScope from, IBasicScope ancestor){
		IBasicScope current = from;
		while(current.hasParent()){
			current = current.getParent();
			if(current.equals(ancestor)) return true;
		}
		return false;
	}

	public static boolean isRoot(IBasicScope scope){
		return !scope.hasParent();	
	}

	public static boolean isGlobal(IBasicScope scope){
		return scope.getDepth() == GLOBAL;	
	}
	
	public static boolean isApp(IBasicScope scope){
		return scope.getDepth() == APPLICATION;
	}

	public static boolean isRoom(IBasicScope scope){
		return scope.getDepth() >= ROOM;
	}
	
	public static Object getScopeService(IScope scope, String name) {
		return getScopeService(scope, name, null);
	}
	
	public static Object getScopeService(IScope scope, String name, Class defaultClass) {
		if (scope.hasAttribute(IPersistable.TRANSIENT_PREFIX + SERVICE_CACHE_PREFIX + name))
			// Return cached service
			return scope.getAttribute(IPersistable.TRANSIENT_PREFIX + SERVICE_CACHE_PREFIX + name);
		
		final IContext context = scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		Object result;
		if (!appCtx.containsBean(name)) {
			if (defaultClass == null)
				return null;
			
			try {
				result = defaultClass.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else
			result = appCtx.getBean(name);
		
		// Cache service
		scope.setAttribute(IPersistable.TRANSIENT_PREFIX + SERVICE_CACHE_PREFIX + name, result);
		return result;
	}

	private static Constructor getPersistenceStoreConstructor(Class theClass, Class[] interfaces) throws Exception {
		Constructor constructor = null;
		for (Class interfaceClass : interfaces) {
			try {
				constructor = theClass.getConstructor(new Class[]{interfaceClass});
			} catch (NoSuchMethodException err) {
				// Ignore this error
			}
			if (constructor != null)
				break;
			
			constructor = getPersistenceStoreConstructor(theClass, interfaceClass.getInterfaces());
			if (constructor != null)
				break;
		}
		return constructor;
	}
	
	public static IPersistenceStore getPersistenceStore(IBasicScope scope, String className) throws Exception {
		Class persistenceClass = Class.forName(className);
		Constructor constructor = getPersistenceStoreConstructor(persistenceClass, scope.getClass().getInterfaces());
		if (constructor == null) {
			// Search in superclasses of the scope.
			Class superClass = scope.getClass().getSuperclass();
			while (superClass != null) {
				constructor = getPersistenceStoreConstructor(persistenceClass, superClass.getInterfaces());
				if (constructor != null)
					break;
				
				superClass = superClass.getSuperclass();
			}
		}
		
		if (constructor == null)
			throw new NoSuchMethodException();
		
		return (IPersistenceStore) constructor.newInstance(new Object[]{scope});
	}
	
}
