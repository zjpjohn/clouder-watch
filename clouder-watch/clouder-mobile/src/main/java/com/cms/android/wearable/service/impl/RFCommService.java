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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yang_shoulai on 12/1/2015.
 */
public class RFCommService extends Service {

    private static final String TAG = "RFCommService";

    public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

    public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

    public static final int RFCOMM_DISCONNECT_CAUSE_EXCEPTION = 2;

    public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

    public static final int RFCOMM_CONNECT_READY = 4;

    private static final int MESSAGE_TYPE_TIMER_OCCUR = 1;

    private static final int MESSAGE_TIME_INITAL = 15;

    private static final int MESSAGE_TIME_INTERVAL = 15;

    private ScheduledFuture<?> future;

    private long mExpiredTime = new Date().getTime() + 30 * 1000;

    private ScheduledExecutorService mScheduledService = Executors.newSingleThreadScheduledExecutor();

    private IRfcommServerCallback mRfcommServerCallback;

    private static final int STATE_SERVER_STARTING = 5;

    private static final int STATE_SERVER_WAITING = 1;

    private static final int STATE_SERVER_STOPPED = 4;

    private static final int STATE_CONNECTED = 2;

    private static final int STATE_DISCONNECTED = 3;

    private volatile int mServerState = STATE_SERVER_STOPPED;

    private volatile int mState = STATE_DISCONNECTED;

