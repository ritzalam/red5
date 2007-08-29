package org.red5.server.war;

import java.io.Serializable;

public class WebSettings implements Serializable {

	private String path;

	private String[] configs;

	private String webAppKey;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String[] getConfigs() {
		return configs;
	}

	public void setConfigs(String[] configs) {
		this.configs = configs;
	}

	public String getWebAppKey() {
		return webAppKey;
	}

	public void setWebAppKey(String webAppKey) {
		this.webAppKey = webAppKey;
	}

}