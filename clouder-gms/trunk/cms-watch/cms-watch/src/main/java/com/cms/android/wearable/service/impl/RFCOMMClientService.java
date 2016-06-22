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
 * Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 * <p/>
 * ***************************************************************************
 */
package com.cms.android.wearable.service.impl;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.LogTool;

/**
 * ClassName: RFCOMMClientService
 *
 * @author xing_pengfei
 * @description RFCOMMClientService
 * @Date 2015-7-29
 */
public class RFCOMMClientService extends Service {

    private static final String TAG = "RFCOMMClientService";

    // Constants that indicate the current rfcomm connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    // now listening for incoming connections
    public static final int STATE_LISTEN = 1;
    // now initiating an outgoing connection
    public static final int STATE_CONNECTING = 2;
    // now connected to a remote device
    public static final int STATE_CONNECTED = 3;

    private int mState = STATE_NONE;

    public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

    public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

    public static final int RFCOMM_DISCONNECT_CAUSE_EXCEPTION = 2;

    public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

    public static final int RFCOMM_CONNECT_READY = 4;


    private int mPhoneSideStatus = -1;

    // Unique UUID for this application

    private RFCommConnector rfCommConnector;

    //private ConnectedThread mConnectedThread;

    private RFCommHandler rfCommHandler;

    private String mRfcommAddress;

    private IRfcommClientCallback mRfcommClientCallback;

    private Handler mainHandler = new Handler();

    private IRfcommClientService.Stub mStub = new IRfcommClientService.Stub() {

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void registerCallback(IRfcommClientCallback callback) throws RemoteException {
            mRfcommClientCallback = callback;
        }

        @Override
        public void start(String address) throws RemoteException {
            startThread(address);
        }

        @Override
        public void restart() throws RemoteException {
            startThread();
        }

        @Override
        public void stop() throws RemoteException {
            LogTool.d(TAG, "stop rfcomm thread.");
            stopRFCommConnection();
        }

        @Override
        public boolean write(byte[] bytes) throws RemoteException {
            return writeBytes(bytes);
        }

        @Override
        public boolean isConnected() throws RemoteException {
            return isServiceConnected();
        }

        @Override
        public void setStatus(int status) throws RemoteException {
            mPhoneSideStatus = status;
        }

        @Override
        public void setTotalTransportSuccess(String uuid) throws RemoteException {
            setTotalTransportRsp(uuid);
        }
    };

    private synchronized boolean isServiceConnected() {
        return mState == STATE_CONNECTED && rfCommHandler != null && !rfCommHandler.isStoped();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }


    private boolean writeBytes(byte[] out) {
        if (mState != STATE_CONNECTED || rfCommHandler == null) {
            return false;
        }
        return rfCommHandler.writeBytes(out);
    }

