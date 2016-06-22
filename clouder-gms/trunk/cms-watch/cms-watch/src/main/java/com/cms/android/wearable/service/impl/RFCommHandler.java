package com.cms.android.wearable.service.impl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.codec.StringUtil;
import com.cms.android.wearable.service.codec.TransportParser;
import com.cms.android.wearable.service.common.LogTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by yang_shoulai on 11/5/2015.
 */
public class RFCommHandler extends Thread {

    private static final String TAG = "RFCommHandler";

    private BluetoothDevice device;

    private BluetoothSocket socket;

    private InputStream inputStream;

    private OutputStream outputStream;

    private HandlerCallback callback;

    private boolean stop = false;


    public RFCommHandler(BluetoothDevice device, BluetoothSocket socket, HandlerCallback callback) {
        LogTool.e(TAG, "RFCommHandler Init");
        setName(TAG);
        this.device = device;
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

    }

    interface HandlerCallback {
        void onDisconnect(boolean isStopNormal);

        void onDataReceived(byte[] bytes);

        void onDataSent(byte[] bytes);


        void onConnected(BluetoothDevice device);
    }

    class SimpleHandlerCallback implements HandlerCallback {

        @Override
        public void onDisconnect(boolean isStopNormal) {

        }

        @Override
        public void onDataReceived(byte[] bytes) {

        }

        @Override
        public void onDataSent(byte[] bytes) {

        }

        @Override
        public void onConnected(BluetoothDevice device) {

        }

    }

    public enum LinkDecoderState {
        ReadA, ReadBCDEFG
    }


    @Override
    public void run() {
        if (outputStream == null || inputStream == null) {
            Log.e(TAG, "RFCommHandler无法执行，因为BluetoothSocket尚未连接");
            callback.onDisconnect(false);
            return;
        }
        callback.onConnected(device);
        while (!stop) {
            LinkDecoderState state = LinkDecoderState.ReadA;
            byte[] ABCDEFG;
            try {
                switch (state) {
                    case ReadA:
                        int readLength = inputStream.read();
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        byte A1 = (byte) readLength;
                        if (A1 != TransportParser.KEY_REQUEST_START) {
                            LogTool.e(TAG,
                                    String.format("The package is invaild start 0 (%s)", StringUtil.getHexString(A1)));
                            continue;
                        }
                        readLength = inputStream.read();
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        byte A2 = (byte) readLength;
                        if (A2 != TransportParser.KEY_REQUEST_START) {
                            LogTool.e(TAG,
                                    String.format("The package is invaild start 1 (%s)", StringUtil.getHexString(A2)));
                            continue;
                        }
                        state = LinkDecoderState.ReadBCDEFG;
                    case ReadBCDEFG:
                        byte[] B = new byte[4];
                        readLength = inputStream.read(B);
                        if (readLength == -1) {
                            throw new IOException("Connection has been closed!");
                        }
                        int l0 = B[0] < 0 ? B[0] + 256 : B[0];
                        int l1 = B[1] < 0 ? B[1] + 256 : B[1];
                        int l2 = B[2] < 0 ? B[2] + 256 : B[2];
                        int l3 = B[3] < 0 ? B[3] + 256 : B[3];
                        int packageLength = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00)
                                | l0 & 0xFF;
                        //LogTool.d(TAG, "The package all length = " + String.valueOf(packageLength));

                        if (packageLength <= 0) {
                            LogTool.e(TAG, String.format("The package is invaild length(%s)", packageLength));
                            continue;
                        }
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

                        if (ABCDEFG[ABCDEFG.length - 2] == TransportParser.KEY_END_0
                                && ABCDEFG[ABCDEFG.length - 1] == TransportParser.KEY_END_1) {
                            state = LinkDecoderState.ReadA;
                            if (TransportParser.checkDataPack(ABCDEFG)) {
                                byte type = ABCDEFG[6];
                                switch (type) {
                                    case TransportParser.REQUEST_PARSER_TYPE:
                                        setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_SUCCESS);
                                        callback.onDataReceived(ABCDEFG);
                                        break;
                                    case TransportParser.RESPONSE_PARSER_TYPE:
                                        callback.onDataSent(ABCDEFG);
                                        break;
                                    default:
                                        LogTool.e(TAG, "Unknown type");
                                        break;
                                }

                            } else {
                                LogTool.e(TAG, String.format("The package is invaild crc. (%s)", StringUtil.getHexString(ABCDEFG)));
                                if (ABCDEFG.length > 6) {
                                    byte type = ABCDEFG[6];
                                    if (type == TransportParser.REQUEST_PARSER_TYPE) {
                                        // 答复请求没有缓存,返回错误码没什么意义
                                        setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_FAIL);
                                    }
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
                Log.e(TAG, "RFCommHandler处理线程异常", e);
                callback.onDisconnect(stop);
                close();
                break;
            }
        }
    }

    private void setTransportRsp(byte[] data, int flag) {
        byte[] ids = new byte[36];
        if (data.length < 44) {
            LogTool.e(TAG, "数据长度 = " + data.length + ", 小于44");
            return;
        }
        System.arraycopy(data, 7, ids, 0, 36);
        ResponseData responseData = new ResponseData(new String(ids), flag);
        // 若是外层包不符合要求，则获取其UUID发送失败标志位
        /*LogTool.i(TAG, String.format(
                "[setTransportRsp(0 = failed,1 = success,2 = total success)] uuid  = %s and flag = %s.",
                new String(ids), flag));*/
        writeBytes(ResponseParser.dataPack(responseData));
    }

    public boolean writeBytes(byte[] bytes) {
        synchronized (outputStream) {
            try {
                this.outputStream.write(bytes);
                this.outputStream.flush();
                return true;
            } catch (IOException e) {
                LogTool.e(TAG, "写入Byte数组异常", e);
                close();
            }
            return false;
        }

    }

    public synchronized void shutdown() {
        stop = true;
        close();
    }

    private synchronized void close() {
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean isStoped() {
        return stop;
    }
}
