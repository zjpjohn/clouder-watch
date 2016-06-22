package com.cms.android.wearable.service.impl;

import android.bluetooth.BluetoothDevice;

interface IRfcommClientCallback {

	void onRFCOMMSocketConnected(in BluetoothDevice device);
	
	void onRFCOMMSocketDisconnected(int cause);
	
	void onDataReceived(in byte[] bytes);
	
	void onDataSent(in byte[] bytes);
	
	void onConnectFailure(in BluetoothDevice device);
}
