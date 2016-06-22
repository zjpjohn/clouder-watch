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
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.List;

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
