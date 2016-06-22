package com.cms.android.wearable.service.codec;

public interface IParser {
	
	public static final int TYPE_MESSAGE = 1;
	
	public static final int TYPE_DATA = 2;
	
	int getType();
	
}
