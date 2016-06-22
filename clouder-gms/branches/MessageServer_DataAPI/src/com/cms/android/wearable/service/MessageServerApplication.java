package com.cms.android.wearable.service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLEPeripheralService;

public class MessageServerApplication extends Application {

	private static final String TAG = "MessageClientApplication";

	/**
	 * 缓存最长存留时间
	 */
	private static final int MAX_CACHE_TIME = 24 * 60 * 60 * 1000;

	@Override
	public void onCreate() {
		super.onCreate();
		LogTool.d(TAG, "MessageClient Application create...");
		startBLEPeripheralService();
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
					deletCacheFile();
				}
				LogTool.d(TAG, "ACTION_TIME_TICK->" + sdf.format(cal.getTime()));
				startBLEPeripheralService();

			}
		}
	}

	private void startBLEPeripheralService() {
		try {
			Intent serviceIntent = new Intent();
			serviceIntent.setAction(BLEPeripheralService.BLE_PHERIPHERAL_SERVICE_ACTION);
			Intent newIntent = Utils.createExplicitFromImplicitIntent(MessageServerApplication.this, serviceIntent);
			if (newIntent != null) {
				startService(newIntent);
			}
		} catch (Exception e) {
			LogTool.e(TAG, "Exception", e);
		}
	}

	private void deletCacheFile() {
		File deleteDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "//cloudwatchcache");
		LogTool.d(TAG, "deleteDirectory->" + deleteDirectory);
		if (deleteDirectory.isDirectory()) {
			File[] files = deleteDirectory.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					long length = file.length();
					if ((new Date().getTime() - file.lastModified()) > MAX_CACHE_TIME) {
						boolean isSuccess = file.delete();
						LogTool.i(TAG, "name = " + name + " length = " + length + " isSuccess = " + isSuccess);
					}
				}
			}
		} else {
			LogTool.d(TAG, "not a directory");
		}
	}

}
