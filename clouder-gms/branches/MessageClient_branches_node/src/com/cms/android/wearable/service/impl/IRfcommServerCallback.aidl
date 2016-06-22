package com.cms.android.wearable.service.impl;

interface IRfcommServerCallback {

	boolean onRFCOMMSocketReady(String address);
	
	void onRFCOMMSocketConnected();
	
	void onRFCOMMSocketDisconnected(int cause,String address);
	
	void onMessageReceived(in byte[] bytes);
	
	void onMessageSend(String requestId,String statusCode,String versionCode);
}