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
package com.hoperun.message.activity;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;
import com.cms.android.wearable.service.impl.IBLECentralCallback;
import com.cms.android.wearable.service.impl.IBLECentralService;
import com.hoperun.message.R;

/**
 * ClassName: MainActivity
 * 
 * @description MainActivity
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "MainActivity";

	private Button mStartBtn, mStopBtn, mSendMessageBtn, mSyncTimeBtn;

	private EditText mMsgEditText;

	private TextView mSyncTimeText;

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private String mDeviceName, mDeviceAddress;

	private IBLECentralService mBLECentralService;
	
	private ServiceConnection connection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		Log.d(TAG, "DeviceName is " + mDeviceName + " DeviceAddress is " + mDeviceAddress);

		init();
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(connection != null){
			unbindService(connection);
		}
	}



	private void init() {
		mMsgEditText = (EditText) findViewById(R.id.et_msg);

		mStartBtn = (Button) findViewById(R.id.btn_start_service);
		mStopBtn = (Button) findViewById(R.id.btn_stop_service);
		mSendMessageBtn = (Button) findViewById(R.id.btn_send_message);
		mSyncTimeBtn = (Button) findViewById(R.id.btn_sync_time);
		mSyncTimeText = (TextView) findViewById(R.id.tv_sync_time);
		mStartBtn.setOnClickListener(this);
		mStopBtn.setOnClickListener(this);
		mSendMessageBtn.setOnClickListener(this);
		mSyncTimeBtn.setOnClickListener(this);
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start_service:
			Log.d(TAG, "connect to BLE.");
			Intent serviceIntent = new Intent();
			serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
			connection = new ServiceConnection() {

				@Override
				public void onServiceDisconnected(ComponentName name) {

				}

				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					Log.d(TAG, "[onServiceConnected] name:" + name);
					mBLECentralService = IBLECentralService.Stub.asInterface(service);
					try {
						mBLECentralService.connect(mDeviceAddress);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			};
			bindService(Utils.createExplicitFromImplicitIntent(this, serviceIntent), connection, Context.BIND_AUTO_CREATE);
			break;
		case R.id.btn_stop_service:

			break;
		case R.id.btn_send_message:
			Log.d(TAG, "onClick send message.");
			if (mBLECentralService != null) {
				try {
					mBLECentralService.sendMessage(new IBLECentralCallback() {

						@Override
						public IBinder asBinder() {
							return null;
						}

						@Override
						public void setStatusRsp(Status status) throws RemoteException {
							Log.d(TAG, ">>>><<<<setStatusRsp " + status.toString());
						}

						@Override
						public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
							Log.d(TAG, ">>>><<<<setSendMessageRsp");

						}

						@Override
						public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
								throws RemoteException {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
							// TODO Auto-generated method stub
							
						}

						@Override
						public String getPackageName() throws RemoteException {
							// TODO Auto-generated method stub
							return null;
						}

					}, getPackageName(), getPackageName(), mMsgEditText.getText().toString().getBytes());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			break;
		case R.id.btn_sync_time:
			Log.d(TAG, "onClick send message.");
			if (mBLECentralService != null) {
				try {
					Date date = new Date();
					long time = date.getTime();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
					mSyncTimeText.setText("当前同步时间为" + sdf.format(date));
					mBLECentralService.syncTime(time, 0x1);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			break;

		default:
			break;
		}
	}

}
