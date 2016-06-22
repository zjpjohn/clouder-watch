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
package com.cms.android.activity;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.cms.android.R;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.Wearable;

/**
 * ClassName: MainActivity
 * 
 * @description MainActivity
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MainActivity extends Activity implements OnConnectionFailedListener, ConnectionCallbacks,
		MessageApi.MessageListener, NodeApi.NodeListener {

	private static final String TAG = "MainActivity";

	private EditText mMsgEdit;

	private Button mMsgBtn;

	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private MobvoiApiClient mobvoiApiClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();

		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
	}

	private void initViews() {
		mMsgEdit = (EditText) findViewById(R.id.et_msg);
		mMsgBtn = (Button) findViewById(R.id.btn_msg);
		mMsgBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String content = mMsgEdit.getText().toString();
				Log.d(TAG, "手机端：发送消息时间:" + sdf.format(new Date()) + " ,内容:" + mMsgEdit.getText().toString());
				Wearable.MessageApi.sendMessage(mobvoiApiClient, getPackageName(), "/test", content.getBytes())
						.setResultCallback(new ResultCallback<SendMessageResult>() {
							@Override
							public void onResult(SendMessageResult sendMessageResult) {
								Log.e(TAG, "手机端：消息结果返回, sendMessageResult status: "
										+ sendMessageResult.getStatus().toString());
							}
						});
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		mobvoiApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Wearable.MessageApi.removeListener(mobvoiApiClient, this);
		mobvoiApiClient.disconnect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
		Wearable.MessageApi.removeListener(mobvoiApiClient, this);
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.e(TAG, "手机端：CMS服务已连接");
		Wearable.MessageApi.addListener(mobvoiApiClient, this);
		Wearable.NodeApi.addListener(mobvoiApiClient, this);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e(TAG, "手机端：CMS服务挂起 cause:" + cause);
		mobvoiApiClient.connect();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.e(TAG, "手机端：CMS接收到消息,消息为" + messageEvent.toString());
	}

	@Override
	public void onPeerConnected(Node node) {
		Log.d(TAG, "onPeerConnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
	}

	@Override
	public void onPeerDisconnected(Node node) {
		Log.d(TAG, "onPeerDisconnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
	}
}
