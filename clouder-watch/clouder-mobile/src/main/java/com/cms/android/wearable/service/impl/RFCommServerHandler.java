package com.cms.android.wearable.service.impl;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.codec.StringUtil;
import com.cms.android.wearable.service.codec.TransportParser;
import com.cms.android.wearable.service.common.LogTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yang_shoulai on 11/6/2015.
 */
public class RFCommServerHandler extends Thread {

    private static final String TAG = "RFCommServerHandler";

    private BluetoothSocket socket;

    private InputStream inputStream;

    private OutputStream outputStream;

    private HandlerCallback callback;

    private boolean stop = false;

    private static long lastWriterTime = -1;
    private static boolean hasWriter = false;
    private Timer timer = new Timer();

    public RFCommServerHandler(BluetoothSocket socket, HandlerCallback callback) {
        LogTool.e(TAG, "RFCommServerHandler Init");
        setName("RFCommServerHandler");
        this.socket = socket;
        this.callback = callback == null ? new SimpleHandlerCallback() : callback;
        if (socket != null && socket.isConnected()) {
            try {
                this.inputStream = socket.getInputStream();
            } catch (IOException e) {
                LogTool.e(TAG, "RFCommHandler can not get [inputstream] from socket!", e);
            }
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                LogTool.e(TAG, "RFCommHandler can not get [outoutstream] from socket!", e);
            }
        }
        TimerTask timeTask = new TimerTask() {
            @Override
            public void run() {
                if (hasWriter) {
                    if (new Date().getTime() > lastWriterTime + 10000) {
                        Log.e(TAG, "检测到socket可能阻塞，关闭socket");
                        close();
                    }
                }

            }
        };
        timer.schedule(timeTask, 5000, 5000);
    }

    interface HandlerCallback {
        void onHandlerStart();

        void onSocketRead();

        void onDisconnect(boolean isStopNormal);

        void onDataReceived(byte[] bytes);

        void onDataSent(byte[] bytes);
    }

    public static class SimpleHandlerCallback implements HandlerCallback {

        @Override
        public void onHandlerStart() {

        }

        @Override
        public void onSocketRead() {

        }

        @Override
        public void onDisconnect(boolean isStopNormal) {

        }

        @Override
        public void onDataReceived(byte[] bytes) {

        }

        @Override
        public void onDataSent(byte[] bytes) {

        }

    }

    public enum LinkDecoderState {
        ReadA, ReadBCDEFG
    }

    @Override
    public void run() {
        if (inputStream == null || outputStream == null) {
            LogTool.e(TAG, "ConnectThread Bad rfcomm socket as can not get inputstream or outputstream!");
            callback.onDisconnect(stop);
            close();
            return;
        }
        callback.onHandlerStart();
        while (!stop) {
            LinkDecoderState state = LinkDecoderState.ReadA;
            byte[] ABCDEFG = null;
            try {
                switch (state) {
                    case ReadA:
                        int readLength = inputStream.read();
                        callback.onSocketRead();
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        byte A1 = (byte) readLength;
                        if (A1 != TransportParser.KEY_REQUEST_START) {
                            LogTool.e(TAG, String.format("The package is invaild start 0 (%s)", StringUtil.getHexString(A1)));
                            continue;
                        }
                        readLength = inputStream.read();
                        callback.onSocketRead();
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        byte A2 = (byte) readLength;
                        if (A2 != TransportParser.KEY_REQUEST_START) {
                            LogTool.e(TAG, String.format("The package is invaild start 1 (%s)", StringUtil.getHexString(A2)));
                            continue;
                        }
                        state = LinkDecoderState.ReadBCDEFG;
                    case ReadBCDEFG:
                        byte[] B = new byte[4];
                        readLength = inputStream.read(B);
                        callback.onSocketRead();
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        int l0 = B[0] < 0 ? B[0] + 256 : B[0];
                        int l1 = B[1] < 0 ? B[1] + 256 : B[1];
                        int l2 = B[2] < 0 ? B[2] + 256 : B[2];
                        int l3 = B[3] < 0 ? B[3] + 256 : B[3];

                        int packageLength = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;
                        LogTool.d(TAG, "The package all length = " + String.valueOf(packageLength));
                        ABCDEFG = new byte[packageLength];
                        ABCDEFG[0] = TransportParser.KEY_REQUEST_START;
                        ABCDEFG[1] = TransportParser.KEY_REQUEST_START;
                        ABCDEFG[2] = B[0];
                        ABCDEFG[3] = B[1];
                        ABCDEFG[4] = B[2];
                        ABCDEFG[5] = B[3];
                        // read C D E F G
                        byte[] CDEFG = new byte[packageLength - 6];
                        int readIndex = 0;
                        while (readIndex < packageLength - 6) {
                            readLength = inputStream.read(CDEFG);
                            callback.onSocketRead();
                            LogTool.d(TAG, "The byte length from io = " + readLength);
                            if (readLength == -1) {
                                throw new IOException("Connection has been closed!");
                            }
                            if (readLength <= packageLength - 6 - readIndex) {
                                System.arraycopy(CDEFG, 0, ABCDEFG, 6 + readIndex, readLength);
                            } else {
                                System.arraycopy(CDEFG, 0, ABCDEFG, 6 + readIndex, packageLength - 6 - readIndex);
                            }
                            readIndex += readLength;
                            CDEFG = new byte[packageLength - 6 - readIndex];
                        }

                        if (ABCDEFG[ABCDEFG.length - 2] == TransportParser.KEY_END_0 && ABCDEFG[ABCDEFG.length - 1] == TransportParser.KEY_END_1) {
                            state = LinkDecoderState.ReadA;
                            if (TransportParser.checkDataPack(ABCDEFG)) {
                                byte type = ABCDEFG[6];
                                switch (type) {
                                    case TransportParser.REQUEST_PARSER_TYPE:
                                        LogTool.d(TAG, "type-> REQUEST");
                                        setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_SUCCESS);
                                        callback.onDataReceived(ABCDEFG);
                                        break;
                                    case TransportParser.RESPONSE_PARSER_TYPE:
                                        LogTool.d(TAG, "type-> RESPONSE");
                                        callback.onDataSent(ABCDEFG);
                                        break;

                                    default:
                                        LogTool.e(TAG, "Unknown type");
                                        break;
                                }

                            } else {
                                // 若是外层包不符合要求，则获取其UUID发送失败标志位
                                LogTool.e(
                                        TAG,
                                        String.format("The package is invaild crc. (%s)",
                                                StringUtil.getHexString(ABCDEFG)));
                                byte type = ABCDEFG[6];
                                if (type == TransportParser.REQUEST_PARSER_TYPE) {
                                    // 答复请求没有缓存,返回错误码没什么意义
                                    LogTool.d(TAG, "package is invalid crc and send response.");
                                    setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_FAIL);
                                }
                            }
                        } else {
                            state = LinkDecoderState.ReadA;
                            LogTool.e(
                                    TAG,
                                    String.format("The package is invaild ending. (%s)",
                                            StringUtil.getHexString(ABCDEFG)));
                            if (ABCDEFG.length > 6) {
                                byte type = ABCDEFG[6];
                                if (type == TransportParser.REQUEST_PARSER_TYPE) {
                                    // 答复请求没有缓存,返回错误码没什么意义
                                    setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_FAIL);
                                }
                            }
                        }
                        break;
                    default:
                        state = LinkDecoderState.ReadA;
                        throw new Exception("Shouldn't reach here.");
                }

            } catch (Exception e) {
                LogTool.e(TAG, "Caught Exception", e);
                callback.onDisconnect(stop);
                close();
                break;
            }
        }


    }


    public synchronized void stopConnection() {
        stop = true;
        close();
    }

    private synchronized void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (timer != null) {
            timer.cancel();
        }
    }

    private void setTransportRsp(byte[] data, int flag) {
        if (data == null) {
            return;
        }
        byte[] ids = new byte[36];
        if (data.length < 44) {
            LogTool.e(TAG, "[setTransportRsp] data length = " + data.length + " but want size >= 44");
            return;
        }
        System.arraycopy(data, 7, ids, 0, 36);
        ResponseData responseData = new ResponseData(new String(ids), flag);
        LogTool.i(TAG, String.format("[setTransportRsp(0 = failed,1 = success,2 = total success)] uuid  = %s and flag = %s.", new String(ids), flag));

        // 若是外层包不符合要求，则获取其UUID发送失败标志位
        writeBytes(ResponseParser.dataPack(responseData));
    }


    public boolean writeBytes(byte[] bytes) {
        Log.i(TAG, "Current Thread = " + Thread.currentThread().getName() + ", OutputStream = " + outputStream);
        synchronized (outputStream) {
            if (!socket.isConnected()) {
                LogTool.e(TAG, "Socket Disconnected!");
                return false;
            }
            try {
                hasWriter = true;
                lastWriterTime = new Date().getTime();
                //Log.i(TAG, "writeBytes start bytes size = " + bytes.length + ", object = " + bytes);
                outputStream.write(bytes);
                //Log.i(TAG, "flush object = " + bytes);
                outputStream.flush();
                //Log.i(TAG, "writeBytes end bytes size = " + bytes.length + ", object = " + bytes);
                hasWriter = false;
                return true;
            } catch (Exception e) {
                LogTool.e(TAG, "outputStream写入异常", e);
                callback.onDisconnect(stop);
                close();
                return false;
            }
        }

    }


    public synchronized boolean isStoped() {
        return stop;
    }
}
