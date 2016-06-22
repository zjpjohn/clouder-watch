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
	// manufacturer name string char. 暂时用来通知手表端rfcomm server已经打开，让手表端主动去连接
	public static final String CHAR_MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
	// model number string char. 暂时用作通知手机端打开rfcomm server
	public static final String CHAR_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
	// serial number string char.暂时用作断开状态
	public static final String CHAR_SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
	// system id string char.暂时用作心跳
	public static final String CHAR_SYSTEM_ID_STRING = "00002a23-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_NODE = "00002a26-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_RFCOMM_CONNECTED_STATUS = "00002a28-0000-1000-8000-00805f9b34fb";
	//	IEEE 11073-20601 Regulatory Certification Data List	 暂时用来表示BLE心跳
//	public static final String CHAR_HEART_BEAT = "00002a2a-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_HEART_BEAT = "00002a23-0000-1000-8000-00805f9b34fb";
	
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
	public static final String SERVICE_CLOUDER_WATCH = "0001a000-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLOUDER_WATCH_RFCOMM = "0001b000-0000-1000-8000-00805f9b34fb";
//	public static final String CHAR_CLOUDER_WATCH_VALUE = "0001B001-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLOUDER_WATCH_NOTIFICATION = "0001b002-0000-1000-8000-00805f9b34fb";
	
//	Time profile
	public static final String SERVICE_CURRENT_TIME = "00001805-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CURRENT_TIME = "00002a2b-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_LOCAL_TIME_INFORMATION = "00002a0f-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_REFERENCE_TIME_INFORMATION = "00002a14-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CLIENT_CHARACTERISTIC_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";

}
