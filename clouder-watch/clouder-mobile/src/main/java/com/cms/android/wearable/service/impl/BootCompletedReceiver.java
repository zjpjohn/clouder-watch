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
			if (!BleUtil.isServiceRunning(context, BLECentralService.BLE_SERVICE_PACKAGE_PATH)) {
				LogTool.d(TAG, "BLE Central Service未运行,BootCompletedReceiver启动该服务");
				boolean toggle = Utils.getShardBondToggle(context);
				if (toggle) {
					try {
						Intent serviceIntent = new Intent();
						serviceIntent.putExtra(Utils.BLUETOOTH_BOND_ADDRESS, Utils.getShardBondAddress(context));
						serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
						Intent newIntent = Utils.createExplicitFromImplicitIntent(context, serviceIntent);
						if (newIntent != null) {
							context.startService(newIntent);
						}
					} catch (Exception e) {
						LogTool.e(TAG, "Exception", e);
					}
				} else {
					LogTool.d(TAG, "toggle is false,do nothing.");
				}
			} else {
				LogTool.d(TAG, "BLE Central Service 运行中");
			}
		}
	}
}
