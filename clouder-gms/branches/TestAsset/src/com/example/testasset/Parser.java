package com.example.testasset;

import java.util.UUID;

public abstract class Parser implements IParser {

	// 4a0304fc-7c3e-4f0e-9143-9d70cc2389f7 长度为36位
	protected String uuid;
	
	protected Parser() {
		super();
		uuid();
	}

	private String uuid() {
		this.uuid = UUID.randomUUID().toString();
		return this.uuid;
	}

	public String getUUID() {
		return this.uuid;
	}

}
