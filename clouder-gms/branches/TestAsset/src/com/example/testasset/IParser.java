package com.example.testasset;

public interface IParser {
	
	public static final int TYPE_MESSAGE = 0;
	
	public static final int TYPE_DATA = 1;
	
	byte[] parse();
	
	int getType();
	
	void callback();
}
