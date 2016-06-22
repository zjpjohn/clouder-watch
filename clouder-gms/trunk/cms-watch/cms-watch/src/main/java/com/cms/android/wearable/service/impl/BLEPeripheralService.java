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
import android.app.AlarmManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.internal.DataEventParcelable;
import com.cms.android.wearable.internal.DataHolder;
import com.cms.android.wearable.internal.DataItemAssetParcelable;
import com.cms.android.wearable.internal.DataItemParcelable;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetFdForAssetResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;
import com.cms.android.wearable.internal.MessageEventHolder;
import com.cms.android.wearable.internal.NodeHolder;
import com.cms.android.wearable.internal.PutDataResponse;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.service.codec.ChildAsset;
import com.cms.android.wearable.service.codec.DataInfo;
import com.cms.android.wearable.service.codec.DataInfoParser;
import com.cms.android.wearable.service.codec.IParser;
import com.cms.android.wearable.service.codec.MessageData;
import com.cms.android.wearable.service.codec.MessageParser;
import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.codec.TransportData;
import com.cms.android.wearable.service.codec.TransportParser;
import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.BleUuid;
import com.cms.android.wearable.service.common.DigestUtils;
import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.common.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * ClassName: BLEPeripheralService
 *
 * @author xing_pengfei
 * @description BLEPeripheralService
 * @Date 2015-7-29
 */
public class BLEPeripheralService extends Service {

    private static final String TAG = "BLEPeripheralService";

    public static final String BLE_PHERIPHERAL_SERVICE_ACTION = "com.hoperun.ble.peripheral.service";

    public static final String BLE_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.BLEPeripheralService";

    public static final String RFCOMM_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.RFCOMMClientService";

    private static final int TRANSPORT_PACKAGE_LENGTH = 4196;

    private static final int MESSAGE_ORDINARY_PRIORITY_OFFSET = 12 * 60 * 60 * 1000;

    private static final int MESSAGE_FAILED_PRIORITY_OFFSET = 24 * 60 * 60 * 1000;

    private static final int DATA_ORDINARY_PRIORITY_OFFSET = 0;

    private static final int DATA_FAILED_PRIORITY_OFFSET = 6 * 60 * 60 * 1000;

    private static final int HANDLER_CACHE_CHECK = 8;

    private static final int TIMEOUT = 10 * 1000;

    private static final int TYPE_MESSAGE_LISTENER = 1;

    private static final int TYPE_DATA_LISTENER = 2;

    private static final int TYPE_NODE_LISTENER = 3;

    /**
     * 消息分包缓存队列
     * 消息消费线程轮训该队列将消息发送至手机设备
     */
    private PriorityBlockingQueue<QueuePriorityTask> mInQueue = new PriorityBlockingQueue<>();

    /**
     * 消息总包的缓存map,用户在消息发送失败的时候可以重发
     */
    private Map<String, IParser> mCacheInfoMap = new HashMap<>();

    /**
     * 消息的回调缓存
     */
    private Map<String, Callback> mCacheCallbackMap = new HashMap<>();

    private Map<String, ChildAsset> mCacheAssetDataMap = new HashMap<>();

    private Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    // task cache
    private Map<String, QueuePriorityTask> mCachePriorityTaskMap = new ConcurrentHashMap<>();

    private Map<String, List<TransportData>> mCacheTransportDataMap = new ConcurrentHashMap<>();

    private List<WearableListenerItem> mMessageWearableListenerList = new CopyOnWriteArrayList<>();

    private List<WearableListenerItem> mDataWearableListenerList = new CopyOnWriteArrayList<>();

    private List<WearableListenerItem> mNodeWearableListenerList = new CopyOnWriteArrayList<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private BluetoothAdapter mBTAdapter;

    private BluetoothLeAdvertiser mBTAdvertiser;

