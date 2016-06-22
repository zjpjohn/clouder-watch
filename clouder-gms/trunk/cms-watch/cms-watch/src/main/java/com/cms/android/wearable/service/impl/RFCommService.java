package com.cms.android.wearable.service.impl;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yang_shoulai on 12/1/2015.
 */
public class RFCommService extends Service {

    private static final String TAG = "RFCommService";

    private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

    public static final int STATE_CONNECTING = 1;

    public static final int STATE_CONNECTED = 2;

    public static final int STATE_DISCONNECTED = 3;

    public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

    public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

    public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

    public volatile int mState = STATE_DISCONNECTED;

    public volatile int mRemoteDeviceState = -1;

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private String mAddress;

    private IRfcommClientCallback mRfcommClientCallback;

    private ReentrantLock stateLock = new ReentrantLock();

    private RFCommServiceHandler rfCommServiceHandler;

    private Handler uiHandler = new Handler();

    private RFCommServiceHandler.IRFCommHandlerCallback callback = new RFCommServiceHandler.IRFCommHandlerCallback() {
        @Override
        public void onRFCommDisconnected(final boolean normalStop) {
            setConnectionState(STATE_DISCONNECTED);
            Log.d(TAG, "RFCommService Disconnected, normal stop ? " + normalStop);
            if (!normalStop) {
                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean reconnect = !normalStop && mRemoteDeviceState != RFCOMM_DISCONNECT_CAUSE_EXPIRED;
                        Log.d(TAG, "isStopNormal = " + normalStop + ", PhoneSideStatus = " + mRemoteDeviceState);
                        if (reconnect) {
                            Log.d(TAG, "需要重新连接");
                            onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_RESTART);
                            connectRemoteDevice(mAddress);
                        } else {
                            Log.d(TAG, "无需重新连接");
                            onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_STOP);
                        }
                    }
                }, 1000);
            }
        }


        @Override
        public void onDataReceived(final byte[] bytes) {
            uiHandler.post(new Runnable() {
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
            uiHandler.post(new Runnable() {
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
    };


    private void onPostRFCOMMSocketDisconnected(int cause) {
        if (mRfcommClientCallback != null) {
            try {
                mRfcommClientCallback.onRFCOMMSocketDisconnected(cause);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void onPostRFCOMMSocketConnected(BluetoothDevice device) {
        try {
            if (mRfcommClientCallback != null) {
                mRfcommClientCallback.onRFCOMMSocketConnected(device);
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        return new IRFCommService.Stub() {
            @Override
            public void registerCallback(IRfcommClientCallback callback) throws RemoteException {
                mRfcommClientCallback = callback;
            }

            @Override
            public void connect(String address) throws RemoteException {
                connectRemoteDevice(address);
            }

            @Override
            public void disconnect() throws RemoteException {
                disconnectRemoteDevice();
            }

            @Override
            public boolean write(byte[] bytes) throws RemoteException {
                return writeBytes(bytes);
            }

            @Override
            public boolean isConnected() throws RemoteException {
                return getConnectionState() == STATE_CONNECTED;

            }

            @Override
            public int getState() throws RemoteException {
                return getConnectionState();
            }

            @Override
            public void setTotalTransportSuccess(String uuid) throws RemoteException {
                if (TextUtils.isEmpty(uuid)) {
                    LogTool.e(TAG, "[setTotalTransportRsp] uuid null error");
                    return;
                }
                ResponseData responseData = new ResponseData(uuid, ResponseData.RESPONSE_STATUS_TOTAL_SUCCESS);
                LogTool.i(TAG, String.format("[setTotalTransportRsp] uuid  = %s.", uuid));
                Log.d(TAG, "setTotalTransportRsp result = " + writeBytes(ResponseParser.dataPack(responseData)));
            }

            @Override
            public void setRemoteDeviceState(int state) throws RemoteException {
                mRemoteDeviceState = state;
            }
        };
    }


    private void connectRemoteDevice(String address) {
        Lock lock = stateLock;
        Log.d(TAG, "lock 1 = " + lock);
        lock.lock();
        Log.d(TAG, "lock 2 = " + lock);
        try {
            if (TextUtils.isEmpty(address) || !address.equals(Utils.getShardBondAddress(this))) {
                return;
            }
            int state = getConnectionState();
            if (address.equals(mAddress) && state != STATE_DISCONNECTED) {
                Log.i(TAG, "RFCommService State = " + state + ", 无需重新建立连接");
            } else {
                disconnectRemoteDevice();
                setConnectionState(STATE_CONNECTING);
                mAddress = address;
                BluetoothDevice bluetoothDevice = getRemoteBluetoothDevice(mAddress);
                if (bluetoothDevice == null) {
                    Log.d(TAG, "无法获取远程蓝牙设备 address = " + address);
                    setConnectionState(STATE_DISCONNECTED);
                    return;
                }
                BluetoothSocket socket;
                try {
                    socket = bluetoothDevice.createRfcommSocketToServiceRecord(RFCOMM_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                    socket = null;
                    setConnectionState(STATE_DISCONNECTED);
                }
                Log.d(TAG, "socket == null ? " + (socket == null) + " lock = " + lock);
                if (socket != null) {
                    long begin = System.currentTimeMillis();
                    try {
                        Log.d(TAG, "UI Thread ? " + (Looper.myLooper() == Looper.getMainLooper()));
                        socket.connect();
                        long end = System.currentTimeMillis();
                        LogTool.e(TAG, "与远程蓝牙设备连接成功,花费时间" + (end - begin) + "毫秒");
                        setConnectionState(STATE_CONNECTED);
                        onPostRFCOMMSocketConnected(bluetoothDevice);
                        rfCommServiceHandler = new RFCommServiceHandler(socket, callback);
                        rfCommServiceHandler.start();
                    } catch (Throwable e) {
                        Log.e(TAG, "Catch Exception!", e);
                        setConnectionState(STATE_DISCONNECTED);
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "createRfcommSocketToServiceRecord() return null");
                    setConnectionState(STATE_DISCONNECTED);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "lock.unlock()");
            lock.unlock();
        }

    }

    private void disconnectRemoteDevice() {
        Lock lock = stateLock;
        lock.lock();
        try {
            if (rfCommServiceHandler != null && !rfCommServiceHandler.isStopped()) {
                rfCommServiceHandler.disconnect();
            }
        } finally {
            lock.unlock();
        }
    }


    private int getConnectionState() {
        Lock lock = stateLock;
        lock.lock();
        try {
            return mState;
        } finally {
            lock.unlock();
        }
    }

    private void setConnectionState(int state) {
        Lock lock = stateLock;
        lock.lock();
        try {
            mState = state;
        } finally {
            lock.unlock();
        }
    }


    private boolean writeBytes(byte[] bytes) {
        Lock lock = stateLock;
        lock.lock();
        try {
            if (rfCommServiceHandler == null || rfCommServiceHandler.isStopped() || getConnectionState() != STATE_CONNECTED) {
                return false;
            }
            return rfCommServiceHandler.writeBytes(bytes);
        } finally {
            lock.unlock();
        }
    }


    private BluetoothDevice getRemoteBluetoothDevice(String address) {
        LogTool.e(TAG, "远程设备蓝牙地址 = " + address);
        BluetoothDevice connectDevice = null;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices != null && !bondedDevices.isEmpty()) {
            for (BluetoothDevice device : bondedDevices) {
                if (device.getAddress().equalsIgnoreCase(address)) {
                    connectDevice = device;
                    Log.d(TAG, "远程手机设备在配对列表中已经存在！");
                    break;
                }
            }
        }
        if (connectDevice == null) {
            connectDevice = bluetoothAdapter.getRemoteDevice(address);
        }
        return connectDevice;
    }


}
