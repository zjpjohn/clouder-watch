/**
 * **************************************************************************
 * <p/>
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2014 by HopeRun.  All rights reserved.
 * <p/>
 * ***************************************************************************
 */
package com.clouder.watch.mobile;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessageParser;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.PhoneNumberSyncMessage;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.mobile.widgets.CallStateDialog;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.Wearable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * ClassName: MessageListenerService
 *
 * @author xing_peng
 * @description
 * @Date 2015-7-17
 */
public class CallMessageListenerService extends Service implements OnConnectionFailedListener, ConnectionCallbacks,
        MessageApi.MessageListener, NodeApi.NodeListener {

    private static String TAG = "CallMessageListenerService";

    private MobvoiApiClient mobvoiApiClient;

    private ContentResolver resolver;

    private Handler mainHandler = new Handler();

    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_CALL_CHANGED = "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED";
    public static final String EXTRA_CALL = "android.bluetooth.headsetclient.extra.CALL";
    public static final int CALL_STATE_ACTIVE = 0;
    public static final int CALL_STATE_HELD = 1;
    public static final int CALL_STATE_DIALING = 2;
    public static final int CALL_STATE_ALERTING = 3;
    public static final int CALL_STATE_INCOMING = 4;
    public static final int CALL_STATE_WAITING = 5;
    public static final int CALL_STATE_HELD_BY_RESPONSE_AND_HOLD = 6;
    public static final int CALL_STATE_TERMINATED = 7;
    public static final int HEADSET_CLIENT = 16;
    private Class<?> mHeadsetClientClass;
    private Class<?> mHeadsetClientCallClass;

    private BluetoothAdapter mAdapter;

    private BluetoothProfile mHeadsetClient;

    private BluetoothDevice remoteDevice;

    private int HF_STATE = BluetoothProfile.STATE_DISCONNECTED;

    private CallStateDialog callStateDialog;

    private String remotePhoneNumber;//配对设备的手机电话号码

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "Check hf Connection State = " + HF_STATE);
            if (HF_STATE != BluetoothProfile.STATE_CONNECTED && HF_STATE != BluetoothProfile.STATE_CONNECTING) {
                disconnectHeadset();
                mAdapter.closeProfileProxy(HEADSET_CLIENT, mHeadsetClient);
                getHandSetClientProxyProfile(getNodeId());
            }

            uiHandler.sendMessageDelayed(Message.obtain(), 60000);

        }
    };


    @Override
    public void onCreate() {

        resolver = this.getContentResolver();
        remotePhoneNumber = Settings.System.getString(resolver, SettingsKey.PAIRED_DEVICE_PHONENUMBER);
        Log.d(TAG, "保存的手机号码 " + remotePhoneNumber);
        callStateDialog = new CallStateDialog(this, this);
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        //for hfp
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            stopSelf();
        }
        IntentFilter filters = new IntentFilter();
        filters.addAction(ACTION_CALL_CHANGED);
        filters.addAction(ACTION_CONNECTION_STATE_CHANGED);
        /*filters.addAction("android.bluetooth.headsetclient.profile.action.AG_EVENT");
        filters.addAction("android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED");*/
        registerReceiver(mReceiver, filters);
        try {
            mHeadsetClientClass = Class.forName("android.bluetooth.BluetoothHeadsetClient");
            mHeadsetClientCallClass = Class.forName("android.bluetooth.BluetoothHeadsetClientCall");
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "ClassNotFound BluetoothHeadsetClient, BluetoothHeadsetClientCall");
            e.printStackTrace();
        }
        uiHandler.sendMessageDelayed(Message.obtain(), 60000);

    }

    private void getHandSetClientProxyProfile(String address) {
        if (address != null && !"".equals(address.trim())) {
            remoteDevice = mAdapter.getRemoteDevice(address);
            if (remoteDevice != null) {
                mAdapter.getProfileProxy(this, listener, HEADSET_CLIENT);
            } else {
                Log.e(TAG, "Can not get device by address = " + address);
            }
        } else {
            Log.e(TAG, "Wrong bluetooth address = " + address);
        }
    }


    private BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "onServiceDisconnected profile = " + profile);
            HF_STATE = BluetoothProfile.STATE_DISCONNECTED;
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "onServiceConnected proxy =  " + proxy + " profile = " + profile);
            if (proxy != null && profile == HEADSET_CLIENT) {
                mHeadsetClient = proxy;
                Log.d(TAG, "开始连接Headset......");
                connectHeadset();
            } else {
                Log.d(TAG, "proxy == null or profile != HEADSET_CLIENT");
            }
            Log.d(TAG, "onServiceConnected mHeadsetClient != null " + (mHeadsetClient != null));
        }
    };

    private void connectHeadset() {
        if (remoteDevice == null) {
            Log.e(TAG, "can not connect Headset as remote device is null");
            HF_STATE = BluetoothProfile.STATE_DISCONNECTED;
            return;
        }
        Class[] parameterTypes = new Class[]{BluetoothDevice.class};
        try {
            Method connectMethod = mHeadsetClientClass.getMethod("connect", parameterTypes);
            Object object = connectMethod.invoke(mHeadsetClient, remoteDevice);
            Log.i(TAG, "connect operation success ? " + object);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            HF_STATE = BluetoothProfile.STATE_DISCONNECTED;
        }

    }


    private void disconnectHeadset() {
        Log.d(TAG, "disconnectHeadset");
        if (remoteDevice != null && mHeadsetClient != null) {
            try {
                Class[] parameterTypes = new Class[1];
                parameterTypes[0] = BluetoothDevice.class;
                Method method = mHeadsetClientClass.getMethod("disconnect", parameterTypes);
                Object object = method.invoke(mHeadsetClient, remoteDevice);

                Log.e(TAG, "disconnectHeadset return:" + object);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //sendMessage(MOBILE_CONNECT_HS, null); // Notify the mobile phone to connect bluetooth headset
        } else {
            Log.e(TAG, "mDevice or mHeadsetClient is null ");
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @SuppressWarnings("rawtypes")
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();
            Bundle bundle = arg1.getExtras();
            Log.d(TAG, "获取到的action：" + action);
            if (ACTION_CALL_CHANGED.equals(action)) {
                Log.d(TAG, "ACTION_CALL_CHANGED");
                Object mHeadsetClientCall = bundle.getParcelable(EXTRA_CALL);
                if (mHeadsetClientCall != null) {
                    try {
                        Method mGetState = mHeadsetClientCallClass.getMethod("getState");
                        int state = (Integer) mGetState.invoke(mHeadsetClientCall);
                        Log.d(TAG, "HeadsetClientCall State = " + state);
                        Method mGetNumber = mHeadsetClientCallClass.getMethod("getNumber");
                        String number = (String) mGetNumber.invoke(mHeadsetClientCall);
                        Log.d(TAG, "HeadsetClientCall phone number = " + number);
                        if (number == null || number.trim().length() == 0) {
                            return;
                        }
                        Log.d(TAG, "Paired device phone number = " + remotePhoneNumber);
                        if (trimPhoneNumber(number).equals(remotePhoneNumber)) {
                            Log.w(TAG, "phone number equals the remote paired device phone number!");
                            return;
                        }
                        switch (state) {
                            case CALL_STATE_ACTIVE:
                                Log.i(TAG, "电话接通，Phone Number = " + number);
                                if (isCallable()) {
                                    showCallActiveDialog(number);
                                }
                                sendCallStateChangeBroadcast(CALL_STATE_ACTIVE, number);
                                break;
                            case CALL_STATE_DIALING:
                                Log.i(TAG, "电话去电，Phone Number = " + number);
                                if (isCallable()) {
                                    showCallDialDialog(number);
                                }
                                sendCallStateChangeBroadcast(CALL_STATE_DIALING, number);
                                break;
                            case CALL_STATE_INCOMING:
                                Log.i(TAG, "电话来电，Phone Number = " + number);
                                if (isCallable()) {
                                    showInComingDialDialog(number);
                                }
                                sendCallStateChangeBroadcast(CALL_STATE_INCOMING, number);
                                break;
                            case CALL_STATE_TERMINATED:
                                Log.i(TAG, "电话挂断，Phone Number = " + number);
                                dismissAllDialogs();
                                sendCallStateChangeBroadcast(CALL_STATE_TERMINATED, number);
                                //插入通话记录
                                String name = queryContactNameByPhoneNumber(number);
                                setPhoneRecords(name, number);
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "mHeadsetClientCall is null");
                }
            } else if (ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int newState = bundle.getInt(BluetoothProfile.EXTRA_STATE, -1);
                Log.d(TAG, "HF Connection state : " + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    HF_STATE = BluetoothProfile.STATE_CONNECTED;
                } else {
                    HF_STATE = newState;
                }
            }
        }

    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectHeadset();
        if (mAdapter != null) {
            mAdapter.closeProfileProxy(HEADSET_CLIENT, mHeadsetClient);
            unregisterReceiver(mReceiver);
        }
        if (mobvoiApiClient != null) {
            Wearable.MessageApi.removeListener(mobvoiApiClient, this);
            Wearable.NodeApi.removeListener(mobvoiApiClient, this);
            mobvoiApiClient.disconnect();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand...........");
        if (!mobvoiApiClient.isConnected()) {
            mobvoiApiClient.connect();
        }
        return START_STICKY;
    }

    //寻找联系人姓名
    private String queryContactNameByPhoneNumber(String phoneNumber) {
        String contactName = phoneNumber;
        Cursor cursorOriginal = null;
        try {
            cursorOriginal = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + "='" + phoneNumber + "'", null, null);
            if (null != cursorOriginal && cursorOriginal.moveToFirst()) {
                contactName = cursorOriginal.getString(cursorOriginal
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursorOriginal != null) {
                cursorOriginal.close();
            }

        }
        return contactName;
    }


    private void setPhoneRecords(String name, String number) {
        Log.d(TAG, "save phone records : " + number);
        long time = new Date().getTime();
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.CACHED_NAME, name == null ? number : name);
        values.put(CallLog.Calls.NUMBER, number);
        values.put(CallLog.Calls.DATE, new Date().getTime());
        values.put(CallLog.Calls.TYPE, 0);// incoming:1,dial:2,not answer:3
        resolver.insert(CallLog.Calls.CONTENT_URI, values);

        Intent intent = new Intent();
        intent.setAction("com.clouder.watch.ACTION_CALL_RECORDS_CHANGE");
        intent.putExtra("name", name == null ? number : name);
        intent.putExtra("number", number);
        intent.putExtra("time", time);
        sendBroadcast(intent);
    }

    public boolean isCallable() {
        String nodeId = getNodeId();
        boolean airPlaneOff = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0;
        boolean isZenMode = inZenMode(this);
        return !isZenMode && airPlaneOff && nodeId != null;
    }


    private String getNodeId() {
        String nodeId = null;
        com.cms.android.wearable.NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            List<Node> nodes = result.getNodes();
            if (nodes != null && !nodes.isEmpty()) {
                nodeId = nodes.get(0).getId();
                Log.d(TAG, "获取到手机端的MAC：" + nodeId);
                Log.d(TAG, "获取到手机端的DisplayName：" + nodes.get(0).getDisplayName());
            }
        }

        return nodeId;
    }

    // Check whether the flight mode
    @SuppressLint("NewApi")
    private static boolean inZenMode(Context paramContext) {
        boolean bool = false;
        try {
            if (Settings.Global.getInt(paramContext.getContentResolver(), "in_zen_mode") != 0)
                bool = true;
        } catch (Settings.SettingNotFoundException localSettingNotFoundException) {
            Log.e(TAG, "Setting not found");
        }
        Log.d(TAG, "inZenMode : " + bool);
        return bool;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serverMessenger.getBinder();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "CMS ConnectionFailed " + connectionResult);
        if (connectionResult != null && connectionResult.getErrorCode() == 9) {
            reconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "CMS Connected");
        Wearable.MessageApi.addListener(mobvoiApiClient, this);
        Wearable.NodeApi.addListener(mobvoiApiClient, CallMessageListenerService.this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "add node listener success!");

                } else {
                    Log.e(TAG, "add node listener failed!");
                }
            }
        });
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            if (result.getStatus().isSuccess()) {
                List<Node> nodes = result.getNodes();
                if (nodes != null && !nodes.isEmpty()) {
                    for (Node node : nodes) {
                        Log.d(TAG, "Send message to node " + node.getId());
                        PhoneNumberSyncMessage message = new PhoneNumberSyncMessage();
                        message.setMethod(SyncMessage.Method.Get);
                        Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), message.getPath(), message.toBytes())
                                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                        if (sendMessageResult.getStatus().isSuccess()) {

                                        } else {
                                            Log.i(TAG, String.format("Send Message with  path [%s] failed!", "/getPhoneNumber"));

                                        }
                                    }
                                });
                    }
                }
            }
        }
        getHandSetClientProxyProfile(getNodeId());


    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e(TAG, "CMS ConnectionSuspended cause:" + cause);
        Log.e(TAG, "Mobvoi api client connection was suspended! mobvoi api client will try reconnect! ");
        reconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "We receive message path = " + messageEvent.getPath() + ", but we do nothing about is!");
        if (SyncMessagePathConfig.SYNC_PHONE_NUMBER.equals(messageEvent.getPath())) {
            Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
            byte[] data = messageEvent.getData();
            final SyncMessage syncMessage = SyncMessageParser.parse(SyncMessagePathConfig.SYNC_PHONE_NUMBER, data);
            if (syncMessage != null && syncMessage.getMethod() == SyncMessage.Method.Set) {
                PhoneNumberSyncMessage phoneNumberSyncMessage = (PhoneNumberSyncMessage) syncMessage;
                if (phoneNumberSyncMessage.getNumber() != null && phoneNumberSyncMessage.getNumber().length() > 0) {
                    remotePhoneNumber = trimPhoneNumber(phoneNumberSyncMessage.getNumber());
                    Log.d(TAG, "获取手机号码 " + remotePhoneNumber);
                    Settings.System.putString(resolver, SettingsKey.PAIRED_DEVICE_PHONENUMBER, remotePhoneNumber);
                }
            }
        }
    }

    private String trimPhoneNumber(String src) {
        if (src == null) {
            return null;
        }
        String s1 = src.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
        if (s1.startsWith("+86")) {
            s1 = s1.substring(3);
        }
        return s1;
    }

    /**
     * 重连
     */
    public void reconnect() {
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.e(TAG, "Mobvoi api client will try connect again " + 5
                + " seconds later! thread =" + Thread.currentThread().getName());

        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mobvoiApiClient.connect();
            }
        }, 5000);
    }

    public void dialPhoneCall(String phoneNumber) {
        Log.d(TAG, "dialPhoneCall");
        if (getNodeId() == null) {
            Toast.makeText(getApplicationContext(), R.string.no_bt, Toast.LENGTH_SHORT).show();
        }
        if (HF_STATE == BluetoothProfile.STATE_CONNECTED) {
            try {
                Class[] parameterTypes = new Class[2];
                parameterTypes[0] = BluetoothDevice.class;
                parameterTypes[1] = String.class;
                Method method = mHeadsetClientClass.getMethod("dial", parameterTypes);
                boolean flag = (Boolean) method.invoke(mHeadsetClient, remoteDevice, phoneNumber);
                Log.d(TAG, "dialPhoneCall status : " + flag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mDevice or mHeadsetClient is null ");
        }
    }


    public void terminatePhoneCall() {
        Log.d(TAG, "terminatePhoneCall");
        if (HF_STATE == BluetoothProfile.STATE_CONNECTED) {
            try {
                Class[] parameterTypes = new Class[2];
                parameterTypes[0] = BluetoothDevice.class;
                parameterTypes[1] = int.class;
                Method method = mHeadsetClientClass.getMethod("terminateCall", parameterTypes);
                boolean flag = (Boolean) method.invoke(mHeadsetClient, remoteDevice, 0);
                Log.d(TAG, "terminatePhoneCall status : " + flag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mDevice or mHeadsetClient is null ");
        }
    }

    public void acceptPhoneCall() {
        Log.d(TAG, "acceptPhoneCall");
        if (HF_STATE == BluetoothProfile.STATE_CONNECTED) {
            try {
                Class[] parameterTypes = new Class[2];
                parameterTypes[0] = BluetoothDevice.class;
                parameterTypes[1] = int.class;
                Method method = mHeadsetClientClass.getMethod("acceptCall", parameterTypes);
                boolean flag = (Boolean) method.invoke(mHeadsetClient, remoteDevice, 0);
                Log.d(TAG, "acceptPhoneCall status : " + flag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mDevice or mHeadsetClient is null ");
        }
    }

    public void rejectPhoneCall() {
        Log.d(TAG, "rejectPhoneCall");
        if (HF_STATE == BluetoothProfile.STATE_CONNECTED) {
            try {
                Class[] parameterTypes = new Class[1];
                parameterTypes[0] = BluetoothDevice.class;
                Method method = mHeadsetClientClass.getMethod("rejectCall", parameterTypes);
                boolean flag = (Boolean) method.invoke(mHeadsetClient, remoteDevice);
                Log.d(TAG, "rejectPhoneCall status : " + flag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mDevice or mHeadsetClient is null ");
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 3) {
                //打电话
                String phoneNumber = msg.getData().getString("phoneNumber");
                dialPhoneCall(phoneNumber);
            } else if (msg.what == 4) {
                //挂断
                terminatePhoneCall();
            } else if (msg.what == 5) {
                //接听
                acceptPhoneCall();
            } else if (msg.what == 6) {
                //拒接
                rejectPhoneCall();
            }

        }
    };
    private Messenger serverMessenger = new Messenger(handler);


    @Override
    public void onPeerConnected(Node node) {
        getHandSetClientProxyProfile(getNodeId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        disconnectHeadset();
        mAdapter.closeProfileProxy(HEADSET_CLIENT, mHeadsetClient);
    }


    private void sendCallStateChangeBroadcast(int state, String number) {
        Intent intent = new Intent();
        intent.setAction("com.clouder.watch.ACTION_CALL_STATE_CHANGE");
        intent.putExtra("extra_state", state);
        intent.putExtra("extra_number", number);
        sendBroadcast(intent);
    }


    private void showCallActiveDialog(String number) {
        if (callStateDialog.getState() == CallStateDialog.STATE_ACTIVE && callStateDialog.isShowing()) {
            return;
        }
        if (callStateDialog.getState() == CallStateDialog.STATE_INCOMING || callStateDialog.getState() == CallStateDialog.STATE_DIAL) {
            String name = queryContactNameByPhoneNumber(number);
            Bitmap head = getPhoto(number);
            callStateDialog.showActiveState(name, head);
        }
    }


    private void showCallDialDialog(String number) {
        if (callStateDialog.getState() == CallStateDialog.STATE_DIAL && callStateDialog.isShowing()) {
            return;
        }
        String name = queryContactNameByPhoneNumber(number);
        Bitmap head = getPhoto(number);
        callStateDialog.showDialState(name, head);
    }

    private void showInComingDialDialog(String number) {
        if (callStateDialog.getState() == CallStateDialog.STATE_INCOMING && callStateDialog.isShowing()) {
            return;
        }
        String name = queryContactNameByPhoneNumber(number);
        Bitmap head = getPhoto(number);
        callStateDialog.showIncomingState(name, head);
    }

    private void dismissAllDialogs() {
        if (callStateDialog != null && callStateDialog.isShowing()) {
            callStateDialog.dismiss();
        }
    }


    //获取联系人照片
    private Bitmap getPhoto(String phoneNumber) {
        Bitmap bitmap = null;
        resolver = getContentResolver();
        InputStream input = null;
        try {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, getContactId(phoneNumber));
            input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
            if (input != null) {
                bitmap = BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_head);
        }
        return bitmap;
    }

    //获取联系人ID
    private long getContactId(String phoneNumber) {
        resolver = getContentResolver();
        long contactId = 0;
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactId;
    }


}