    private BluetoothGattServer mBTGattServer;

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            LogTool.e(TAG, "BLE广播服务启动失败 errorCode = " + errorCode);
            isStartSuccess = false;
            bleServerStarting = false;
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            LogTool.e(TAG, "BLE广播服务启动成功!");
            isStartSuccess = true;
            bleServerStarting = false;
        }
    };

    private BluetoothDevice mBLEDevice;

    private MessageQueueConsumer messageQueueConsumer;

    private String mLocalNodeId;

    /**
     * 当前BLE 关闭是否启动成功
     */
    private boolean isStartSuccess = false;

    /**
     * Gatt Server是否正在启动
     */
    private boolean bleServerStarting = false;

    /**
     * BLE current mState
     */
    private int mBLEState = BluetoothGattServer.STATE_DISCONNECTED;

    private IRFCommService mRfcommClientService;

    private BroadcastReceiver mBluetoothReceiver;

    private BluetoothManager bluetoothManager;


    /**
     * RFCOMM connected mState
     */

    private static final String RFCOMM_CONNECTED_SUCCESS = "1";

    private static final String RFCOMM_CONNECTED_FAILUE = "0";

    /**
     * node type
     */
    private static String NODE_WATCH = "1";

    private static String NODE_PHONE = "2";


    private Handler uiHandler = new Handler();

    /**
     * 与RFCOMM Service的连接
     */
    private ServiceConnection rfcommConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogTool.e(TAG, "与RFCommService绑定断开");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogTool.e(TAG, "与RFCommService绑定成功");
            mRfcommClientService = IRFCommService.Stub.asInterface(service);
            try {
                mRfcommClientService.registerCallback(rfcommCallback);
            } catch (RemoteException e) {
                LogTool.e(TAG, "注册RFComm服务监听异常", e);
            }
        }
    };

    private IRfcommClientCallback.Stub rfcommCallback = new IRfcommClientCallback.Stub() {

        @Override
        public void onRFCOMMSocketConnected(BluetoothDevice device) throws RemoteException {
            LogTool.d(TAG, "检测到RFComm连接已经成功，启动消息消费线程");
            if (messageQueueConsumer != null) {
                messageQueueConsumer.cancel();
            }
            messageQueueConsumer = new MessageQueueConsumer(mRfcommClientService, mInQueue, mCachePriorityTaskMap, mCacheInfoMap);
            messageQueueConsumer.start();
            if (device != null && mBTGattServer != null) {
                BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_RFCOMM_CONNECTED_STATUS));
                characteristic.setValue(RFCOMM_CONNECTED_SUCCESS + (mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress()));
                LogTool.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
                try {
                    mBTGattServer.notifyCharacteristicChanged(device, characteristic, false);
                } catch (Exception e) {
                    LogTool.e(TAG, "notifyCharacteristicChanged error", e);
                }
            }
        }

        @Override
        public void onRFCOMMSocketDisconnected(int cause) throws RemoteException {
            LogTool.e(TAG, "检测到RFComm连接已经断开，停止消息消费线程");
            if (messageQueueConsumer != null) {
                messageQueueConsumer.cancel();
            }
        }

        @Override
        public void onDataReceived(byte[] bytes) throws RemoteException {
            TransportData transportData = TransportParser.dataUnpack(bytes);
            LogTool.e(TAG, "[onDataReceived] index = " + transportData.getIndex() + ", count = " + transportData.getCount());
            String uuid = transportData.getUuid();
            long count = transportData.getCount();
            byte protocolType = transportData.getProtocolType();
            String filepath;
            if (count == 1) {
                LogTool.d(TAG, "包总数为1,直接进行上传到上层");
                filepath = FileUtil.createFilePath(uuid);
            } else {
                if (!mCacheTransportDataMap.containsKey(uuid)) {
                    LogTool.d(TAG, "[onDataReceived] mCacheTransportDataMap不包含该uuid = " + uuid);
                    List<TransportData> dataList = new ArrayList<>();
                    mCacheTransportDataMap.put(uuid, dataList);
                }
                List<TransportData> dataList = mCacheTransportDataMap.get(uuid);
                if (dataList.contains(transportData)) {
                    LogTool.w(TAG, "已经包含该TransportData,忽略");
                    return;
                } else {
                    LogTool.d(TAG, "[onDataReceived] dataList不包含该uuid = " + uuid + ", 添入dataList中");
                    dataList.add(transportData);
                    if (dataList.size() == count) {
                        LogTool.d(TAG, String.format("[onDataReceived] 数据包(uuid = %s)齐全 size = %d count =  %d,合并数据包", transportData.getUuid(), dataList.size(), count));
                        mCacheTransportDataMap.remove(uuid);
                        LogTool.d(TAG, "mCacheTransportDataMap size = " + mCacheTransportDataMap.size());
                        filepath = FileUtil.createFilePath(uuid);
                    } else {
                        return;
                    }
                }

            }
            switch (protocolType) {
                case TransportData.PROTOCOL_MESSAGE_TYPE:
                    handleMessage(filepath);
                    break;
                case TransportData.PROTOCOL_DATA_TYPE:
                    handleData(filepath);
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onDataSent(byte[] bytes) throws RemoteException {
            ResponseData response = ResponseParser.dataUnpack(bytes);
            String uuid = response.getUuid();
            switch (response.getStatus()) {
                case ResponseData.RESPONSE_STATUS_SUCCESS:
                    //LogTool.i(TAG, String.format("传输子包(UUID = %s)发送成功.", uuid));
                    // 成功,1.清除掉该消息的缓存 2.通知上层某一个包传输成功
                    handleDataSentSuccess(uuid);
                    break;
                case ResponseData.RESPONSE_STATUS_FAIL:
                    LogTool.w(TAG, String.format("传输子包(UUID = %s)发送失败,重新传送.", uuid));
                    reputQueue(uuid);
                    break;
                case ResponseData.RESPONSE_STATUS_TOTAL_SUCCESS:
                    LogTool.i(TAG, String.format("传输总包(UUID = %s)发送成功", uuid));
                    handleTotalDataSentSuccess(uuid);
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onConnectFailure(BluetoothDevice device) throws RemoteException {
            LogTool.e(TAG, "RFCOMM连接失败！");
            if (device != null && mBTGattServer != null) {
                try {
                    BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_RFCOMM_CONNECTED_STATUS));
                        if (characteristic != null) {
                            characteristic.setValue(RFCOMM_CONNECTED_FAILUE + (mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress()));
                            mBTGattServer.notifyCharacteristicChanged(device, characteristic, false);
                        }
                    }
                } catch (Exception e) {
                    LogTool.e(TAG, "notifyCharacteristicChanged error", e);
                }
            }
        }
    };

    private void clearQueue() {
        mInQueue.clear();
        mCacheInfoMap.clear();
        mCacheCallbackMap.clear();
        mCacheAssetDataMap.clear();
        nodeMap.clear();
        mCachePriorityTaskMap.clear();
        mCacheTransportDataMap.clear();
    }

    /**
     * handle message
     *
     * @param filePath the cache file's path
     * @throws RemoteException
     */
    private void handleMessage(String filePath) {
        //LogTool.d(TAG, "Received message,prepare to unpack message.");
        MessageData messageData = null;
        try {
            messageData = MessageParser.dataUnpack(new RandomAccessFile(filePath, "rw"));
            MessageEventHolder holder = messageData2Holder(messageData);
            LogTool.d(TAG, "Received message is " + holder.toString());
            String uuid = messageData.getUUID();
            onTotalTransportSuccess(uuid);
            onPostMessageReceived(uuid, holder);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * handle data bytes
     *
     * @param filePath
     * @throws RemoteException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void handleData(String filePath) {
        // 传入参数应该是文件名而不是byte[]数组 将基本信息读取后抛出
        LogTool.d(TAG, "Received data,prepare to unpack data." + filePath);
        try {
            DataInfo dataInfo = DataInfoParser.dataUnpack(new RandomAccessFile(filePath, "rw"));
            DataHolder dataHolder = dataInfo2Holder(dataInfo);
            String uuid = dataInfo.getUUID();
            //LogTool.d(TAG, "[handleData] UUID = " + uuid);
            onTotalTransportSuccess(uuid);
            onPostDataReceived(uuid, dataHolder);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * send total transport success flag
     *
     * @param id
     * @throws RemoteException
     */
    private void onTotalTransportSuccess(String id) throws RemoteException {
        if (mRfcommClientService != null) {
            mRfcommClientService.setTotalTransportSuccess(id);
        }
    }

    /**
     * convert MessageData to MessageEventHolder
     */
    private MessageEventHolder messageData2Holder(MessageData messageData) {
        if (messageData == null) {
            return null;
        }
        MessageEventHolder holder = new MessageEventHolder();
        holder.setNodeId(new String(messageData.getNodeId()));
        holder.setPath(new String(messageData.getPath()));
        holder.setData(messageData.getData());
        holder.setPackageName(messageData.getPackageName());
        return holder;
    }

    /**
     * convert DataInfo to DataHolder
     *
     * @param dataInfo
     * @return
     */
    private DataHolder dataInfo2Holder(DataInfo dataInfo) {
        if (dataInfo == null) {
            return null;
        }
        Map<String, Asset> map = dataInfo.getAssetsMap();
        Iterator<String> keys = map.keySet().iterator();
        Bundle bundle = new Bundle();
        while (keys.hasNext()) {
            String key = keys.next();
            ChildAsset asset = (ChildAsset) map.get(key);
            //LogTool.d(TAG, "key->" + key + " asset->" + asset.toString());
            //LogTool.e(TAG, "UUID->" + asset.getUuid() + " Index->" + asset.getIndex());
            String digest = asset.getDigest();
            if (TextUtils.isEmpty(digest)) {
                asset.setDigest(DigestUtils.md5(UUID.randomUUID().toString().getBytes()));
            }
            digest = asset.getDigest();
            DataItemAssetParcelable dataItemAsset = new DataItemAssetParcelable(digest, key);
            bundle.putParcelable(key, dataItemAsset);
            // cache data
            LogTool.e(TAG, "cache data-> digest = " + digest);
            mCacheAssetDataMap.put(digest, asset);
        }
        DataItemParcelable dataItem = new DataItemParcelable(dataInfo.getUri(), bundle, dataInfo.getData());
        List<DataItemParcelable> dataItems = new ArrayList<>();
        dataItems.add(dataItem);
        List<DataEventParcelable> dataEvents = new ArrayList<>();
        DataEventParcelable dataEvent = new DataEventParcelable(DataEvent.TYPE_CHANGED, dataItem);
        //LogTool.e(TAG, "dataEvent->" + dataEvent.getType());
        dataEvents.add(dataEvent);
        DataHolder dataHolder = new DataHolder(0, dataInfo.getPackageName(), dataItems, dataEvents);
        //LogTool.e(TAG, "dataEvent->>>>>" + dataHolder.getDataEvents().get(0).getType());

        return dataHolder;
    }

    /**
     * Post message to upper layer
     *
     * @param uuid
     * @param uuid
     */
    private void onPostMessageReceived(String uuid, MessageEventHolder messageEventHolder) {
        synchronized (mMessageWearableListenerList) {
            LogTool.i(TAG, String.format(
                    "手表端：遍历所有WearableListener接口(Message),发送消息到上层 -> node = %s  path = %s and data size = %d.",
                    messageEventHolder.getSourceNodeId(), messageEventHolder.getPath(),
                    (messageEventHolder.getData() == null ? 0 : messageEventHolder.getData().length)));
            LogTool.d(TAG, "当前缓存中接口个数为" + mMessageWearableListenerList.size());
            for (WearableListenerItem listener : mMessageWearableListenerList) {
                LogTool.i(
                        TAG,
                        "手表端：当前接收消息Node为 " + messageEventHolder.getSourceNodeId() + " 当前接收消息包名为 "
                                + listener.getPackageName());
                //LogTool.i(TAG, "mLocalNodeId = " + mLocalNodeId + " packageName = " + listener.getPackageName());
                if (listener.getPackageName().equals(messageEventHolder.getPackageName())
                        && !TextUtils.isEmpty(mLocalNodeId) && mLocalNodeId.equals(messageEventHolder.getSourceNodeId())) {
                    try {
                        LogTool.i(TAG, "手表端：Node节点匹配,通知上层接收消息");
                        listener.getListener().onMessageReceived(messageEventHolder);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogTool.i(TAG, "手机端：Node节点不匹配：v1 = " + listener.getPackageName());
                    LogTool.i(TAG, "手机端：Node节点不匹配：v2 = " + messageEventHolder.getPackageName());
                    LogTool.i(TAG, "手机端：Node节点不匹配：v3 = " + TextUtils.isEmpty(mLocalNodeId));
                    LogTool.i(TAG, "手机端：Node节点不匹配：v4 = " + mLocalNodeId);
                    LogTool.i(TAG, "手机端：Node节点不匹配：v5 = " + messageEventHolder.getSourceNodeId());
                    LogTool.i(TAG,
                            "手机端：Node节点不匹配：boolean = "
                                    + (listener.getPackageName().equals(messageEventHolder.getPackageName())
                                    && TextUtils.isEmpty(mLocalNodeId) && mLocalNodeId.equals(messageEventHolder
                                    .getSourceNodeId())));
                }
            }
            if (messageEventHolder.getPath().equals("/current_time")) {
                onSyncCurrentTime(messageEventHolder.getData());
            }

            FileUtil.deleteCacheFile(uuid);
        }
    }

    private void onSyncCurrentTime(byte[] data) {
        String str = new String(data);
        try {
            JSONObject json = new JSONObject(str);
            int year01 = json.getInt("year01");
            int year02 = json.getInt("year02");
            int month = json.getInt("moth");
            int day = json.getInt("day");
            int hour = json.getInt("hour");
            int minute = json.getInt("minute");
            int second = json.getInt("seconds");
            int dayOfWeek = json.getInt("dayOfWeek");
            int fraction = json.getInt("fraction");
            String timeZoneId = json.getString("timeZoneId");
            TimeZone timeZone = TimeZone.getDefault();
            if (timeZoneId != null && !"".equals(timeZoneId) && !timeZone.getID().equals(timeZoneId)) {
                try {
                    AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Log.i(TAG, "TimeZone Before Change = " + TimeZone.getDefault());
                    mAlarmManager.setTimeZone(timeZoneId);
                    Log.i(TAG, "System TimeZone After Change = " + TimeZone.getDefault());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Calendar cal = Calendar.getInstance();
            int l1 = year01 < 0 ? year01 + 256 : year01;
            int l0 = year02 < 0 ? year02 + 256 : year02;
            int year = (l0 << 8) | l1;
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            //cal.set(Calendar.DAY_OF_WEEK, value[7]);
            // 无符号byte转化 MILLISECOND分为256份
            int millisecond = fraction & 0xff;
            cal.set(Calendar.MILLISECOND, (int) (1000 * millisecond / 256f));
            cal.setTimeZone(TimeZone.getTimeZone(timeZoneId));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            Log.i(TAG, "时间同步, 调整时间为" + sdf.format(cal.getTime()) + ",TimeZone = " + cal.getTimeZone());
            // 将调整的时间通过广播方式发送出去
            long currentTime = cal.getTime().getTime();
            try {
                boolean isSuccess = SystemClock.setCurrentTimeMillis(currentTime);
                if (isSuccess) {
                    Log.d(TAG, "设置当前时间成功");
                } else {
                    Log.e(TAG, "设置当前时间失败");
                }

            } catch (Exception e) {
                Log.e(TAG, "设置当前时间异常", e);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Post data to upper layer
     *
     * @param dataHolder
     */
    private void onPostDataReceived(String uuid, DataHolder dataHolder) {
        synchronized (mDataWearableListenerList) {
            LogTool.i(TAG, "手表端：遍历所有WearableListener接口,发送数据到上层." + "当前缓存中接口个数为" + mDataWearableListenerList.size());
            for (WearableListenerItem listener : mDataWearableListenerList) {
                if (listener.getPackageName().equals(dataHolder.getPackageName())) {
                    try {
                        LogTool.i(TAG, "手表端：Node节点匹配,通知上层接收数据");
                        listener.getListener().onDataChanged(dataHolder);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // FileUtil.deleteCacheFile(uuid);
    }

    private IBLEPeripheralService.Stub mStub = new IBLEPeripheralService.Stub() {

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void sendMessage(IBLEPeripheralCallback callback, String node, String path, byte[] data)
                throws RemoteException {
            LogTool.d(TAG, String.format("[sendMessage] node = %s  path = %s and data = %d.", node, path, data.length));
            boolean toggle = Utils.getShardBondToggle(BLEPeripheralService.this);
            if (!toggle) {
                LogTool.i(TAG, "toggle false can't send message.");
                SendMessageResponse response = new SendMessageResponse();
                response.setRequestId(1);
                response.setStatusCode(-1);
                callback.setSendMessageRsp(response);
                return;
            }
            if (!checkCommunicateService()) {
                Log.d(TAG, "设备连接尚未全部建立，无法发送消息");
                SendMessageResponse response = new SendMessageResponse();
                response.setRequestId(1);
                response.setStatusCode(-1);
                callback.setSendMessageRsp(response);
                return;
            }
            if (!mRfcommClientService.isConnected()) {
                LogTool.e(TAG, "RFComm通道未连接,尝试连接，此时消息被缓存");
                BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
                characteristic.setValue("start");
                if (mBLEDevice != null) {
                    mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
                }
            }

            synchronized (this) {
                String packageName = callback.getPackageName();
                MessageData messageData = new MessageData(getDeviceId(), new byte[]{1, 0}, data, new Date().getTime(), packageName, path, node);
                MappedInfo mappedInfo = MessageParser.dataPack(messageData);
                if (mappedInfo == null) {
                    LogTool.e(TAG, "MessageParser.dataPack return null!");
                    SendMessageResponse response = new SendMessageResponse();
                    response.setRequestId(1);
                    response.setStatusCode(-1);
                    callback.setSendMessageRsp(response);
                    return;
                }
                String messageId = messageData.getUUID();
                // 将已处理-待返回Response消息缓存在Map中
                mCacheInfoMap.put(messageId, messageData);
                mCacheCallbackMap.put(messageId, new Callback(Callback.TYPE_MESSAGE, callback));
                //LogTool.i(TAG, "mCacheInfoMap size = " + mCacheInfoMap.size());
                long contentLength = new File(mappedInfo.getFilepath()).length();
                long count = contentLength / TRANSPORT_PACKAGE_LENGTH + 1;
                int packIndex = 0;
                int index = 0;
                long priority = new Date().getTime();
                Log.d(TAG, "开始分包，总共" + count + "个, Message Id = " + messageId + ", package = " + packageName);
                while (index < contentLength) {
                    int readLength = (int) (contentLength - index >= TRANSPORT_PACKAGE_LENGTH ? TRANSPORT_PACKAGE_LENGTH
                            : contentLength - index);
                    TransportData transportData = new TransportData(packageName, "path = " + path + " node = " + node, messageData.getUUID(), TransportData.PROTOCOL_MESSAGE_TYPE, mappedInfo,
                            contentLength, index, readLength, count, packIndex);
                    QueuePriorityTask task = new QueuePriorityTask(priority + packIndex - MESSAGE_ORDINARY_PRIORITY_OFFSET, transportData);
                    index += readLength;
                    putQueue(task);
                    packIndex++;
                }
            }
        }

        @Override
        public void registerCallback(String packageName, IBLEPeripheralCallback callback) throws RemoteException {

        }

        @Override
        public void disconnect() throws RemoteException {

        }

        @Override
        public void addListener(int type, IBLEPeripheralCallback callback, IWearableListener listener)
                throws RemoteException {
            addCommonListener(type, callback, listener);
        }

        @Override
        public void removeListener(int type, IBLEPeripheralCallback callback, IWearableListener listener)
                throws RemoteException {
            removeCommonListener(type, callback, listener);
        }

        @Override
        public void putDataItem(IBLEPeripheralCallback callback, PutDataRequest putDataRequest) throws RemoteException {
            LogTool.d(TAG, "[putDataItem] putDataRequest = " + putDataRequest.toString());
            boolean toggle = Utils.getShardBondToggle(BLEPeripheralService.this);
            if (!toggle) {
                LogTool.i(TAG, "toggle false can't send data.");
                PutDataResponse response = new PutDataResponse(1, -1, null);
                callback.setPutDataRsp(response);
                return;
            }
            if (!checkCommunicateService()) {
                Log.d(TAG, "设备连接尚未全部建立，无法发送消息");
                PutDataResponse response = new PutDataResponse(1, -1, null);
                callback.setPutDataRsp(response);
                return;
            }
            if (!mRfcommClientService.isConnected()) {
                LogTool.e(TAG, "RFComm通道未连接,尝试连接，此时消息被缓存");
                BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
                characteristic.setValue("start");
                if (mBLEDevice != null) {
                    mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
                }
            }
            // 若是多个应用同时进入putDataItem 那如何处理? TODO
            LogTool.d(TAG, "发送数据当前线程 id = " + Thread.currentThread().getId() + " name = "
                    + Thread.currentThread().getName());
            synchronized (this) {
                String packageName = callback.getPackageName();
                DataInfo dataInfo = new DataInfo(putDataRequest.getVersionCode(), putDataRequest.getUri(),
                        putDataRequest.getBundle(), putDataRequest.getData(), getDeviceId(), new Date().getTime(),
                        packageName);
                MappedInfo mappedInfo = DataInfoParser.dataPack(dataInfo);
                if (mappedInfo == null) {
                    LogTool.e(TAG, "DataInfoParser.dataPack return null!");
                    PutDataResponse response = new PutDataResponse(1, -1, null);
                    callback.setPutDataRsp(response);
                    return;
                }
                callback.setAssetRsp();
                // DataInfoParser.dataUnpack(randomAccessFile);
                String dataId = dataInfo.getUUID();
                LogTool.e(TAG, "put dataId = " + dataId);
                mCacheInfoMap.put(dataId, dataInfo);
                // 将已处理-待返回Response消息缓存在Map中
                mCacheCallbackMap.put(dataId, new Callback(Callback.TYPE_DATA, callback));
                LogTool.i(TAG, "mCacheInfoMap size = " + mCacheInfoMap.size());
                /**
                 * 当传送文件大于10M时 RandomAccessFile.getChannel().size()
                 * 和MappedByteBuffer capacity()和remaining()获取的大小都偏下
                 */
                long contentLength = new File(mappedInfo.getFilepath()).length();
                LogTool.d(TAG, "contentLength = " + contentLength + " capacity = "
                        + mappedInfo.getBuffer().capacity() + " remaining = " + mappedInfo.getBuffer().remaining());
                long count = contentLength / TRANSPORT_PACKAGE_LENGTH + 1;
                int packIndex = 0;
                int index = 0;
                long priority = new Date().getTime();
                while (index < contentLength) {
                    int readLength = (int) (contentLength - index >= TRANSPORT_PACKAGE_LENGTH ? TRANSPORT_PACKAGE_LENGTH : contentLength - index);
                    TransportData transportData = new TransportData(packageName, putDataRequest.getUri().toString(), dataInfo.getUUID(),
                            TransportData.PROTOCOL_DATA_TYPE, mappedInfo, contentLength, index, readLength, count,
                            packIndex);
                    LogTool.d(TAG, "正在分包, Data item id = " + dataId + ", package index = " + transportData.getPackIndex() + ", total = " + transportData.getCount());
                    QueuePriorityTask task = new QueuePriorityTask(priority + packIndex - DATA_ORDINARY_PRIORITY_OFFSET, transportData);
                    index += readLength;
                    putQueue(task);
                    packIndex++;
                }

            }


        }

        @Override
        public void getFdForAsset(IBLEPeripheralCallback callback, Asset asset) throws RemoteException {
            LogTool.d(TAG, "invoke getFdForAsset....." + asset.toString());
            if (callback == null) {
                LogTool.e(TAG, "添加监听接头失败");
                return;
            }
            Thread thread = Thread.currentThread();
            LogTool.d(TAG, "getFdForAsset thread id = " + thread.getId() + " name = " + thread.getName());
            LogTool.d(TAG, "after invoke getFdForAsset");
            String digest = asset.getDigest();
            if (mCacheAssetDataMap.containsKey(digest)) {
                LogTool.d(TAG, "Cache Asset 包含 digest = " + digest);
                ChildAsset childAsset = mCacheAssetDataMap.remove(digest);
                if (childAsset != null) {
                    ParcelFileDescriptor[] pfds;
                    try {
                        pfds = ParcelFileDescriptor.createPipe();
                    } catch (IOException e) {
                        throw new IllegalStateException("create Fd failed, digest = " + digest, e);
                    }
                    FutureTask<Boolean> futureTask = new FutureTask<>(new DataCallable(pfds[1], childAsset));
                    executorService.execute(futureTask);
                    GetFdForAssetResponse response = new GetFdForAssetResponse(1, 0, pfds[0]);
                    callback.setGetFdForAssetRsp(response);
                }
            } else {
                LogTool.e(TAG, "Cache Asset 不包含 digest = " + digest);
            }
        }

        @Override
        public void getDataItems(IBLEPeripheralCallback callback, Uri uri) throws RemoteException {

        }

        @Override
        public void getConnectedNodes(IBLEPeripheralCallback callback) throws RemoteException {
            GetConnectedNodesResponse response = new GetConnectedNodesResponse();
            List<Node> nodeList = new ArrayList<>();
            for (String key : nodeMap.keySet()) {
                nodeList.add(nodeMap.get(key));
            }
            response.setNodes(nodeList);
            callback.setGetConnectedNodesRsp(response);
            LogTool.d(TAG, "Set GetConnected Nodes Response, Node Size  = " + nodeList.size());
        }

        @Override
        public void getLocalNode(IBLEPeripheralCallback callback) throws RemoteException {
            GetLocalNodeResponse response = new GetLocalNodeResponse();
            String nodeId = mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress();
            String displayName = mBTAdapter.getName() != null ? mBTAdapter.getName() : nodeId;
            NodeHolder node = new NodeHolder(mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress(), displayName);
            response.setNode(node);
            callback.setLocalNodeRsp(response);
            LogTool.d(TAG, "set local node : " + node.getId());
        }

    };

    @SuppressLint("HandlerLeak")
    private Handler mCacheHandler = new Handler() {

        @SuppressLint("SimpleDateFormat")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_CACHE_CHECK:
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if (mCachePriorityTaskMap != null && mCachePriorityTaskMap.size() > 0) {
                        Iterator<String> uuidsIterator = mCachePriorityTaskMap.keySet().iterator();
                        long currentTime = new Date().getTime();
                        while (uuidsIterator.hasNext()) {
                            String uuid = uuidsIterator.next();
                            QueuePriorityTask task = mCachePriorityTaskMap.get(uuid);
                            if (task != null && currentTime - task.getTime() > TIMEOUT) {
                                LogTool.w(TAG, String.format("检测到传输子包(UUID = %s)已超时,重新传送.", uuid));
                                reputQueue(uuid);
                            } else {
                                continue;
                            }
                        }
                    }
                    //LogTool.d(TAG, "重新发送HANDLER_CACHE_CHECK消息 Time = " + sdf.format(new Date()));
                    sendEmptyMessageDelayed(HANDLER_CACHE_CHECK, 5000);
                    break;

                default:
                    break;
            }
        }

    };

    private boolean checkCommunicateService() throws RemoteException {
        if (!isStartSuccess || mBTGattServer == null) {
            LogTool.e(TAG, "当前BLE Server未启动，等待...");
        } else if (!isBLEServiceAvailable()) {
            LogTool.e(TAG, "当前BLE未连接,等待手机连接...");
        } else if (!isRfcommServiceAvailable() || mRfcommClientService == null) {
            LogTool.e(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
            bindRfcommService();
        } else {
            LogTool.i(TAG, "服务都已经准备就绪");
            return true;
        }
        return false;
    }

    /**
     * reset priority
     */
    private void resetCachePriority(QueuePriorityTask task) {
        if (task == null) {
            return;
        }
        TransportData transportData = task.getData();
        String uuid = transportData.getUuid();
        IParser parser = mCacheInfoMap.get(uuid);
        int offset = parser.getType() == IParser.TYPE_MESSAGE ? MESSAGE_FAILED_PRIORITY_OFFSET
                : DATA_FAILED_PRIORITY_OFFSET;
        task.setPriority(task.getRepeat() == 0 ? (task.getPriority() - offset) : task.getPriority());
    }

    /**
     * put task in queue
     *
     * @param task
     */
    private void putQueue(QueuePriorityTask task) {
        if (task == null) {
            return;
        }
        mInQueue.offer(task);
    }

    /**
     * reput PriorityTask in queue
     *
     * @param id
     */
    private void reputQueue(String id) {
        if (TextUtils.isEmpty(id)) {
            LogTool.e(TAG, "reput queue error id = " + id);
            return;
        }
        QueuePriorityTask task = mCachePriorityTaskMap.remove(id);
        if (task == null) {
            LogTool.e(TAG, "reput queue error CachePriorityTaskMap缓存中无对应对象");
            return;
        }
        TransportData transportData = task.getData();
        String uuid = transportData.getUuid();
        IParser parser = mCacheInfoMap.get(uuid);
        if (parser == null) {
            LogTool.e(TAG, "reput queue error CacheInfoMap缓存中无对应对象");
            return;
        }
        // reset befor repeat
        resetCachePriority(task);
        // 重发次数
        int repeat = task.getRepeat();
        task.setRepeat(++repeat);
        task.setTime(new Date().getTime());
        putQueue(task);
    }

    /**
     * handle sub package succeed
     *
     * @param uuid
     */
    private void handleDataSentSuccess(String uuid) {
        mCachePriorityTaskMap.remove(uuid);
    }

    /**
     * handle total package succeed
     *
     * @param uuid
     * @throws RemoteException
     */
    private void handleTotalDataSentSuccess(String uuid) throws RemoteException {
        if (mCacheInfoMap.containsKey(uuid)) {
            Callback callback = mCacheCallbackMap.remove(uuid);
            if (callback != null) {
                switch (callback.getType()) {
                    case Callback.TYPE_MESSAGE:
                        LogTool.d(TAG, "消息全部传输成功, uuid = " + uuid + ", 正在调用传输成功回调" + "消息类型为[Message]");
                        if (mCacheInfoMap.containsKey(uuid)) {
                            mCacheInfoMap.remove(uuid);
                            SendMessageResponse response = new SendMessageResponse();
                            response.setRequestId(1);
                            response.setStatusCode(0);
                            callback.getCallback().setSendMessageRsp(response);
                            //LogTool.d(TAG, "[handleTotalDataSentSuccess] invoke setSendMessageRsp");
                        }
                        break;
                    case Callback.TYPE_DATA:
                        LogTool.d(TAG, "消息全部传输成功, uuid = " + uuid + ", 正在调用传输成功回调" + "消息类型为[Data]");
                        if (mCacheInfoMap.containsKey(uuid)) {
                            DataInfo dataInfo = (DataInfo) mCacheInfoMap.remove(uuid);
                            Bundle assets = dataInfo.getAssets();
                            Bundle bundle = new Bundle();
                            DataItemParcelable dataItem = new DataItemParcelable(dataInfo.getUri(), bundle, dataInfo.getData());
                            PutDataResponse putDataResponse = new PutDataResponse(1, 0, dataItem);
                            callback.getCallback().setPutDataRsp(putDataResponse);
                            //LogTool.d(TAG, "[handleTotalDataSentSuccess] invoke setPutDataRsp");
                        }
                        break;
                    default:
                        break;
                }
            } else {
                LogTool.d(TAG, "消息全部传输成功, uuid = " + uuid + ", 回调监听不存在！");
            }
            FileUtil.deleteCacheFile(uuid);
        } else {
            LogTool.e(TAG, "消息全部发送成功，但是消息在mCacheInfoMap中已经不存在，无需回调监听");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogTool.i(TAG, "BLEPeripheralService OnCreate()!");
        LogTool.i(TAG, "开始绑定RFComm Service!");
        bindRfcommService();
        LogTool.i(TAG, "注册蓝牙开关状态广播接收器");
        mBluetoothReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        bluetoothManager = BleUtil.getManager(this);
        if (!BleUtil.isBLESupported(this)) {
            LogTool.e(TAG, "当前设备不支持BLE!!!");
            stopSelf();
            return;
        }
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBTAdapter == null) {
            LogTool.e(TAG, "当前设备不支持蓝牙");
            stopSelf();
            return;
        }
        String address = mBTAdapter.getAddress();
        LogTool.e(TAG, "当前手表的蓝牙MAC地址 = " + address);
        /**
         * wake up timeout scan
         */
        LogTool.e(TAG, "启动消息缓存检查");
        mCacheHandler.sendEmptyMessageDelayed(HANDLER_CACHE_CHECK, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogTool.e(TAG, "BLEPeripheralService onDestroy()");
        LogTool.e(TAG, "取消蓝牙开关状态广播接收器");
        unregisterReceiver(mBluetoothReceiver);
        LogTool.e(TAG, "取消RFCommService绑定");
        unbindRfcommService();
        resetBleServer();
    }

    /**
     * 重置BLE Server包括:
     * 停止GattServer,
     * 停止BLE广播，
     * 停止RFComm Service,
     * 停止消息消费线程
     * 清空缓存的数据
     */
    private void resetBleServer() {
        LogTool.e(TAG, "清空缓存");
        clearQueue();
        if (messageQueueConsumer != null) {
            LogTool.e(TAG, "停止消息处理器");
            messageQueueConsumer.cancel();
        }
        if (mRfcommClientService != null) {
            try {
                LogTool.e(TAG, "断开RFCOMM连接");
                mRfcommClientService.disconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        LogTool.e(TAG, "停止GattServer");
        if (mBTGattServer != null) {
            mBTGattServer.close();
            mBTGattServer = null;
        }
        LogTool.e(TAG, "停止蓝牙BLE广播");
        if (mBTAdvertiser != null) {
            try {
                mBTAdvertiser.stopAdvertising(mAdvertiseCallback);
            } catch (Exception e) {
                LogTool.e(TAG, "Caught Exception", e);
            }
        }
        mBLEState = BluetoothGatt.STATE_DISCONNECTED;
        bleServerStarting = false;
        isStartSuccess = false;
    }


    private class MyBluetoothGattServerCallback extends BluetoothGattServerCallback {

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            String deviceAddress = device.getAddress();
            String deviceName = device.getName();
            LogTool.d(TAG, String.format("【onConnectionStateChange】 device address = %s, status = %d, newState = %d.", deviceAddress, status, newState));
            String bondAddress = Utils.getShardBondAddress(BLEPeripheralService.this);
            if (BluetoothGattServer.STATE_CONNECTED == newState) {
                Log.e(TAG, String.format("设备【%s, %s】BLE已经连接！", deviceName, deviceAddress));
                Log.e(TAG, "已经绑定的设备地址 = " + bondAddress);
                if (bondAddress != null && bondAddress.equals(deviceAddress)) {
                    // 连接成功
                    Log.e(TAG, "该设备是已经绑定了设备，连接成功");
                    Log.e(TAG, String.format("设备【%s, %s】BLE连接成功！", deviceName, deviceAddress));
                    mBLEState = BluetoothGattServer.STATE_CONNECTED;
                    Utils.saveSharedBondToggle(BLEPeripheralService.this, true);
                    mBLEDevice = device;
                    broadcastBLeConnectionChangeEvent(device, newState);

                } else {
                    Log.e(TAG, "非法的客户端连接请求，通知该设备断开连接！");
                    BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                    BluetoothGattCharacteristic heartbeatCharacteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING));
                    heartbeatCharacteristic.setValue("2#disconnect");
                    boolean isSuccess = mBTGattServer.notifyCharacteristicChanged(device, heartbeatCharacteristic, false);
                    uiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBTGattServer.cancelConnection(device);
                        }
                    }, 500);
                }

            } else {
                Log.e(TAG, String.format("设备【%s,%s】BLE断开连接！", deviceName, deviceAddress));
                Log.e(TAG, "已经绑定的设备地址 = " + bondAddress);
                if (bondAddress != null && bondAddress.equals(deviceAddress)) {
                    // 连接失败
                    Log.i(TAG, "已经绑定的设备断开了连接,回调NodeDisconnect接口");
                    mBLEDevice = null;
                    mBLEState = BluetoothGattServer.STATE_DISCONNECTED;
                    Log.i(TAG, "清空所有消息数据缓存");
                    clearQueue();
                    Log.i(TAG, "断开已经存在的RFComm连接");
                    if (mRfcommClientService != null) {
                        try {
                            mRfcommClientService.disconnect();
                        } catch (RemoteException e) {

                        }
                    }
                    Log.i(TAG, "停止消息消费线程");
                    if (messageQueueConsumer != null) {
                        messageQueueConsumer.cancel();
                    }
                    NodeHolder nodeHolder = new NodeHolder(device.getAddress(), device.getName() == null ? device.getAddress() : device.getName());
                    onPeerDisconnected(nodeHolder);
                    broadcastBLeConnectionChangeEvent(device, newState);

                } else {
                    Log.w(TAG, "无关的设备断开了连接");
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            LogTool.d(TAG, "【onCharacteristicReadRequest】");
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            LogTool.d(TAG, "【onCharacteristicWriteRequest】");
            if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING))) {
                int status = -1;
                String mac = "";
                String content = new String(value);
                LogTool.d(TAG, "接收到RFComm服务命令 = " + content);
                if (content.contains("&")) {
                    String[] stringArray = content.split("&");
                    status = Integer.parseInt(stringArray[0]);
                    mac = stringArray[1];
                } else if (content.contains("#")) {
                } else {
                    LogTool.e(TAG, String.format("命令格式错误,应该是type&Bluetooth MAC address"));
                    return;
                }
                // 将蓝牙地址作为启动RFCOMM标志位
                try {
                    if (isRfcommServiceAvailable() && mRfcommClientService != null) {
                        if (status == RFCOMMClientService.RFCOMM_CONNECT_READY && BleUtil.isBluetoothAddress(mac)) {
                            LogTool.d(TAG, "手表端启动RFComm连接服务");
                            mRfcommClientService.setRemoteDeviceState(RFCOMMClientService.RFCOMM_CONNECT_READY);
                            mRfcommClientService.connect(mac);
                        } else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXCEPTION) {
                            LogTool.d(TAG, "手机端RFCommServer已经因为【过期、停止、异常】而停止, Status = " + status);
                            mRfcommClientService.setRemoteDeviceState(status);
                            LogTool.d(TAG, "手机端RFCommServer停止");
                            mRfcommClientService.disconnect();
                            LogTool.d(TAG, "手机端重启");
                            mRfcommClientService.connect(mac);
                        } else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXPIRED
                                || status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_STOP) {
                            mRfcommClientService.setRemoteDeviceState(status);
                        } else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_RESTART) {
                            LogTool.d(TAG, "手表端重启REFComm连接");
                        } else {
                            LogTool.e(TAG, "未知命令");
                        }
                    } else {
                        // 应该启动rfcomm服务并且根据所给地址建立rfcomm通道 TODO
                        Log.e(TAG, "尚未绑定RFComm服务，命令无法处理，正在尝试绑定该服务");
                        bindRfcommService();
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "处理RFComm服务命令异常！", e);
                }

            } else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING))) {
                String content = new String(value);
                LogTool.d(TAG, "Cloud watch heartbeat or sync time command is " + new String(value));
                if (content.contains("#")) {
                    String[] stringArray = content.split("#");
                    int type = Integer.parseInt(stringArray[0]);
                    String command = stringArray[1];
                    switch (type) {
                        case 1:
                            LogTool.d(TAG, "接收到心跳命令");
                            int heartBeat = Integer.parseInt(command);
                            LogTool.d(TAG, "接收到心跳包， value = " + heartBeat);
                            if (mBTGattServer != null) {
                                BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
                                BluetoothGattCharacteristic heartbeatCharacteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING));
                                heartBeat++;
                                heartbeatCharacteristic.setValue("1#" + heartBeat);
                                boolean isSuccess = mBTGattServer.notifyCharacteristicChanged(device, heartbeatCharacteristic, false);
                                if (isSuccess) {
                                    LogTool.d(TAG, "设置心跳包 value = " + heartBeat + " 成功");
                                } else {
                                    LogTool.e(TAG, "设置心跳包 value = " + heartBeat + " 失败");
                                }
                            }
                            break;
                        case 2:
                            long time = Long.parseLong(command);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
                            LogTool.d(TAG, "手表端：同步时间 time = " + sdf.format(time));
                            LogTool.d(TAG, "接收到时间同步命令, DateTime = " + time);
                            byte[] timeBytes = new byte[10];
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(time);
                            int year = cal.get(Calendar.YEAR);
                            // year 低位在前,高位在后
                            timeBytes[0] = (byte) (year & 0xff);
                            timeBytes[1] = (byte) (year >> 8 & 0xff);
                            // month
                            timeBytes[2] = (byte) cal.get(Calendar.MONTH);
                            // day
                            timeBytes[3] = (byte) cal.get(Calendar.DAY_OF_MONTH);
                            // hour
                            timeBytes[4] = (byte) cal.get(Calendar.HOUR_OF_DAY);
                            // minute
                            timeBytes[5] = (byte) cal.get(Calendar.MINUTE);
                            // second
                            timeBytes[6] = (byte) cal.get(Calendar.SECOND);
                            // day of week
                            timeBytes[7] = (byte) cal.get(Calendar.DAY_OF_WEEK);
                            // fraction
                            timeBytes[8] = (byte) (cal.get(Calendar.MILLISECOND) * 256 / 1000f);
                            // adjust reason
                            timeBytes[9] = 0x01;
                            setCurrentTime(timeBytes, stringArray.length > 2 ? stringArray[2] : null);
                            break;
                        default:
                            break;
                    }
                } else {
                    LogTool.e(TAG, String.format("命令格式错误 %s,应该是type#commad", content));
                    return;
                }
            } else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_CURRENT_TIME))) {
                setCurrentTime(value, null);
            } else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_NODE))) {
                if (value != null) {
                    String nodeType = new String(value).substring(0, 1);
                    String nodeId = new String(value).substring(1);
                    if (nodeType.equals(NODE_PHONE)) {
                        NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId : device.getName());
                        onPeerConnected(nodeHolder);
                    } else if (nodeType.equals(NODE_WATCH)) {
                        mLocalNodeId = nodeId;
                    } else {
                        LogTool.e(TAG, "无NodeId");
                    }
                } else {
                    LogTool.e(TAG, "无NodeId");
                }
            } else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_SYSTEM_ID_STRING))) {

            } else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_SERIAL_NUMBER_STRING))) {
                String command = new String(value);
                LogTool.d(TAG, "current commad is " + command);

                if (command.equals("disconnect")) {
                    LogTool.d(TAG, "save shared toggle = " + command);
                    if (device.getAddress().equals(Utils.getShardBondAddress(BLEPeripheralService.this))) {
                        Utils.saveSharedBondToggle(BLEPeripheralService.this, false);
                    }
                } else {
                    LogTool.e(TAG, "Unknown command");
                }
            } else {
                LogTool.e(TAG, "无匹配Characteristic");
            }
            if (mBTGattServer != null) {
                mBTGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{});
            }
        }


        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            LogTool.d(TAG, String.format("onExecuteWrite device = %s, requestId = %d, execute = %s.", device, requestId, execute + ""));
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            LogTool.d(TAG, String.format("【onNotificationSent】 device = %s, status = %d.", device, status));
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogTool.d(TAG, "添加服务" + service.getUuid().toString() + "成功");
            } else {
                LogTool.e(TAG, "添加服务" + service.getUuid().toString() + "失败！");
            }
        }

    }

    private void startCloudWatchAdvertising() {
        if (bleServerStarting) {
            Log.i(TAG, "BLE广播正在启动，请等待...");
            return;
        }
        Log.i(TAG, "正在开启BLE广播...");
        bleServerStarting = true;
        resetBleServer();
        mBTGattServer = bluetoothManager.openGattServer(this, new MyBluetoothGattServerCallback());
        if (mBTGattServer == null) {
            LogTool.e(TAG, "无法打开GattServer，openGattServer返回null");
            isStartSuccess = false;
            bleServerStarting = false;
            return;
        }
        if (setupCloudWatchService()) {
            LogTool.i(TAG, "添加自定义服务成功");
            //mBTAdapter.setName(mBTAdapter.getAddress());
            mBTAdvertiser.startAdvertising(BleUtil.createAdvSettings(true, 0), BleUtil.createCloudWatchAdvertiseData(), mAdvertiseCallback);
        } else {
            LogTool.e(TAG, "添加自定义服务失败,关闭并等待下一次尝试");
            mBTGattServer.clearServices();
            mBTGattServer.close();
            isStartSuccess = false;
            bleServerStarting = false;
        }
    }

    private void setCurrentTime(byte[] value, String timeZoneId) {
        TimeZone timeZone = TimeZone.getDefault();
        if (timeZoneId != null && !timeZone.equals(timeZoneId)) {
            try {
                AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Log.i(TAG, "TimeZone Before Change = " + TimeZone.getDefault());
                mAlarmManager.setTimeZone(timeZoneId);
                Log.i(TAG, "System TimeZone After Change = " + TimeZone.getDefault());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (value != null && value.length == 10) {
            Calendar cal = Calendar.getInstance();
            int l1 = value[0] < 0 ? value[0] + 256 : value[0];
            int l0 = value[1] < 0 ? value[1] + 256 : value[1];
            int year = (l0 << 8) | l1;
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, value[2]);
            cal.set(Calendar.DAY_OF_MONTH, value[3]);
            LogTool.d(TAG, "value[0]:" + value[0] + " value[1]" + value[1] + " value[2]:" + value[2] + " value[3]:" + value[3]);
            cal.set(Calendar.HOUR_OF_DAY, value[4]);
            cal.set(Calendar.MINUTE, value[5]);
            cal.set(Calendar.SECOND, value[6]);
            //cal.set(Calendar.DAY_OF_WEEK, value[7]);
            // 无符号byte转化 MILLISECOND分为256份
            int millisecond = value[8];
            millisecond = value[8] & 0xff;
            cal.set(Calendar.MILLISECOND, (int) (1000 * millisecond / 256f));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            LogTool.i(TAG, "时间同步, 调整时间为" + sdf.format(cal.getTime()) + ",TimeZone = " + cal.getTimeZone());
            // 将调整的时间通过广播方式发送出去
            cal.setTimeZone(timeZoneId == null ? timeZone : TimeZone.getTimeZone(timeZoneId));
            long currentTime = cal.getTime().getTime();
            sendSyncTimeBroadcast(currentTime, value[8]);
            try {
                boolean isSuccess = SystemClock.setCurrentTimeMillis(currentTime);
                if (isSuccess) {
                    LogTool.d(TAG, "设置当前时间成功");
                } else {
                    LogTool.e(TAG, "设置当前时间失败");
                }

            } catch (Exception e) {
                LogTool.e(TAG, "设置当前时间异常", e);
            }

        } else {
            LogTool.e(TAG, "时间同步命令,格式错误");
        }
    }

    private void cancelConnection() {
        clearQueue();
        if (mRfcommClientService != null) {
            try {
                mRfcommClientService.disconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (messageQueueConsumer != null) {
            messageQueueConsumer.cancel();
        }
        if (mBTGattServer != null && mBLEDevice != null) {
            BluetoothGattService service = mBTGattServer.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
            BluetoothGattCharacteristic heartbeatCharacteristic = service.getCharacteristic(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING));
            heartbeatCharacteristic.setValue("2#disconnect");
            boolean isSuccess = mBTGattServer.notifyCharacteristicChanged(mBLEDevice, heartbeatCharacteristic, false);
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBTGattServer != null && mBLEDevice != null) {
                        mBTGattServer.cancelConnection(mBLEDevice);
                    }
                }
            }, 500);
        }
    }


    private boolean setupCloudWatchService() {
        /**
         * For Now 暂时将Device Info的协议替代为Cloud Watch
         */
        Log.d(TAG, "正在添加自定义BLE服务...");
        // device information
        BluetoothGattService dis = new BluetoothGattService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        // manufacturer name string char. 暂时用来通知手表端rfcomm
        // server已经打开，让手表端主动去连接
        BluetoothGattCharacteristic mansc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // model number string char. 暂时用作通知手机端打开rfcomm server
        BluetoothGattCharacteristic monsc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ
                | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // serial number string char.暂时用作断开状态
        BluetoothGattCharacteristic snsc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_SERIAL_NUMBER_STRING), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // serial number string char. 暂时用来表示BLE心跳
        BluetoothGattCharacteristic sysIdsc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_SYSTEM_ID_STRING), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic nodebc = new BluetoothGattCharacteristic(UUID.fromString(BleUuid.CHAR_NODE),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ
                | BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic rfcommStatus = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_RFCOMM_CONNECTED_STATUS), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        dis.addCharacteristic(mansc);
        dis.addCharacteristic(monsc);
        dis.addCharacteristic(snsc);
        dis.addCharacteristic(sysIdsc);
        dis.addCharacteristic(nodebc);
        dis.addCharacteristic(rfcommStatus);


        BluetoothGattService cloudService = new BluetoothGattService(
                UUID.fromString(BleUuid.SERVICE_CLOUDER_WATCH), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // rfcomm
        BluetoothGattCharacteristic rfcommbc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_CLOUDER_WATCH_RFCOMM), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ
                | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // notification
        BluetoothGattCharacteristic notifybc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_CLOUDER_WATCH_NOTIFICATION), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        cloudService.addCharacteristic(rfcommbc);
        cloudService.addCharacteristic(notifybc);


        BluetoothGattService timeService = new BluetoothGattService(UUID.fromString(BleUuid.SERVICE_CURRENT_TIME),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic ctsc = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.CHAR_CURRENT_TIME), BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        timeService.addCharacteristic(ctsc);
        Log.i(TAG, "添加dis Service" + (mBTGattServer.addService(dis) ? "成功" : "失败"));
        Log.i(TAG, "添加cloud Service " + (mBTGattServer.addService(cloudService) ? "成功" : "失败"));
        Log.i(TAG, "添加Current Time Service " + (mBTGattServer.addService(timeService) ? "成功" : "失败"));

        return true;
    }

    /**
     * 绑定Rfcomm服务连接
     */
    private void bindRfcommService() {
        Log.e(TAG, "开始绑定RFCommService");
        Intent intent = new Intent(this, RFCommService.class);
        bindService(intent, rfcommConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindRfcommService() {
        if (rfcommConnection != null) {
            unbindService(rfcommConnection);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogTool.e(TAG, "BLEPeripheralService onStartCommand()");
        String bondedAddress = Utils.getShardBondAddress(this);
        LogTool.i(TAG, "当前配对的蓝牙设备地址 = " + bondedAddress);
        boolean serviceRestart = false;
        if (intent != null) {
            String address = intent.getStringExtra(Utils.SHARED_BLUETOOTH_BOND_ADDRESS);
            if (address != null && !"".equals(address)) {
                if (!address.equals(bondedAddress)) {
                    serviceRestart = true;
                    Utils.saveSharedBondAddress(this, address);
                    Log.i(TAG, "发现新的配对蓝牙设备 Address = " + address + "，保存新的地址");
                }
            }
        }
        boolean airPlaneModeOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;

        if (serviceRestart) {
            Log.w(TAG, "BLE Server需要取消掉现有的连接");
            cancelConnection();
        } else if (airPlaneModeOn) {
            Log.w(TAG, "当前设备处于飞行模式，无法启动BLE广播");
        } else {
            if (mBTAdapter.getState() == BluetoothAdapter.STATE_ON) {
                LogTool.e(TAG, "【BLE连接检测】设备蓝牙已经开启");
                if (bleServerStarting) {
                    LogTool.e(TAG, "【BLE连接检测】BLE广播正在开启，请等待完成");
                } else if (isStartSuccess && mBTGattServer != null) {
                    LogTool.e(TAG, "【BLE连接检测】BLE广播已经启动，无需重启");
                } else {
                    mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
                    if (mBTAdvertiser == null) {
                        LogTool.e(TAG, "mBTAdvertiser为null, 当前设备不支持蓝牙BLE广播");
                    } else {
                        startCloudWatchAdvertising();
                    }
                }
            } else if (mBTAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                LogTool.e(TAG, "【BLE连接检测】设备蓝牙尚未开启，正在开启蓝牙...");
                mBTAdapter.enable();
            } else {
                LogTool.e(TAG, "【BLE连接检测】 设备蓝牙正在开启/关闭，请等待动作完成");
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }

    /**
     * common add listener operation
     *
     * @param type
     * @param callback
     * @param listener
     * @throws RemoteException
     */
    private void addCommonListener(int type, IBLEPeripheralCallback callback, IWearableListener listener)
            throws RemoteException {
        try {
            List<WearableListenerItem> wearableListenerList = null;
            switch (type) {
                case TYPE_MESSAGE_LISTENER:
                    wearableListenerList = mMessageWearableListenerList;
                    break;
                case TYPE_DATA_LISTENER:
                    wearableListenerList = mDataWearableListenerList;
                    break;
                case TYPE_NODE_LISTENER:
                    wearableListenerList = mNodeWearableListenerList;
                    break;
                default:
                    LogTool.w(TAG, "手表端：wearableListenerList NullPointerException");
                    return;
            }
            clearUnusedListener(wearableListenerList);
            LogTool.d(TAG, String.format("手表端：准备添加监听器(type = %d),当前监听器count = %d.", type, wearableListenerList.size()));
            if (callback == null) {
                LogTool.e(TAG, "添加监听接头失败");
                return;
            }
            if (listener == null) {
                callback.setStatusRsp(new Status(3006));
                return;
            }
            wearableListenerList.add(new WearableListenerItem(callback.getPackageName(), listener));
            callback.setStatusRsp(new Status(0));
            LogTool.d(TAG, "手表端：添加监听器成功,当前监听器 count =" + wearableListenerList.size());
        } catch (Exception e) {
            LogTool.e(TAG, "addCommonListener Exception", e);
        }

    }

    /**
     * common remove listener operation
     *
     * @param type
     * @param callback
     * @param listener
     * @throws RemoteException
     */
    private void removeCommonListener(int type, IBLEPeripheralCallback callback, IWearableListener listener)
            throws RemoteException {
        List<WearableListenerItem> wearableListenerList = null;
        switch (type) {
            case TYPE_MESSAGE_LISTENER:
                wearableListenerList = mMessageWearableListenerList;
                break;
            case TYPE_DATA_LISTENER:
                wearableListenerList = mDataWearableListenerList;
                break;
            case TYPE_NODE_LISTENER:
                wearableListenerList = mNodeWearableListenerList;
                break;
            default:
                throw new NullPointerException("手表端：wearableListenerList NullPointerException");
        }
        if (callback == null) {
            LogTool.e(TAG, "添加监听接口失败");
            return;
        }
        if (listener == null) {
            callback.setStatusRsp(new Status(3006));
            return;
        }
        clearUnusedListener(wearableListenerList);
        LogTool.d(TAG, String.format("手表端：准备移除监听器(type = %d),当前监听器count = %d.", type, wearableListenerList.size()));
        int index = -1;
        int size = wearableListenerList.size();
        if (size > 0) {
            Iterator<WearableListenerItem> iterator = wearableListenerList.iterator();
            while (iterator.hasNext()) {
                WearableListenerItem item = iterator.next();
                try {
                    item.getListener().id();
                } catch (DeadObjectException e) {
                    iterator.remove();
                    LogTool.e(TAG, "移除已死掉的回调接口");
                }
            }
            for (int i = 0; i < wearableListenerList.size(); i++) {
                WearableListenerItem item = wearableListenerList.get(i);
                String id = item.getListener().id();
                String listenId = listener.id();
                LogTool.d(TAG, " item id = " + id + " listener id = " + listenId);
                String packageName = item.getPackageName();
                String callbackPackageName = callback.getPackageName();
                LogTool.d(TAG, " packageName = " + packageName + " callback packageName = " + listenId);
                boolean flag = (id.equals(listenId) && (packageName.equals(callbackPackageName)));
                LogTool.d(TAG, "flag->" + flag);
                if (flag) {
                    index = i;
                    LogTool.d(TAG, "index = " + index);
                    break;
                }
            }
            LogTool.d(TAG, "移除监听器 index = " + index);
            if (index >= 0 && index < wearableListenerList.size()) {
                WearableListenerItem item = wearableListenerList.remove(index);
                LogTool.d(TAG, "移除监听器 item = " + item);
            }
        }
        LogTool.d(TAG, "移除监听器成功,当前监听器count = " + wearableListenerList.size());
        callback.setStatusRsp(new Status(0));
    }

    /**
     * clear unused listener
     *
     * @param wearableListenerList
     * @throws RemoteException
     */
    private void clearUnusedListener(List<WearableListenerItem> wearableListenerList) throws RemoteException {
        if (wearableListenerList == null) {
            return;
        }
        Iterator<WearableListenerItem> iterator = wearableListenerList.iterator();
        while (iterator.hasNext()) {
            WearableListenerItem item = iterator.next();
            try {
                IWearableListener listener = item.getListener();
                if (listener != null) {
                    listener.id();
                }
            } catch (DeadObjectException e) {
                wearableListenerList.remove(item);

                LogTool.e(TAG, "移除已死掉的回调接口");
            } catch (Exception e) {
                LogTool.e(TAG, "Exception", e);
            }
        }
    }

    /**
     * 判断RFCOMM Service是否存在
     *
     * @return
     */
    private boolean isRfcommServiceAvailable() {
        return BleUtil.isServiceRunning(this, "com.cms.android.wearable.service.impl.RFCommService");
    }

    private boolean isBLEServiceAvailable() {
        return mBLEState == BluetoothProfile.STATE_CONNECTED;
    }

    public class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (BluetoothAdapter.STATE_ON == state) {
                    Log.e(TAG, "检测到蓝牙已经打开");
                } else if (BluetoothAdapter.STATE_OFF == state) {
                    LogTool.e(TAG, "检测到蓝牙已经关闭");
                    resetBleServer();
                }
            }
        }
    }

    private void onPeerConnected(NodeHolder nodeHolder) {
        synchronized (mNodeWearableListenerList) {
            LogTool.i(
                    TAG,
                    String.format("手表端：遍历所有NodeListener接口,发送到上层,建立连接 -> nodeId = %s and displayName = %s.",
                            nodeHolder.getId(), nodeHolder.getDisplayName()));
            nodeMap.put(nodeHolder.getId(), nodeHolder);
            for (WearableListenerItem listener : mNodeWearableListenerList) {
                LogTool.i(
                        TAG,
                        "手表端：通知上层onPeerConnected, node =  " + nodeHolder.getId() + ", displayName = "
                                + nodeHolder.getDisplayName());
                try {
                    listener.getListener().onPeerConnected(nodeHolder);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onPeerDisconnected(NodeHolder nodeHolder) {
        LogTool.i(
                TAG,
                String.format("手表端：遍历所有NodeListener接口,发送到上层 ,连接失败-> nodeId = %s and displayName = %s.",
                        nodeHolder.getId(), nodeHolder.getDisplayName()));
        nodeMap.remove(nodeHolder.getId());
        for (WearableListenerItem listener : mNodeWearableListenerList) {
            LogTool.i(TAG, "手表端：通知上层onPeerDisconnected, node =  " + nodeHolder.getId() + ", displayName = "
                    + nodeHolder.getDisplayName());
            try {
                listener.getListener().onPeerDisconnected(nodeHolder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private String getDeviceId() {
        TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String deviceId = mTm.getDeviceId();

        LogTool.d(TAG, "deviceId = " + deviceId);
        return deviceId;
    }

    /**
     * 转移队列中的数据
     */
    private void drainQueue() {
        if (mInQueue == null || mInQueue.isEmpty()) {
            mInQueue = new PriorityBlockingQueue<>();
        } else {
            PriorityBlockingQueue<QueuePriorityTask> newInQueue = new PriorityBlockingQueue<>();
            mInQueue.drainTo(newInQueue);
            mInQueue = newInQueue;
        }
    }


    /**
     * 发送接收同步时间广播
     *
     * @param time
     */
    private void sendSyncTimeBroadcast(long time, int reason) {
        Intent intent = new Intent("com.clouderwatch.synctime");
        intent.putExtra("currenttime", time);
        intent.putExtra("reason", reason);
        sendBroadcast(intent);
    }


    private void broadcastBLeConnectionChangeEvent(BluetoothDevice device, int status) {
        if (device == null && (status != BluetoothProfile.STATE_CONNECTED || status != BluetoothProfile.STATE_DISCONNECTED)) {
            Log.d(TAG, String.format("Bluetooth Device = %s and Status = %s is not invalid!", device.getAddress(), status));
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.clouder.watch.BLEPeripheralService.ACTION_BLE_STATE_CHANGE");
        intent.putExtra("com.clouder.watch.BLEPeripheralService.EXTRA_DEVICE", device);
        intent.putExtra("com.clouder.watch.BLEPeripheralService.EXTRA_STATE", status);
        sendBroadcast(intent);
    }

}