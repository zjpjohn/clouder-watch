package com.cms.android.wearable.service.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.cms.android.wearable.service.common.Utils;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by yang_shoulai on 12/1/2015.
 */
public class RFCommServiceServer extends Thread {

    private static final String TAG = "RFCommServer";

    private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

    private BluetoothServerSocket serverSocket;

    private ListeningCallback callback;

    private Context context;

    private volatile boolean stopped = false;

    public RFCommServiceServer(Context context, ListeningCallback callback) {
        setName("RFCommServer");
        this.callback = callback == null ? new SimpleListeningCallback() : callback;
        this.context = context;
    }

    interface ListeningCallback {
        void onStartSuccess();

        void onStartFailed();

        void onAccept(BluetoothSocket socket);

        void onStopped(boolean normalStop);

    }

    public static class SimpleListeningCallback implements ListeningCallback {
        @Override
        public void onStartSuccess() {

        }

        @Override
        public void onStartFailed() {

        }

        @Override
        public void onAccept(BluetoothSocket socket) {

        }

        @Override
        public void onStopped(boolean normalStop) {

        }
    }


    @Override
    public void run() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("CloudWatchService", RFCOMM_UUID);
            callback.onStartSuccess();
            Log.e(TAG, "RFCommServiceServer 启动成功!");
        } catch (IOException e) {
            Log.e(TAG, "RFCommServiceServer启动异常!", e);
            callback.onStartFailed();
            close();
            return;
        }
        while (!stopped) {
            try {
                BluetoothSocket socket = serverSocket.accept();
                String bondAddress = Utils.getShardBondAddress(context);
                String remoteAddress = socket.getRemoteDevice().getAddress();
                Log.e(TAG, "绑定的蓝牙设备地址 = 【" + bondAddress + "】");
                Log.e(TAG, "连接的设备蓝牙地址 = 【" + remoteAddress + "】");
                if (remoteAddress.equals(bondAddress)) {
                    callback.onAccept(socket);
                } else {
                    Log.w(TAG, "RFComm Server reject a connection as the remote device is not the bonded one!");
                    try {
                        socket.close();
                    } catch (IOException e) {

                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Caught Exception When Accept RFComm Client! Stopped ? " + stopped, e);
                close();
                if (!stopped) {
                    callback.onStopped(false);
                }
            }
        }
    }


    public synchronized void shutdown() {
        stopped = true;
        close();
        callback.onStopped(stopped);
    }

    private synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized boolean isStopped() {
        return stopped;
    }
}
