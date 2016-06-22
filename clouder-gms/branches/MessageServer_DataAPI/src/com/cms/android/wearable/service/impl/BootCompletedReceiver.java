package com.cms.android.wearable.service.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;

public class BootCompletedReceiver extends BroadcastReceiver {

	private static final String TAG = "BootCompletedReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		LogTool.d(TAG, "接收到开机启动广播:" + intent.toString());
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			if (!BleUtil.isServiceRunning(context, BLEPeripheralService.BLE_SERVICE_PACKAGE_PATH)) {
				LogTool.d(TAG, "BLE Pheripheral Service未运行,BootCompletedReceiver启动该服务");
				try {
					Intent serviceIntent = new Intent();
					serviceIntent.setAction(BLEPeripheralService.BLE_PHERIPHERAL_SERVICE_ACTION);
					Intent newIntent = Utils.createExplicitFromImplicitIntent(context, serviceIntent);
					if (newIntent != null) {
						context.startService(newIntent);
					}
				} catch (Exception e) {
					LogTool.e(TAG, "Exception", e);
				}
			} else {
				LogTool.d(TAG, "BLE Pheripheral Service 运行中");
			}
		}
	}
}
