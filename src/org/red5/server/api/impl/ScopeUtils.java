package org.red5.server.api.impl;

import org.red5.server.api.Scope;

public class ScopeUtils {
	
	private static final String SLASH = "/";
	
	public static Scope resolveScope(Scope from, String path){
		Scope current = from;
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
			current = current.getChildScope(part);
		}
		return current;
	}

	public static Scope findRoot(Scope from){
		Scope current = from;
		while(current.hasParent()){
			current = current.getParent();
		}
		return current;
	}
	
	public static boolean isAncestor(Scope from, Scope ancestor){
		Scope current = from;
		while(current.hasParent()){
			current = current.getParent();
			if(current.equals(ancestor)) return true;
		}
		return false;
	}
	
}
