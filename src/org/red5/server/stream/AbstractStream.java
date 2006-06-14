package org.red5.server.stream;

import org.red5.server.api.IScope;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamCodecInfo;

public abstract class AbstractStream implements IStream {
	private String name;
	private IStreamCodecInfo codecInfo;
	private IScope scope;

	public String getName() {
		return name;
	}

	public IStreamCodecInfo getCodecInfo() {
		return codecInfo;
	}

	public IScope getScope() {
		return scope;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setCodecInfo(IStreamCodecInfo codecInfo) {
		this.codecInfo = codecInfo;
	}
	
	public void setScope(IScope scope) {
		this.scope = scope;
	}
}
