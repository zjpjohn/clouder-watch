/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.service.common;

import android.app.ActivityManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.text.TextUtils;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClassName: BleUtil
 *
 * @description BleUtil
 * @author xing_pengfei
 * @Date 2015-7-29
 *
 */
public class BleUtil {

	/**
	 * check if BLE Supported device
	 */
	public static boolean isBLESupported(Context context) {
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}

	/**
	 * get BluetoothManager
	 */
	public static BluetoothManager getManager(Context context) {
		return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
	}

	/**
	 * create AdvertiseSettings
	 *
	 * @param connectable
	 * @param timeoutMillis
	 * @return
	 */
	public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		builder.setConnectable(connectable);
		builder.setTimeout(timeoutMillis);
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		return builder.build();
	}

	/**
	 * create CloudWatch AdvertiseData
	 *
	 * @return
	 */
	public static AdvertiseData createCloudWatchAdvertiseData() {
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		builder.addServiceUuid(new ParcelUuid(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION)));
		builder.addServiceUuid(new ParcelUuid(UUID.fromString(BleUuid.SERVICE_CLOUDER_WATCH)));
		builder.addServiceUuid(new ParcelUuid(UUID.fromString(BleUuid.SERVICE_CURRENT_TIME)));
		//builder.setIncludeDeviceName(true);
		AdvertiseData adv = builder.build();
		return adv;
	}

	public static boolean isBluetoothAddress(String address) {
		// B0:E0:3C:ED:48:0E
		if (TextUtils.isEmpty(address)) {
			return false;
		}
		Pattern pattern = Pattern.compile("^(\\w{2}:){5}(\\w{2})$");
		Matcher matcher = pattern.matcher(address);
		return matcher.find();
	}

	/**
	 * 判定服务是否运行
	 *
	 * @param serviceName
	 * @return
	 */
	public static boolean isServiceRunning(Context context, String serviceName) {
		if (TextUtils.isEmpty(serviceName)) {
			return false;
		}
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> mServiceList = manager.getRunningServices(100);
		for (int i = 0; i < mServiceList.size(); i++) {
			String clasString = mServiceList.get(i).service.getClassName();
			if (clasString.equals(serviceName)) {
				return true;
			}
		}
		return false;
	}

}
