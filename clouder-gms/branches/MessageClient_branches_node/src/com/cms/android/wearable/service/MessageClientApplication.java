package com.cms.android.wearable.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class MessageClientApplication extends Application {

	private static final String TAG = "MessageClientApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "MessageClient Application create...");
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
		registerReceiver(new TimeTickBroadcastReceiver(), filter);
	}

	private class TimeTickBroadcastReceiver extends BroadcastReceiver {

		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Log.d(TAG, "ACTION_TIME_TICK->" + sdf.format(new Date()));
				if (!BleUtil
						.isServiceRunning(MessageClientApplication.this, BLECentralService.BLE_SERVICE_PACKAGE_PATH)) {
					Log.d(TAG, "BLE Central Service未运行,MessageClientApplication启动该服务");
					Intent serviceIntent = new Intent();
					serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
					startService(Utils.createExplicitFromImplicitIntent(MessageClientApplication.this, serviceIntent));
				} else {
					Log.d(TAG, "BLE Central Service 运行中");
				}
			}
		}
	}

}
