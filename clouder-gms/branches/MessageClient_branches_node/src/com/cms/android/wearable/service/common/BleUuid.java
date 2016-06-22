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

/**
 * ClassName: BleUtil
 * 
 * @description BleUtil
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class BleUuid {
	
//	 180A Device Information
	public static final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_SYSTEM_ID_STRING = "00002A23-0000-1000-8000-00805f9b34fb";
//
//	// 1802 Immediate Alert
//	public static final String SERVICE_IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";
//
//	// 180F Battery
//	public static final String SERVICE_BATTERY_SERVICE = "0000180F-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
//
//	// 180D Heart Rate
//	public static final String SERVICE_HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_BATTER = "00002a19-0000-1000-8000-00805f9b34fb";

	// Clouder Watch
	public static final String SERVICE_CLOUDER_WATCH = "0001A000-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLOUDER_WATCH_RFCOMM = "0001B000-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_CLOUDER_WATCH_VALUE = "0001B001-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLOUDER_WATCH_NOTIFICATION = "0001B002-0000-1000-8000-00805f9b34fb";
	
//	Time profile
	public static final String SERVICE_CURRENT_TIME = "00001805-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CURRENT_TIME = "00002A2B-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_LOCAL_TIME_INFORMATION = "00002A0F-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_REFERENCE_TIME_INFORMATION = "00002A14-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLIENT_CHARACTERISTIC_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";

}
