package com.cms.android.wearable.service.impl;

interface IRfcommServerCallback {

	boolean onRFCOMMSocketReady(String address);
	
	void onRFCOMMSocketConnected();
	
	void onRFCOMMSocketDisconnected(int cause,String address);
	
	void onDataReceived(in byte[] bytes);
	
	void onDataSent(in byte[] bytes);
}