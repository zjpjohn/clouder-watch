package com.cms.android.wearable.service;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MessageClientApplication extends Application {

	private static final String TAG = "MessageClientApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		LogTool.e(TAG, "MessageClient Application create...");
		startBLECentralService();
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
		registerReceiver(new TimeTickBroadcastReceiver(), filter);
	}

	private class TimeTickBroadcastReceiver extends BroadcastReceiver {

		@SuppressLint("SimpleDateFormat")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar cal = Calendar.getInstance();
				LogTool.d(TAG, "ACTION_TIME_TICK->" + sdf.format(cal.getTime()));
				int minute = cal.get(Calendar.MINUTE);
				int second = cal.get(Calendar.SECOND);
				/**
				 * 0分0秒即意味着每一小时触发一次
				 */
				if (minute == 0 && second == 0) {
					FileUtil.deletCacheFile();
				}
				boolean toggle = Utils.getShardBondToggle(context);
				if (toggle) {
					startBLECentralService();
				} else {
					LogTool.d(TAG, "toggle is false,do nothing.");
				}
			}
		}
	}

	private void startBLECentralService() {
		try {
			Intent serviceIntent = new Intent();
			serviceIntent.putExtra(Utils.BLUETOOTH_BOND_ADDRESS,
					Utils.getShardBondAddress(MessageClientApplication.this));
			serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
			Intent newIntent = Utils.createExplicitFromImplicitIntent(MessageClientApplication.this, serviceIntent);
			if (newIntent != null) {
				startService(newIntent);
			}
		} catch (Exception e) {
			LogTool.e(TAG, "Exception", e);
		}
	}

}
