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
 * Created by yang_shoulai on 11/6/2015.
 */
public class RFCommServer extends Thread {

    private static final String TAG = "RFCommServer";

    private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

    private BluetoothServerSocket serverSocket;

    private ListeningCallback callback;

    private Context context;

    private boolean running = true;

    public RFCommServer(Context context, ListeningCallback callback) {
        setName("RFCommServer");
        this.callback = callback == null ? new SimpleListeningCallback() : callback;
        this.context = context;
    }

    interface ListeningCallback {
        void onStartSuccess();

        void onStartFailed();

        void onAccept(BluetoothSocket socket);

        void onAcceptError();

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
        public void onAcceptError() {

        }
    }


    @Override
    public void run() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("CloudWatchService", RFCOMM_UUID);
            callback.onStartSuccess();
            Log.e(TAG, "RFComm Server 启动成功!");
        } catch (IOException e) {
            Log.e(TAG, "RFComm Server启动异常!", e);
            callback.onStartFailed();
            stopListen();
            return;
        }
        while (running) {
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
                Log.e(TAG, "Caught Exception When Accept RFComm Client! Running = " + running, e);
                if (running) {
                    callback.onAcceptError();
                }
                stopListen();
            }
        }
    }


    public void stopListen() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocket = null;
    }


    public boolean isRunning() {
        return running;
    }
}
