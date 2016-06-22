package com.cms.android.wearable.service.impl;

interface IRfcommClientCallback {

	void onRFCOMMSocketConnected();
	
	void onRFCOMMSocketDisconnected(int cause);
	
	void onMessageReceived(in byte[] bytes);
	
	void onMessageSend(String requestId,String statusCode,String versionCode);
  
}