    private RFCommHandler.HandlerCallback rfcommHandlerCallback = new RFCommHandler.HandlerCallback() {
        @Override
        public void onDisconnect(final boolean isStopNormal) {
            setState(STATE_NONE);
            rfCommHandler.shutdown();
            LogTool.d(TAG, "检测到RFComm连接断开，1秒后检查是否需要重连");
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean reconnect = !isStopNormal && mPhoneSideStatus != RFCOMM_DISCONNECT_CAUSE_EXPIRED;
                    Log.d(TAG, "isStopNormal = " + isStopNormal + ", PhoneSideStatus = " + mPhoneSideStatus);
                    if (reconnect) {
                        Log.d(TAG, "需要重新连接");
                        onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_RESTART);
                        reconnect();
                    } else {
                        Log.d(TAG, "无需重新连接");
                        onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_STOP);
                    }
                }
            }, 1000);

        }

        @Override
        public void onDataReceived(final byte[] bytes) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRfcommClientCallback.onDataReceived(bytes);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void onDataSent(final byte[] bytes) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRfcommClientCallback.onDataSent(bytes);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void onConnected(BluetoothDevice device) {
            setState(STATE_CONNECTED);
            onPostRFCOMMSocketConnected(device);
        }
    };


    private RFCommConnector.ConnectionCallback rfcommConnectionCallback = new RFCommConnector.ConnectionCallback() {

        @Override
        public void onConnectSuccess(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "onConnectSuccess");
            if (rfCommHandler != null) {
                Log.d(TAG, "rfCommHandler.shutdown()");
                rfCommHandler.shutdown();
            }

            rfCommHandler = new RFCommHandler(device, socket, rfcommHandlerCallback);
            rfCommHandler.start();
        }

        @Override
        public void onConnectFailed(BluetoothDevice device) {
            setState(STATE_NONE);
            onConnectFailure(device);
        }
    };

    private synchronized void startThread(String address) {
        if (mState == STATE_CONNECTING) {
            Log.e(TAG, "RFComm State = " + mState + ",无需再次启动");
            return;
        }
        if (!TextUtils.isEmpty(address) && BleUtil.isBluetoothAddress(address)) {
            mRfcommAddress = address;
            stopRFCommConnection();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setState(STATE_CONNECTING);
            rfCommConnector = new RFCommConnector(address, rfcommConnectionCallback);
            rfCommConnector.start();
        } else {
            LogTool.e(TAG, "非法的蓝牙地址 " + address);
        }
    }

    private void startThread() {
        startThread(mRfcommAddress);
    }

    /**
     * Stop all threads include AcceptThread InQueueThread and ConnectedThread
     */
    public synchronized void stopRFCommConnection() {
        if (rfCommHandler != null) {
            rfCommHandler.shutdown();
        }
        if (rfCommConnector != null) {
            rfCommConnector.stopConnecting();
        }

    }

    private synchronized void reconnect() {
        LogTool.d(TAG, "RFCOMM连接异常,断开连接,尝试重新连接");
        LogTool.d(TAG, "休眠 200ms");
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startThread();
            }
        }, 200);
    }

    /**
     * 通知上层RFCOMM建立连接
     */
    private void onPostRFCOMMSocketConnected(BluetoothDevice device) {
        try {
            if (mRfcommClientCallback != null) {
                mRfcommClientCallback.onRFCOMMSocketConnected(device);
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 通知上层RFCOMM连接断开
     *
     * @param cause 断开原因
     */
    private void onPostRFCOMMSocketDisconnected(int cause) {
        if (mRfcommClientCallback != null) {
            try {
                mRfcommClientCallback.onRFCOMMSocketDisconnected(cause);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 通知上层RFCOMM连接失败
     */
    private void onConnectFailure(BluetoothDevice device) {
        try {
            if (mRfcommClientCallback != null) {
                mRfcommClientCallback.onConnectFailure(device);
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    private void setTotalTransportRsp(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            LogTool.e(TAG, "[setTotalTransportRsp] uuid null error");
            return;
        }

        ResponseData responseData = new ResponseData(uuid, ResponseData.RESPONSE_STATUS_TOTAL_SUCCESS);
        LogTool.i(TAG, String.format("[setTotalTransportRsp] uuid  = %s.", uuid));
        Log.d(TAG, "setTotalTransportRsp result = " + writeBytes(ResponseParser.dataPack(responseData)));
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        LogTool.d(TAG, "手机端：setState() RFCOMM连接状态 " + getStateText(mState) + " -> " + getStateText(state));
        mState = state;
    }

    private String getStateText(int state) {
        String stateText = "";
        switch (state) {
            case STATE_NONE:
                stateText = "NONE";
                break;
            case STATE_LISTEN:
                stateText = "LISTEN";
                break;
            case STATE_CONNECTING:
                stateText = "CONNECTING";
                break;
            case STATE_CONNECTED:
                stateText = "CONNECTED";
                break;

            default:
                break;
        }
        return stateText;
    }

}