    private String mAddress = "";

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_TYPE_TIMER_OCCUR:
                    if (isRFCOMMExpired()) {
                        LogTool.e(TAG, "RFComm连接超时, 关闭RFCOMM通道");
                        stopHandlerIfAvaliable();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private ReentrantLock stateLock = new ReentrantLock();

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private RFCommServiceServer rfCommServiceServer;

    private RFCommServiceHandler rfCommServiceHandler;

    private RFCommServiceServer.ListeningCallback listeningCallback = new RFCommServiceServer.ListeningCallback() {
        @Override
        public void onStartSuccess() {
            Log.d(TAG, "RFCommServiceServer启动成功！");
            mServerState = STATE_SERVER_WAITING;
            onRFCommServerReady();
        }

        @Override
        public void onStartFailed() {
            Log.e(TAG, "RFCommServiceServer启动失败！");
            mServerState = STATE_SERVER_STOPPED;
        }

        @Override
        public void onAccept(BluetoothSocket socket) {
            Log.d(TAG, "客户端连接成功");
            setConnectionState(STATE_CONNECTED);
            stopHandlerIfAvaliable();
            rfCommServiceHandler = new RFCommServiceHandler(socket, handlerCallback);
            rfCommServiceHandler.start();
        }

        @Override
        public void onStopped(boolean normalStop) {
            Log.e(TAG, "RFCommServiceServer启动停止！Normal Stop ? " + normalStop);
            mServerState = STATE_SERVER_STOPPED;
            if (!normalStop) {
                startup();
            }
        }
    };


    private RFCommServiceHandler.HandlerCallback handlerCallback = new RFCommServiceHandler.HandlerCallback() {
        @Override
        public void onHandlerStart() {
            setConnectionState(STATE_CONNECTED);
            onPostRFCOMMSocketConnected();
            future = mScheduledService.scheduleAtFixedRate(new Runnable() {
                @SuppressLint("SimpleDateFormat")
                @Override
                public void run() {
                    LogTool.d(TAG, "当前计时任务触发,当前时间为" + sdf.format(new Date()));
                    uiHandler.sendEmptyMessage(MESSAGE_TYPE_TIMER_OCCUR);
                }
            }, MESSAGE_TIME_INITAL, MESSAGE_TIME_INTERVAL, TimeUnit.SECONDS);
        }

        @Override
        public void onSocketRead() {
            validateExpireTime();
        }

        @Override
        public void onDisconnect(boolean isStopNormal) {
            setConnectionState(STATE_DISCONNECTED);
            if (future != null) {
                future.cancel(false);
            }
            onPostRFCOMMSocketDisconnected(isStopNormal ? RFCOMM_DISCONNECT_CAUSE_EXPIRED : RFCOMM_DISCONNECT_CAUSE_EXCEPTION);
        }

        @Override
        public void onDataReceived(final byte[] bytes) {
            uiHandler.post(new Runnable() {
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
            uiHandler.post(new Runnable() {
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


    @Override
    public void onCreate() {
        super.onCreate();
        mAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mScheduledService.shutdownNow();
        shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return new IRFCommService.Stub() {
            @Override
            public void registerCallback(IRfcommServerCallback callback) throws RemoteException {
                mRfcommServerCallback = callback;
            }

            @Override
            public boolean write(byte[] bytes) throws RemoteException {
                validateExpireTime();
                return writeBytes(bytes);
            }

            @Override
            public void start() throws RemoteException {
                validateExpireTime();
                startup();
            }

            @Override
            public void stop() throws RemoteException {
                shutdown();
            }

            @Override
            public boolean isConnected() throws RemoteException {

                return getConnectionState() == STATE_CONNECTED;
            }

            @Override
            public void setTotalTransportSuccess(String uuid) throws RemoteException {
                setTotalTransportRsp(uuid);
            }
        };
    }

    private void onPostRFCOMMSocketConnected() {
        try {
            if (mRfcommServerCallback != null) {
                mRfcommServerCallback.onRFCOMMSocketConnected();
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

    private void validateExpireTime() {
        // 暂时使过期时间设长些
        mExpiredTime = new Date().getTime() + 60 * 5 * 1000;
    }

    private boolean isRFCOMMExpired() {
        return new Date().getTime() > mExpiredTime;
    }

    private void onPostRFCOMMSocketDisconnected(int cause) {
        if (mRfcommServerCallback != null) {
            try {
                mRfcommServerCallback.onRFCOMMSocketDisconnected(cause, mAddress);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }


    private int getConnectionState() {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            return mState;
        } finally {
            lock.unlock();
        }
    }


    private void setConnectionState(int state) {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            mState = state;
        } finally {
            lock.unlock();
        }
    }

    private void startup() {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            int state = getConnectionState();
            if (state == STATE_CONNECTED) {
                Log.d(TAG, "RFCommService State = " + state + ", 已经连接，无需重新启动");
                return;
            }
            if (mServerState == STATE_SERVER_WAITING) {
                Log.d(TAG, "RFCommServer处于等待状态，通知客户端连接");
                onRFCommServerReady();
                return;
            }
            if (mServerState == STATE_SERVER_STARTING) {
                Log.d(TAG, "RFCommService State = " + state + ", 正在启动， 无需重新启动");
                return;
            }
            shutdown();
            mServerState = STATE_SERVER_STARTING;
            rfCommServiceServer = new RFCommServiceServer(this, listeningCallback);
            rfCommServiceServer.start();
        } finally {
            lock.unlock();
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


    private void shutdown() {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            if (rfCommServiceServer != null && !rfCommServiceServer.isStopped()) {
                rfCommServiceServer.shutdown();
            }
            stopHandlerIfAvaliable();
        } finally {
            lock.unlock();
        }
    }

    private boolean writeBytes(byte[] bytes) {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            if (rfCommServiceHandler == null || rfCommServiceHandler.isStopped()) {
                return false;
            }
            return rfCommServiceHandler.writeBytes(bytes);
        } finally {
            lock.unlock();
        }
    }


    private void stopHandlerIfAvaliable() {
        Lock lock = this.stateLock;
        try {
            lock.lock();
            if (rfCommServiceHandler != null && !rfCommServiceHandler.isStopped()) {
                rfCommServiceHandler.disconnect();
            }
        } finally {
            lock.unlock();
        }
    }

    private void onRFCommServerReady() {
        if (mRfcommServerCallback != null) {
            try {
                mRfcommServerCallback.onRFCOMMSocketReady(BluetoothAdapter.getDefaultAdapter().getAddress());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

}
