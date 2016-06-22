/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.hoperun.records.activity;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.CallLog;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Wearable;
import com.hoperun.records.R;
import com.hoperun.records.RoundImageView;
import com.hoperun.records.service.MessageListenerService;

/**
 * ClassName: IncomingActivity
 *
 * @description
 * @author xing_peng
 * @Date 2015-7-27
 * 
 */
public class IncomingActivity extends Activity implements OnClickListener, OnConnectionFailedListener,
		ConnectionCallbacks, MessageApi.MessageListener {

	private static final String TAG = "IncomingActivity";

	private ImageButton btnAccept, btnTerminate, btnReject;
	private View callView, acceptView;
	private RoundImageView head, accHead;
	private Chronometer timer;
	private TextView nameTextView, namingTextView;

	private MobvoiApiClient mobvoiApiClient;
	private Context mContext;
	private ContentResolver resolver;

	private BluetoothHeadsetClient mHeadsetClient;
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;

	private int[] images = { 0, R.drawable.bg_call_light_1, R.drawable.bg_call_light_2, R.drawable.bg_call_light_3 };
	private int SIGN = 0, num = 0;
	private String mName, mNum;
	private Long callTime;

	private static String MOBILE_DISCONNECT_HS = "/call/disconnect";
	private static String MOBILE_CONNECT_HS = "/call/connect";

	private boolean isDial = false;
	private boolean isConnect = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_incoming);
		mContext = this;

		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mAdapter.getProfileProxy(this, listener, BluetoothProfile.HEADSET_CLIENT);

		Set<BluetoothDevice> bondDevices = mAdapter.getBondedDevices();
		Iterator<BluetoothDevice> it = bondDevices.iterator();
		while (it.hasNext()) {
			mDevice = it.next();
			Log.d(TAG, "name = " + mDevice.getName() + ",address:" + mDevice.getAddress());
		}

		Intent intent = getIntent();
		if (intent.getStringExtra("command").equals("dialCall")) {
			isDial = true;
		}
		
		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.incoming_main_stub);
		stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
			@Override
			public void onLayoutInflated(WatchViewStub stub) {
				
				initViews();

				IntentFilter filter = new IntentFilter();
				filter.addAction(MessageListenerService.ACCEPT_ACTION);
				filter.addAction(MessageListenerService.REJECT_ACTION);
				filter.addAction(MessageListenerService.TERMINATE_ACTION);

				registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String action = intent.getAction();
						if (action.equals(MessageListenerService.ACCEPT_ACTION)) {
							callingPage(); // 通话页面
							acceptPage();
							timer.setBase(SystemClock.elapsedRealtime());
							timer.start();
						}
						if (action.equals(MessageListenerService.REJECT_ACTION)) {
							setPhoneRecords();
							finish();
						}
						if (action.equals(MessageListenerService.TERMINATE_ACTION)) {
							setPhoneRecords();
							finish();
						}
					}
				}, filter);
			}
		});
	}

	private BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {

		@Override
		public void onServiceDisconnected(int profile) {
			Log.d(TAG, "onServiceDisconnected profile = " + profile);
			if (profile == BluetoothProfile.HEADSET_CLIENT) {
				mHeadsetClient = null;
			}
			isConnect = false;
		}

		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			Log.d(TAG, "onServiceConnected proxy != null " + (proxy != null) + " profile = " + profile);
			if (proxy != null && profile == BluetoothProfile.HEADSET_CLIENT && !isConnect) {
				mHeadsetClient = (BluetoothHeadsetClient) proxy;
				connectHeadset();

				if (isDial) {
					isDial = false;
					DialThread dialThread = new DialThread();
					dialThread.start();
				}
				
			}
			Log.d(TAG, "onServiceConnected mHeadsetClient != null " + (mHeadsetClient != null));
		}
	};

	@SuppressLint("HandlerLeak")
	private void initViews() {

		head = (RoundImageView) findViewById(R.id.head);
		accHead = (RoundImageView) findViewById(R.id.accHead);
		callView = findViewById(R.id.Top);
		acceptView = findViewById(R.id.acceptCall);
		nameTextView = (TextView) findViewById(R.id.name);
		namingTextView = (TextView) findViewById(R.id.naming);

		btnAccept = (ImageButton) findViewById(R.id.callAccept);
		btnTerminate = (ImageButton) findViewById(R.id.callTerminate);
		btnReject = (ImageButton) findViewById(R.id.callReject);
		btnAccept.setOnClickListener(this);
		btnTerminate.setOnClickListener(this);
		btnReject.setOnClickListener(this);

		timer = (Chronometer) this.findViewById(R.id.chronometer);
		callTime = new Date().getTime();

		Intent intent = getIntent();
		mName = intent.getStringExtra("name");

		nameTextView.setText(mName);
		namingTextView.setText(mName);

		mNum = intent.getStringExtra("number");
		byte[] b = intent.getByteArrayExtra("img");
		Bitmap bm = BitmapFactory.decodeByteArray(b, 0, b.length);

		head.setImageBitmap(bm);
		accHead.setImageBitmap(bm);

		String command = intent.getStringExtra("command");
		if (command.equals("incomingCall")) {
			incomingPage();
		} else {
			callingPage();
		}

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == SIGN) {
					callView.setBackgroundResource(images[num++]);
					if (num >= images.length) {
						num = 0;
					}
				}
			}
		};

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = SIGN;
				handler.sendMessage(msg);
			}
		}, 0, 200);
	}

	private void incomingPage() {
		btnAccept.setVisibility(View.VISIBLE);
		btnReject.setVisibility(View.VISIBLE);
		btnTerminate.setVisibility(View.INVISIBLE);
	}

	private void callingPage() {
		btnTerminate.setVisibility(View.VISIBLE);
		btnAccept.setVisibility(View.INVISIBLE);
		btnReject.setVisibility(View.INVISIBLE);
	}

	private void acceptPage() {
		acceptView.setVisibility(View.VISIBLE);
		callView.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.callAccept:
			callingPage();
			acceptPage();
			acceptPhoneCall();
			// 将计时器清零
			timer.setBase(SystemClock.elapsedRealtime());
			timer.start();
			break;
		case R.id.callTerminate:
			terminatePhoneCall();
			finish();
			break;
		case R.id.callReject:
			rejectPhoneCall();
			finish();
			break;
		default:
			break;
		}
	}

	private void setPhoneRecords() {
		resolver = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(CallLog.Calls.CACHED_NAME, mName == null ? mNum : mName);
		values.put(CallLog.Calls.NUMBER, mNum);
		values.put(CallLog.Calls.DATE, callTime);
		values.put(CallLog.Calls.TYPE, 0);// 来电:1,拨出:2,未接:3
		resolver.insert(CallLog.Calls.CONTENT_URI, values);
	}

	@Override
	protected void onStart() {
		super.onStart();
//		mobvoiApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// mobvoiApiClient.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		disconnectHeadset();
//		mAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, mHeadsetClient);
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.e(TAG, "手机端：CMS服务已连接");
		Wearable.MessageApi.addListener(mobvoiApiClient, this);
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

	/**
	 * 发送消息通知
	 */
	private void sendMessage(String path, byte[] bytes) {
		if (bytes == null)
			bytes = new byte[] { 0 };
		Log.d(TAG, "sendMessage, path + " + path + ", bytes" + bytes);
		mobvoiApiClient.connect();
		Wearable.MessageApi.sendMessage(mobvoiApiClient, getPackageName(), path, bytes).setResultCallback(
				new ResultCallback<SendMessageResult>() {
					@Override
					public void onResult(SendMessageResult sendMessageResult) {
						Log.e(TAG, "消息结果返回, sendMessageResult status: " + sendMessageResult.getStatus().toString());
						if (!sendMessageResult.getStatus().isSuccess()) {
							Log.e(TAG, "Failed to send message with status code: "
									+ sendMessageResult.getStatus().getCode());
						}
					}
				});
	}

	/**
	 * 接听来电
	 */
	public void acceptPhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "acceptPhoneCall");
			mHeadsetClient.acceptCall(mDevice, BluetoothHeadsetClient.CALL_ACCEPT_NONE);
			// disconnectHeadset();
		}
	}

	/**
	 * 拒接电话
	 */
	public void rejectPhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "rejectPhoneCall");
			mHeadsetClient.rejectCall(mDevice);
		}
		setPhoneRecords();
		finish();
	}

	/**
	 * 挂断电话
	 */
	public void terminatePhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "terminatePhoneCall");
			mHeadsetClient.terminateCall(mDevice, 0);
		}
		setPhoneRecords();
		finish();
	}

	/**
	 * 拨打电话
	 */
	public void dialPhoneCall(String phoneNumber) {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "dialPhoneCall" + mHeadsetClient.dial(mDevice, phoneNumber));
			// disconnectHeadset();
		}
	}

	/**
	 * 断开连接
	 */
	private void disconnectHeadset() {
		if (mDevice != null && mHeadsetClient != null) {
			Log.d(TAG, "disconnectHeadset");
			mHeadsetClient.disconnect(mDevice);
			sendMessage(MOBILE_CONNECT_HS, null); // 通知手机重新连接蓝牙耳机
		}
	}

	/**
	 * 建立连接
	 */
	private void connectHeadset() {
		if (mDevice != null && mHeadsetClient != null) {
			Log.d(TAG, "connectHeadset");
			sendMessage(MOBILE_DISCONNECT_HS, null); // 通知手机断开蓝牙耳机
			mHeadsetClient.connect(mDevice);

		}
	}
	
	class DialThread extends Thread {
		@Override
		public void run() {
			while (mHeadsetClient.getConnectionState(mDevice) != 0) {
				Log.d(TAG, "connectHeadset state " + mHeadsetClient.getConnectionState(mDevice));
				if (mHeadsetClient.getConnectionState(mDevice) == 2) {
					Log.d(TAG, "connectHeadset success");
					isConnect = true;
					dialPhoneCall(mNum);
					break;
				}
			}
		}
	}
}
