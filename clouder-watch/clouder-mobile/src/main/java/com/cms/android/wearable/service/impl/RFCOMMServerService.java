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

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.common.LogTool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RFCOMMServerService extends Service {

    private static final String TAG = "RFCOMMService";

    public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

    public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

    public static final int RFCOMM_DISCONNECT_CAUSE_EXCEPTION = 2;

    public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

    public static final int RFCOMM_CONNECT_READY = 4;

    private static final int MESSAGE_TYPE_TIMER_OCCUR = 1;

    private static final int MESSAGE_TIME_INITAL = 15;

    private static final int MESSAGE_TIME_INTERVAL = 15;

    private ScheduledFuture<?> future;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private RFCommServer rfCommServer;

    private RFCommServerHandler rfCommServerHandler;

    private String mBluetoothMacAddress;

    private IRfcommServerCallback mRfcommServerCallback;

    private ScheduledExecutorService mScheduledService = Executors.newSingleThreadScheduledExecutor();

    private long mExpiredTime = new Date().getTime() + 30 * 1000;

    // Constants that indicate the current rfcomm connection state
    // we're doing nothing
    public static final int STATE_NONE = 0;
    // now listening for incoming connections
    public static final int STATE_LISTEN = 1;
    // now initiating an outgoing connection
    public static final int STATE_CONNECTING = 2;
    // now connected to a remote device
    public static final int STATE_CONNECTED = 3;

    private int mState = STATE_NONE;

    private Handler uiHanlder = new Handler();

    @SuppressLint("HandlerLeak")
    private Handler mTimeHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_TYPE_TIMER_OCCUR:
                    if (isRFCOMMExpired()) {
                        LogTool.e(TAG, "RFComm连接超时, 关闭RFCOMM通道");
                        //onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_EXPIRED);

                        if (rfCommServerHandler != null) {
                            rfCommServerHandler.stopConnection();
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private IRfcommServerService.Stub mStub = new IRfcommServerService.Stub() {

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void stop() throws RemoteException {
            LogTool.d(TAG, "关闭RFComm连接线程");
            if (rfCommServer != null) {
                rfCommServer.stopListen();
            }
            if (rfCommServerHandler != null) {
                rfCommServerHandler.stopConnection();
            }
        }

        @Override
        public void start(boolean force) throws RemoteException {
            validateExpireTime();
            startThread(force);
        }


        @Override
        public void registerCallback(IRfcommServerCallback callback) throws RemoteException {
            mRfcommServerCallback = callback;
        }

        @Override
        public boolean isConnected() throws RemoteException {
            return isRFCommConnected();
        }

        @Override
        public boolean write(byte[] bytes) throws RemoteException {
            validateExpireTime();
            return writeBytes(bytes);
        }

        @Override
        public boolean isConnecting() throws RemoteException {
            return false;
        }

        @Override
        public void setTotalTransportSuccess(String uuid) throws RemoteException {
            setTotalTransportRsp(uuid);
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScheduledService.shutdownNow();
        if (rfCommServer != null) {
            rfCommServer.stopListen();
        }
        if (rfCommServerHandler != null) {
            rfCommServerHandler.stopConnection();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogTool.d(TAG, "MessageClientService onBind");
        return mStub;
    }


    private void reconnect() {
        LogTool.d(TAG, "手机端：RFCOMM连接异常,断开连接,尝试重新连接");
        try {
            LogTool.d(TAG, "手机端：休眠 1000ms");
            Thread.sleep(1000);
            LogTool.d(TAG, "手机端：尝试重新创建RFCOMM Server...");
            startThread(true);
        } catch (InterruptedException e) {
            LogTool.e(TAG, "手机端：reconnect InterruptedException", e);
        }
    }

    private void setTotalTransportRsp(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            LogTool.e(TAG, "[setTotalTransportRsp] uuid null error");
            return;
        }
        ResponseData responseData = new ResponseData(uuid, ResponseData.RESPONSE_STATUS_TOTAL_SUCCESS);
        LogTool.i(TAG, String.format("[setTotalTransportRsp] uuid  = %s.", uuid));
        writeBytes(ResponseParser.dataPack(responseData));
    }

    /**
     * 通知上层RFCOMM连接断开
     *
     * @param cause 断开原因
     */
    private void onPostRFCOMMSocketDisconnected(int cause) {
        if (mRfcommServerCallback != null) {
            try {
                mRfcommServerCallback.onRFCOMMSocketDisconnected(cause, mBluetoothMacAddress);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 通知上层RFCOMM建立连接
     */
    private void onPostRFCOMMSocketConnected() {
        try {
            if (mRfcommServerCallback != null) {
                mRfcommServerCallback.onRFCOMMSocketConnected();
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    private RFCommServerHandler.HandlerCallback handlerCallback = new RFCommServerHandler.HandlerCallback() {
        @Override
        public void onHandlerStart() {
            onPostRFCOMMSocketConnected();
            future = mScheduledService.scheduleAtFixedRate(new Runnable() {
                @SuppressLint("SimpleDateFormat")
                @Override
                public void run() {
                    LogTool.d(TAG, "当前计时任务触发,当前时间为" + sdf.format(new Date()));
                    mTimeHandler.sendEmptyMessage(MESSAGE_TYPE_TIMER_OCCUR);
                }
            }, MESSAGE_TIME_INITAL, MESSAGE_TIME_INTERVAL, TimeUnit.SECONDS);
        }

        @Override
        public void onSocketRead() {
            validateExpireTime();
        }

        @Override
        public void onDisconnect(boolean isStopNormal) {
            if (future != null) {
                future.cancel(false);
            }
            rfCommServerHandler.stopConnection();
            onPostRFCOMMSocketDisconnected(isStopNormal ? RFCOMM_DISCONNECT_CAUSE_EXPIRED : RFCOMM_DISCONNECT_CAUSE_EXCEPTION);
        }

        @Override
        public void onDataReceived(final byte[] bytes) {
            uiHanlder.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRfcommServerCallback.onDataReceived(bytes);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void onDataSent(final byte[] bytes) {
            uiHanlder.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRfcommServerCallback.onDataSent(bytes);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    };


    private RFCommServer.ListeningCallback listeningCallback = new RFCommServer.ListeningCallback() {
        @Override
        public void onStartSuccess() {
            Log.d(TAG, "RFComm Server等待客户端连接");
            try {
                mRfcommServerCallback.onRFCOMMSocketReady(BluetoothAdapter.getDefaultAdapter().getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStartFailed() {
            Log.e(TAG, "RFComm Server启动失败");
        }

        @Override
        public void onAccept(BluetoothSocket socket) {
            LogTool.e(TAG, "RFComm客户端已经连接");
            mState = STATE_CONNECTED;
            if (rfCommServerHandler != null) {
                rfCommServerHandler.stopConnection();
            }
            rfCommServerHandler = new RFCommServerHandler(socket, handlerCallback);
            rfCommServerHandler.start();
        }

        @Override
        public void onAcceptError() {
            LogTool.e(TAG, "RFComm客户端连接失败");
            //setState(STATE_NONE);
        }
    };


    private void startThread(boolean force) {
        LogTool.i(TAG, "创建线程,准备启动RFComm服务,FORCE = " + force);
        if (mState == STATE_CONNECTING) {
            LogTool.i(TAG, "当前正在等待客户端连接，无需重新启动");
            try {
                Log.d(TAG, "RFComm Server正常！");
                mRfcommServerCallback.onRFCOMMSocketReady(BluetoothAdapter.getDefaultAdapter().getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }
        mState = STATE_CONNECTING;
        if (force) {
            if (rfCommServerHandler != null) {
                rfCommServerHandler.stopConnection();
            }
            if (rfCommServer != null) {
                rfCommServer.stopListen();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rfCommServer = new RFCommServer(this, listeningCallback);
            rfCommServer.start();
        } else {
            if (rfCommServerHandler != null && !rfCommServerHandler.isStoped()) {
                Log.e(TAG, "RFComm 连接正常，无需再次启动");
                return;
            }
            if (rfCommServer != null && rfCommServer.isRunning()) {
                try {
                    Log.d(TAG, "RFComm Server正常！");
                    mRfcommServerCallback.onRFCOMMSocketReady(BluetoothAdapter.getDefaultAdapter().getAddress());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "RFComm Server未启动或者已经停止");
                if (rfCommServer != null) {
                    rfCommServer.stopListen();
                }
                rfCommServer = new RFCommServer(this, listeningCallback);
                rfCommServer.start();
            }
        }
    }


    private boolean writeBytes(byte[] out) {
        if (isRFCommConnected()) {
            return rfCommServerHandler.writeBytes(out);
        }
        return false;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
   /* private synchronized void setState(int state) {
        LogTool.d(TAG, "手机端：setState() RFCOMM连接状态 " + getStateText(mState) + " -> " + getStateText(state));
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
    }*/
    private boolean isRFCommConnected() {
        return rfCommServerHandler != null && !rfCommServerHandler.isStoped();
    }


    /**
     * 更新RFCOMM服务过期时间
     */
    private void validateExpireTime() {
        // 暂时使过期时间设长些
        mExpiredTime = new Date().getTime() + 60 * 5 * 1000;
    }

    /**
     * 判定RFCOMM是否过期
     *
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    private boolean isRFCOMMExpired() {
        return new Date().getTime() > mExpiredTime;
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
