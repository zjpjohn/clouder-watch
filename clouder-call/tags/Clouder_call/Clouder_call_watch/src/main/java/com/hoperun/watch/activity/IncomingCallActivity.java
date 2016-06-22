package com.hoperun.watch.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.ConnectionApi;
import com.hoperun.watch.R;
import com.hoperun.watch.RoundImageView;
import com.hoperun.watch.service.MessageListenerService;

import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xing_peng on 2015/7/14.
 */
public class IncomingCallActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, NodeApi.NodeListener, OnClickListener {

    private static String TAG = "IncomingCallActivity";

    private static String MOBILE_DISCONNECT_HS = "/call/disconnect";
    private static String MOBILE_CONNECT_HS = "/call/connect";

    private GoogleApiClient mGoogleApiClient;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadsetClient mBluetoothHeadset;
    private BluetoothDevice mPhoneDevice;

    private String mPhoneAddress;
    private String mName;
    private String mNode;
    private String mPhoneNumber;

    private Context mContext;
    private ImageButton btnAccept, btnTerminate, btnReject;
    private View callView, acceptView;
    private RoundImageView head, accHead;
    private Chronometer timer;
    private ContentResolver resolver;
    private Long callTime;
    private int callType = 3;

    private int[] images = { 0, R.mipmap.bg_call_light_1, R.mipmap.bg_call_light_2, R.mipmap.bg_call_light_3 };
    private int SIGN = 0, num = 0;

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "on service connected");
            if (profile == BluetoothProfile.HEADSET_CLIENT) {
                mBluetoothHeadset = (BluetoothHeadsetClient) proxy;
            }
        }

        public void onServiceDisconnected(int paramInt) {
            Log.e(TAG, "on service disconnected");
            if (paramInt == BluetoothProfile.HEADSET_CLIENT) {
                mBluetoothHeadset = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.incoming_call);
        mContext = this;
        initViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Intent localIntent = getIntent();
        mPhoneNumber = localIntent.getStringExtra("number");
        mName = localIntent.getStringExtra("name");
        mNode = localIntent.getStringExtra("node");

        String command = localIntent.getStringExtra("command");
        String isDial = localIntent.getStringExtra("isDial");
        if (command.equals("dialCall")) {
            callingPage(); // 通话页面
            if (isDial.equals("watchDial")) {
                dialPhoneCall(mPhoneNumber); // 手表拨打电话
            }
            callType = 2;
        } else {
            incomingPage(); // 来电页面
        }

        // 头像动画
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
                    if (callType == 2)
                        callType = 1;
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

    private void initViews() {

        head = (RoundImageView) findViewById(R.id.head);
        accHead = (RoundImageView) findViewById(R.id.accHead);

        callView = findViewById(R.id.Top);
        acceptView = findViewById(R.id.acceptCall);

        btnAccept = (ImageButton) findViewById(R.id.callAccept);
        btnTerminate = (ImageButton) findViewById(R.id.callTerminate);
        btnReject = (ImageButton) findViewById(R.id.callReject);
        btnAccept.setOnClickListener(this);
        btnTerminate.setOnClickListener(this);
        btnReject.setOnClickListener(this);

        timer = (Chronometer) this.findViewById(R.id.chronometer);

        callTime = new Date().getTime();
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
                acceptPhoneCall();
                callingPage();
                acceptPage();
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                callType = 1;
                break;
            case R.id.callTerminate:
                terminatePhoneCall();
                break;
            case R.id.callReject:
                rejectPhoneCall();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        disconnectHeadset();
        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, mBluetoothHeadset);
    }

    @Override
    public void onConnected(Bundle paramBundle) {
        Log.d(TAG, "on connected");
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        Wearable.ConnectionApi.getConfig(this.mGoogleApiClient).setResultCallback(new ResultCallback() {
            @Override
            public void onResult(ConnectionApi.GetConfigResult paramGetConfigResult) {
                if (paramGetConfigResult.getStatus().isSuccess()) {
                    mPhoneAddress = paramGetConfigResult.getConfig().getAddress();
                    Log.d(TAG, "PhoneAddress : " + mPhoneAddress);

                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter != null) {
                        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET_CLIENT);
                    }
                }
            }
        });
    }

    @Override
    public void onPeerConnected(Node paramNode) {
        Log.d(TAG, "onPeerConnected ");
    }

    @Override
    public void onPeerDisconnected(Node paramNode) {
        Log.d(TAG, "onPeerDisconnected ");
        finish();
    }

    @Override
    public void onConnectionSuspended(int paramInt) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    /**
     * 连接设备
     */
    private void checkDeviceIsConnected() {
        if (mPhoneDevice == null) {
            boolean flag = false;

            Iterator connectedIterator = this.mBluetoothHeadset.getConnectedDevices().iterator();
            while (connectedIterator.hasNext()) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) connectedIterator.next();
                if (mPhoneAddress.equals(bluetoothDevice.getAddress())) {
                    mPhoneDevice = bluetoothDevice;
                    flag = true;
                }
            }

            if (!flag) {
                Iterator bondedIterator = this.mBluetoothAdapter.getBondedDevices().iterator();
                while (bondedIterator.hasNext()) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) bondedIterator.next();
                    if (mPhoneAddress.equals(bluetoothDevice.getAddress())) {
                        mPhoneDevice = bluetoothDevice;
                    }
                }
            }
        }
        connectHeadset();
    }

    /**
     * 接听来电
     */
    public void acceptPhoneCall() {
        if (mBluetoothHeadset != null && mPhoneDevice != null) {
            Log.d(TAG, "acceptPhoneCall");
            checkDeviceIsConnected();
            mBluetoothHeadset.acceptCall(mPhoneDevice, BluetoothHeadsetClient.CALL_ACCEPT_NONE);
            disconnectHeadset();
        }
    }

    /**
     * 拒接来电
     */
    public void rejectPhoneCall() {
        if (mBluetoothHeadset != null && mPhoneDevice != null) {
            Log.d(TAG, "rejectPhoneCall");
            checkDeviceIsConnected();
            mBluetoothHeadset.rejectCall(mPhoneDevice);
        }
        finish();
        setPhoneRecords();
    }

    /**
     * 挂断电话
     */
    public void terminatePhoneCall() {
        if (mBluetoothHeadset != null && mPhoneDevice != null) {
            Log.d(TAG, "terminatePhoneCall");
            checkDeviceIsConnected();
            mBluetoothHeadset.terminateCall(mPhoneDevice, 0);
        }
        finish();
        setPhoneRecords();
    }

    /**
     * 拨打电话
     */
    public void dialPhoneCall(String phoneNumber) {
        if (mBluetoothHeadset != null && mPhoneDevice != null) {
            Log.d(TAG, "dialPhoneCall");
            checkDeviceIsConnected();
            mBluetoothHeadset.dial(mPhoneDevice, phoneNumber);
            disconnectHeadset();
        }
    }

    /**
     * 断开连接
     */
    private void disconnectHeadset()
    {
        if (mPhoneDevice != null && mBluetoothHeadset != null) {
            Log.d(TAG, "disconnectHeadset");
            mBluetoothHeadset.disconnect(mPhoneDevice);
            sendMessage(MOBILE_CONNECT_HS, null); // 通知手机重新连接蓝牙耳机
        }
    }

    /**
     * 建立连接
     */
    private void connectHeadset() {
        if (mPhoneDevice != null && mBluetoothHeadset != null) {
            Log.d(TAG, "connectHeadset");
            sendMessage(MOBILE_DISCONNECT_HS, null); // 通知手机断开蓝牙耳机
            mBluetoothHeadset.connect(mPhoneDevice);
        }
    }

    /**
     * 发送消息通知
     */
    private void sendMessage(String path, byte[] bytes) {
        Log.d(TAG, "sendMessage, path + " + path + ", bytes" + bytes);
        Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode, path, bytes).await();
    }

    /**
     * 保存通话记录
     */
    private void setPhoneRecords() {
        resolver = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.CACHED_NAME, mName);
        values.put(CallLog.Calls.NUMBER, mPhoneNumber);
        values.put(CallLog.Calls.DATE, callTime);
        values.put(CallLog.Calls.TYPE, callType);
        resolver.insert(CallLog.Calls.CONTENT_URI, values);
    }
}