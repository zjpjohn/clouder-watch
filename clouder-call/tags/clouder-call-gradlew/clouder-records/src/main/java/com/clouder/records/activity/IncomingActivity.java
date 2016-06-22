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
package com.clouder.records.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import com.clouder.records.R;
import com.clouder.records.view.RoundImageView;
import com.clouder.records.service.MessageListenerService;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Wearable;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
	private View callView, acceptView, headBgView;
	private RoundImageView head, accHead;
	private Chronometer timer;
	private TextView nameTextView, namingTextView;

	private MobvoiApiClient mobvoiApiClient;
	private Context mContext;
	private ContentResolver resolver;

	private Object mHeadsetClient;
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;

	private Class<?> mHeadsetClientClass;
	private Class<?> mHeadsetClientCallClass;

	private int[] images = { 0, R.drawable.bg_call_light_1, R.drawable.bg_call_light_2, R.drawable.bg_call_light_3 };
	private int SIGN = 0, num = 0;
	private String mName, mNum;
	private Long callTime;

	private static String MOBILE_DISCONNECT_HS = "/call/disconnect";
	private static String MOBILE_CONNECT_HS = "/call/connect";

	private boolean isDial = false;

	/**
	 * Intent sent whenever connection to remote changes.
	 *
	 * <p>Note that features supported by AG are being sent as
	 * booleans with value <code>true</code>,
	 * and not supported ones are <strong>not</strong> being sent at all.</p>
	 */
	public static final String ACTION_CONNECTION_STATE_CHANGED  = "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";

	/**
	 * Intent sent whenever state of a call changes.
	 *
	 * <p>It includes:
	 * {@link #EXTRA_CALL},
	 * with value of instance,
	 * representing actual call state.</p>
	 */
	public static final String ACTION_CALL_CHANGED = "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED";

	/**
	 *  Extra for AG_CALL_CHANGED intent indicates the
	 *  object that has changed.
	 */
	public static final String EXTRA_CALL = "android.bluetooth.headsetclient.extra.CALL";

	/**
	 * Call is active.
	 */
	public static final int CALL_STATE_ACTIVE = 0;
	/**
	 * Call is in held state.
	 */
	public static final int CALL_STATE_HELD = 1;
	/**
	 * Outgoing call that is being dialed right now.
	 */
	public static final int CALL_STATE_DIALING = 2;
	/**
	 * Outgoing call that remote party has already been alerted about.
	 */
	public static final int CALL_STATE_ALERTING = 3;
	/**
	 * Incoming call that can be accepted or rejected.
	 */
	public static final int CALL_STATE_INCOMING = 4;
	/**
	 * Waiting call state when there is already an active call.
	 */
	public static final int CALL_STATE_WAITING = 5;
	/**
	 * Call that has been held by response and hold
	 * (see Bluetooth specification for further references).
	 */
	public static final int CALL_STATE_HELD_BY_RESPONSE_AND_HOLD = 6;
	/**
	 * Call that has been already terminated and should not be referenced as a valid call.
	 */
	public static final int CALL_STATE_TERMINATED = 7;

	/**
	 * Headset Client - HFP HF Role
	 * @hide
	 */
	public static final int HEADSET_CLIENT = 16;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_incoming);
		mContext = this;

		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mAdapter.getProfileProxy(this, listener, HEADSET_CLIENT);

		Set<BluetoothDevice> bondDevices = mAdapter.getBondedDevices();
		Iterator<BluetoothDevice> it = bondDevices.iterator();
		while (it.hasNext()) {
			// TODO
			mDevice = it.next();
			Log.d(TAG, "name = " + mDevice.getName() + ",address:" + mDevice.getAddress());
		}

		Intent intent = getIntent();
		if (intent.getStringExtra("command").equals("dialCall")) {
			isDial = true;
		}

		try {
			mHeadsetClientClass = Class.forName("android.bluetooth.BluetoothHeadsetClient");
			mHeadsetClientCallClass = Class.forName("android.bluetooth.BluetoothHeadsetClientCall");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
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
				filter.addAction(ACTION_CALL_CHANGED);
				filter.addAction(ACTION_CONNECTION_STATE_CHANGED);
				registerReceiver(mReceiver, filter);
			}
		});
	}

	private BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {

		@Override
		public void onServiceDisconnected(int profile) {
			Log.d(TAG, "onServiceDisconnected profile = " + profile);
			if (profile == HEADSET_CLIENT) {
				mHeadsetClient = null;
			}
		}

		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			Log.d(TAG, "onServiceConnected proxy != null " + (proxy != null) + " profile = " + profile);
			if (proxy != null && profile == HEADSET_CLIENT) {
				mHeadsetClient = proxy;
				connectHeadset();
			}
			Log.d(TAG, "onServiceConnected mHeadsetClient != null " + (mHeadsetClient != null));
		}
	};

	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@SuppressWarnings("rawtypes")
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			String action = arg1.getAction();
			Bundle bundle = arg1.getExtras();
			if(ACTION_CALL_CHANGED.equals(action)){
				Object mHeadsetClientCall = bundle.getParcelable(EXTRA_CALL);
				if(mHeadsetClientCall != null){
					try {
						Method mGetState = mHeadsetClientCallClass.getMethod("getState");
						int state = (Integer) mGetState.invoke(mHeadsetClientCall);
						Method mGetNumber = mHeadsetClientCallClass.getMethod("getNumber");
						String number = (String) mGetNumber.invoke(mHeadsetClientCall);
						switch (state) {
							case CALL_STATE_ACTIVE :
								acceptPage();
								timer.setBase(SystemClock.elapsedRealtime());
								timer.start();
								Log.d(TAG, "CALL_STATE_ACTIVE, number : " + number);
								break;
							case CALL_STATE_DIALING :
								Log.d(TAG, "CALL_STATE_DIALING, number : " + number);
								break;
							case CALL_STATE_INCOMING :
								Log.d(TAG, "CALL_STATE_INCOMING, number : " + number);
								break;
							case CALL_STATE_TERMINATED :
								Log.d(TAG, "CALL_STATE_TERMINATED, number : " + number);
								setPhoneRecords();
								finish();
								break;
							default:
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}else if(ACTION_CONNECTION_STATE_CHANGED.equals(action)){
				int newState = bundle.getInt(BluetoothProfile.EXTRA_STATE,-1);
				if (BluetoothProfile.STATE_CONNECTED == newState && isDial) {
					// dial
					dialPhoneCall(mNum);
				}
			} else if (action.equals(MessageListenerService.ACCEPT_ACTION)) {
				callingPage(); // calling page
				acceptPage();
				timer.setBase(SystemClock.elapsedRealtime());
				timer.start();
			} else if (action.equals(MessageListenerService.REJECT_ACTION)) {
				setPhoneRecords();
				finish();
			} else if (action.equals(MessageListenerService.TERMINATE_ACTION)) {
				setPhoneRecords();
				finish();
			}
		}

	};

	@SuppressLint("HandlerLeak")
	private void initViews() {

		head = (RoundImageView) findViewById(R.id.head);
		accHead = (RoundImageView) findViewById(R.id.accHead);
		callView = findViewById(R.id.top);
		headBgView = findViewById(R.id.headBg);
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
					headBgView.setBackgroundResource(images[num++]);
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
			// The timer is reset
			timer.setBase(SystemClock.elapsedRealtime());
			timer.start();
			break;
		case R.id.callTerminate:
			terminatePhoneCall();
			setPhoneRecords();
			finish();
			break;
		case R.id.callReject:
			rejectPhoneCall();
			setPhoneRecords();
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
		values.put(CallLog.Calls.TYPE, 0);// incoming:1,dial:2,not answer:3
		resolver.insert(CallLog.Calls.CONTENT_URI, values);
	}

	@Override
	protected void onStart() {
		super.onStart();
        // mobvoiApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// mobvoiApiClient.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		terminatePhoneCall();
		disconnectHeadset();
		mAdapter.closeProfileProxy(HEADSET_CLIENT, (BluetoothProfile) mHeadsetClient);
		unregisterReceiver(mReceiver);
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "mobile：CMS ConnectionFailed " + connectionResult.toString());
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.e(TAG, "mobile：CMS Connected");
		Wearable.MessageApi.addListener(mobvoiApiClient, this);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e(TAG, "mobile：CMS Suspended cause:" + cause);
		mobvoiApiClient.connect();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.e(TAG, "mobile：CMS Message Received :" + messageEvent.toString());
	}

	/**
	 * Send Message
	 */
	private void sendMessage(String path, byte[] bytes) {
		/**
		if (bytes == null)
			bytes = new byte[]{0};
		Log.d(TAG, "sendMessage, path + " + path + ", bytes" + bytes);
		mobvoiApiClient.connect();
		Wearable.MessageApi.sendMessage(mobvoiApiClient, getPackageName(), path, bytes).setResultCallback(
				new ResultCallback<SendMessageResult>() {
					@Override
					public void onResult(SendMessageResult sendMessageResult) {
						Log.e(TAG, "response, sendMessageResult status: " + sendMessageResult.getStatus().toString());
						if (!sendMessageResult.getStatus().isSuccess()) {
							Log.e(TAG, "Failed to send message with status code: "
									+ sendMessageResult.getStatus().getCode());
						}
					}
				});
		 */
	}

	/**
	 * acceptPhoneCall
	 */
	public void acceptPhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "acceptPhoneCall");
			try {
				Class[] parameterTypes = new Class[2];
				parameterTypes[0] = BluetoothDevice.class;
				parameterTypes[1] = int.class;
				Method method = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("acceptCall", parameterTypes);
				method.invoke(mHeadsetClient, mDevice, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// disconnectHeadset();
		}
	}

	/**
	 * rejectPhoneCall
	 */
	public void rejectPhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "rejectPhoneCall");
			try {
				Class[] parameterTypes = new Class[1];
				parameterTypes[0] = BluetoothDevice.class;
				Method method = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("rejectCall", parameterTypes);
				method.invoke(mHeadsetClient, mDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		finish();
	}

	/**
	 * terminatePhoneCall
	 */
	public void terminatePhoneCall() {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "terminatePhoneCall");
			try {
				Class[] parameterTypes = new Class[2];
				parameterTypes[0] = BluetoothDevice.class;
				parameterTypes[1] = int.class;
				Method method = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("terminateCall", parameterTypes);
				method.invoke(mHeadsetClient, mDevice, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		finish();
	}

	/**
	 * dialPhoneCall
	 */
	public void dialPhoneCall(String phoneNumber) {
		if (mHeadsetClient != null && mDevice != null) {
			// connectHeadset();
			Log.d(TAG, "dialPhoneCall");
			try {
				Class[] parameterTypes = new Class[2];
				parameterTypes[0] = BluetoothDevice.class;
				parameterTypes[1] = String.class;
				Method tt = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("dial", parameterTypes);
				boolean flag = (Boolean) tt.invoke(mHeadsetClient, mDevice, phoneNumber);
				Log.d(TAG, "dialPhoneCall status : " + flag);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// disconnectHeadset();
		}
	}

	/**
	 * disconnectHeadset
	 */
	private void disconnectHeadset() {
		if (mDevice != null && mHeadsetClient != null) {
			Log.d(TAG, "disconnectHeadset");
			try {
				Class[] parameterTypes = new Class[1];
				parameterTypes[0] = BluetoothDevice.class;
				Method tt = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("disconnect", parameterTypes);
				tt.invoke(mHeadsetClient, mDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
			sendMessage(MOBILE_CONNECT_HS, null); // Notify the mobile phone to connect bluetooth headset
		}
	}

	/**
	 * connectHeadset
	 */
	private void connectHeadset() {
		if (mDevice != null && mHeadsetClient != null) {
			Log.d(TAG, "connectHeadset");
			sendMessage(MOBILE_DISCONNECT_HS, null); // Notify the phone disconnect bluetooth headset
			try {
				Class[] parameterTypes = new Class[1];
				parameterTypes[0] = BluetoothDevice.class;
				Method tt = Class.forName("android.bluetooth.BluetoothHeadsetClient").getMethod("connect", parameterTypes);
				tt.invoke(mHeadsetClient, mDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
