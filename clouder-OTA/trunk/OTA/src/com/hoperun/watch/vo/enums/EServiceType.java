package com.hoperun.watch.vo.enums;

public enum EServiceType {
	
	QUERY_VERSION_NO("/queryVersionNo"),DOWNLOAD("/versionDownLoad");
	
	public String path;

	EServiceType(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
	
}
