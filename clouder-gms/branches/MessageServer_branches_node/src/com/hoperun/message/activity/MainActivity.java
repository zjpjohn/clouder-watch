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

import com.cms.android.common.api.Status;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.service.impl.BLEPeripheralService;
import com.cms.android.wearable.service.impl.IBLEPeripheralCallback;
import com.cms.android.wearable.service.impl.IBLEPeripheralService;
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

	private EditText mMsgEdit;

	private Button mStartBtn, mStopBtn, mMsgBtn;

	private IBLEPeripheralService mBLEPeripheralService;

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "[onServiceConnected] name:" + name);
			mBLEPeripheralService = IBLEPeripheralService.Stub.asInterface(service);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
	}

	private void init() {
		mMsgEdit = (EditText) findViewById(R.id.et_msg);

		mStartBtn = (Button) findViewById(R.id.btn_start);
		mStopBtn = (Button) findViewById(R.id.btn_stop);
		mMsgBtn = (Button) findViewById(R.id.btn_msg);

		mStartBtn.setOnClickListener(this);
		mStopBtn.setOnClickListener(this);
		mMsgBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		switch (v.getId()) {
		case R.id.btn_start:
			intent = new Intent(this, BLEPeripheralService.class);
			bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
			// startService(intent);
			break;
		case R.id.btn_stop:
			// stopService(intent);
			unbindService(mServiceConnection);
			break;
		case R.id.btn_msg:
			if (mBLEPeripheralService != null) {
				try {
					mBLEPeripheralService.sendMessage(new IBLEPeripheralCallback() {

						@Override
						public IBinder asBinder() {
							return null;
						}

						@Override
						public void setStatusRsp(Status status) throws RemoteException {
							
						}

						@Override
						public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
							Log.d(TAG, "server setSendMessageRsp&&&");
							
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
					}, "com.cms.android", "/servertest", mMsgEdit.getText().toString().getBytes());
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
