package com.cms.android.wearable.service.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import com.cms.android.wearable.service.MessageServerApplication;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created by yang_shoulai on 11/5/2015.
 */
public class RFCommConnector extends Thread {

    private static final String TAG = "RFCommConnector";

    private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

    public static final int CONNECTION_STATE_IDAL = 0;

    public static final int CONNECTION_STATE_SUCCESS = 1;

    public static final int CONNECTION_STATE_CONNECTING = 2;

    public static final int CONNECTION_STATE_FAILED = 3;

    private int times = 1;

    private int state = CONNECTION_STATE_IDAL;

    private BluetoothDevice connectDevice;

    private ConnectionCallback callback;

    private boolean stop = false;

    private BluetoothSocket socket = null;

    public RFCommConnector(String address, ConnectionCallback callback) {
        Log.d(TAG, "RFCommConnector Init");
        setName("RFCommConnector");
        this.callback = callback == null ? new SimpleConnectionCallback() : callback;
        LogTool.e(TAG, "远程设备蓝牙地址 = " + address);
        Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        LogTool.d(TAG, "检测远程设备是否在已经配对的蓝牙列表中");
        if (bondedDevices != null && !bondedDevices.isEmpty()) {
            for (BluetoothDevice device : bondedDevices) {
                Log.d(TAG, String.format("已经配对的蓝牙设备【%s, %s】", device.getName(), device.getAddress()));
            }
            for (BluetoothDevice device : bondedDevices) {
                if (device.getAddress().equalsIgnoreCase(address)) {
                    connectDevice = device;
                    Log.d(TAG, "远程手机设备在配对列表中已经存在！");
                    break;
                }
            }
        }
        if (connectDevice == null) {
            LogTool.d(TAG, "未发现该设备中存在蓝牙配对列表！");
            this.connectDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        }
    }


    interface ConnectionCallback {

        void onConnectSuccess(BluetoothSocket socket, BluetoothDevice device);

        void onConnectFailed(BluetoothDevice device);

    }

    class SimpleConnectionCallback implements ConnectionCallback {

        @Override
        public void onConnectSuccess(BluetoothSocket socket, BluetoothDevice device) {

        }

        @Override
        public void onConnectFailed(BluetoothDevice device) {

        }
    }

    public int getConnectionState() {

        return this.state;
    }


    @Override
    public void run() {
        LogTool.e(TAG, "RFCommConnector 开始尝试连接远程设备! Time = " + System.currentTimeMillis());
        this.state = CONNECTION_STATE_CONNECTING;
        if (this.connectDevice != null) {
            BluetoothSocket socket = null;
            boolean success = false;
            while (!stop && times > 0) {
                times--;
                if (!connectDevice.getAddress().equals(Utils.getShardBondAddress(MessageServerApplication.getInstance()))) {
                    Log.e(TAG, "地址不匹配，连接退出！！");
                    return;
                }
                Log.d(TAG, String.format("Thread[%s], RFCommConnector 正在尝试第【%s】次与设备【%s, %s】连接", Thread.currentThread().getId(), 5 - times, this.connectDevice.getName(), this.connectDevice.getAddress()));
                int sdk = Build.VERSION.SDK_INT;
                try {
                    if (sdk >= 10) {
                        socket = this.connectDevice.createInsecureRfcommSocketToServiceRecord(RFCOMM_UUID);
                    } else {
                        socket = this.connectDevice.createRfcommSocketToServiceRecord(RFCOMM_UUID);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "与远程蓝牙设备创建Socket通道异常", e);
                    socket = null;
                }
                if (socket != null) {
                    try {
                        Thread.sleep(1000);
                        long begin = System.currentTimeMillis();
                        Log.d(TAG, "********" + times);
                        socket.connect();
                        long endTime = new Date().getTime();
                        LogTool.e(TAG, "与远程蓝牙设备连接成功,花费时间" + (endTime - begin) + "毫秒");
                        success = true;
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "与远程蓝牙设备Socket通道连接异常,Thread = " + Thread.currentThread().getId(), e);
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (success) {
                state = CONNECTION_STATE_SUCCESS;
                callback.onConnectSuccess(socket, this.connectDevice);
            } else {
                state = CONNECTION_STATE_FAILED;
                callback.onConnectFailed(this.connectDevice);
            }

        } else {
            Log.e(TAG, "无法获取远程蓝牙设备，连接失败");
            this.state = CONNECTION_STATE_FAILED;
            callback.onConnectFailed(this.connectDevice);
        }
    }


    public synchronized void stopConnecting() {
        stop = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
