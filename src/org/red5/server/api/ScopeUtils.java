package org.red5.server.api;

import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.so.SharedObjectService;
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
	
	public static boolean isAncestor(IScope from, IScope ancestor){
		IScope current = from;
		while(current.hasParent()){
			current = current.getParent();
			if(current.equals(ancestor)) return true;
		}
		return false;
	}

	public static boolean isRoot(IScope scope){
		return !scope.hasParent();	
	}

	public static boolean isGlobal(IScope scope){
		return scope.getDepth() == GLOBAL;	
	}
	
	public static boolean isApp(IScope scope){
		return scope.getDepth() == APPLICATION;
	}

	public static boolean isRoom(IScope scope){
		return scope.getDepth() >= ROOM;
	}
	
	public static Object getScopeService(IScope scope, String name) {
		return getScopeService(scope, name, null);
	}
	
	public static Object getScopeService(IScope scope, String name, Class defaultClass) {
		if (scope.hasAttribute(SERVICE_CACHE_PREFIX + name))
			// Return cached service
			return scope.getAttribute(SERVICE_CACHE_PREFIX + name);
		
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
		scope.setAttribute(SERVICE_CACHE_PREFIX + name, result);
		return result;
	}

}
