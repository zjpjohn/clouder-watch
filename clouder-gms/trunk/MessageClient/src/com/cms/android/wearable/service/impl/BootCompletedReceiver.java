package com.cms.android.wearable.service.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.Utils;

public class BootCompletedReceiver extends BroadcastReceiver {

	private static final String TAG = "BootCompletedReceiver";

	@Override
    public void onReceive(Context context, Intent intent) {
    	Log.d(TAG, "接收到开机启动广播:" + intent.toString());
    	if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
    		if (!BleUtil
					.isServiceRunning(context, BLECentralService.BLE_SERVICE_PACKAGE_PATH)) {
				Log.d(TAG, "BLE Central Service未运行,BootCompletedReceiver启动该服务");
				Intent serviceIntent = new Intent();
				serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
				context.startService(Utils.createExplicitFromImplicitIntent(context, serviceIntent));
			} else {
				Log.d(TAG, "BLE Central Service 运行中");
			}
    	}
    }
}
