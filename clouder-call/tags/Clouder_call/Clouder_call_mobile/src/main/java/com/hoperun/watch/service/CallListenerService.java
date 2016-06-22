package com.hoperun.watch.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by xing_peng on 2015/7/17.
 */
public class CallListenerService extends Service implements MessageApi.MessageListener, NodeApi.NodeListener,
        ConnectionCallbacks, OnConnectionFailedListener {

    private static String TAG = "CallListenerService";

    private static String INCOMING_CALL =  "/call/incoming";
    private static String DIAL_CALL =  "/call/dial";
    private static String REJECT_CALL = "/call/reject";
    private static String ACCEPT_CALL = "/call/accept";

    private static String MOBILE_DISCONNECT_HS = "/call/disconnect";
    private static String MOBILE_CONNECT_HS = "/call/connect";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("action" + intent.getAction());

            // 拨打电话
            if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                Log.d(TAG, "call OUT:" + phoneNumber);
                sendMessage(DIAL_CALL, phoneNumber.getBytes());

            } else { //电话状态
                TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
            }

        }
    };

    private PhoneStateListener listener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    // 挂断电话
                    Log.d(TAG, "REJECT_CALL");
                    sendMessage(REJECT_CALL, null);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 接听电话
                    Log.d(TAG, "ACCEPT_CALL");
                    sendMessage(ACCEPT_CALL, null);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    // 来电
                    Log.d(TAG, "INCOMING_CALL phoneNumber : " + incomingNumber);
                    sendMessage(INCOMING_CALL, incomingNumber.getBytes());
                    break;
            }
        }
    };

    @Override
    public void onMessageReceived(MessageEvent paramMessageEvent) {
        if (paramMessageEvent.getPath().endsWith(MOBILE_DISCONNECT_HS)) {
            //TODO 断开耳机蓝牙
        } else if (paramMessageEvent.getPath().endsWith(MOBILE_CONNECT_HS)) {
            //TODO 重新连接耳机蓝牙
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection to Google API client has failed");
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
    }

    /**
     * 获取已连接设备节点的列表
     */
    private Collection<String> getNodes() {
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    /**
     * 发送消息通知
     */
    private void sendMessage(String path, byte[] bytes) {
        Log.d(TAG, "sendMessage, path + " + path + ", bytes" + bytes);
        Collection<String> nodes = getNodes();
        for (String node : nodes) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, node, path, bytes).await();
        }
    }

}
