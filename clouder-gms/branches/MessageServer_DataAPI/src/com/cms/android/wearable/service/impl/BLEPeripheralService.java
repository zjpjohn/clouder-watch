/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;

import android.annotation.SuppressLint;
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
import android.telephony.TelephonyManager;
import android.text.TextUtils;

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

/**
 * ClassName: BLEPeripheralService
 * 
 * @description BLEPeripheralService
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
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

	private PriorityBlockingQueue<QueuePriorityTask> mInQueue = new PriorityBlockingQueue<QueuePriorityTask>();

	private Map<String, IParser> mCacheInfoMap = new HashMap<String, IParser>();

	private Map<String, Callback> mCacheCallbackMap = new HashMap<String, Callback>();

	private Map<String, ChildAsset> mCacheAssetDataMap = new HashMap<String, ChildAsset>();

	private Map<String, Node> nodeMap = new HashMap<String, Node>();

	// task cache
	private Map<String, QueuePriorityTask> mCachePriorityTaskMap = new ConcurrentHashMap<String, QueuePriorityTask>();

	private Map<String, List<TransportData>> mCacheTransportDataMap = new ConcurrentHashMap<String, List<TransportData>>();

	private List<WearableListenerItem> mMessageWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<WearableListenerItem> mDataWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<WearableListenerItem> mNodeWearableListenerList = new ArrayList<WearableListenerItem>();

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private BluetoothAdapter mBTAdapter;

	private BluetoothLeAdvertiser mBTAdvertiser;

	private BluetoothGattServer mBTGattServer;

	private AdvertiseCallback mAdvertiseCallback;

	private BluetoothDevice mBLEDevice;

	private InQueueThread mInQueueThread;

	private String mLocalNodeId;

	private boolean isStartSuccess = false;

	/**
	 * BLE current state
	 */
	private int mBLEState = BluetoothGattServer.STATE_DISCONNECTED;

	private IRfcommClientService mRfcommClientService;

	private BroadcastReceiver mBluetoothReceiver;

	/**
	 * RFCOMM connected state
	 */
	public static boolean isRFCOMMConnected = true;

	private static final String RFCOMM_CONNECTED_SUCCESS = "1";

	private static final String RFCOMM_CONNECTED_FAILUE = "0";

	/**
	 * node type
	 */
	private static String NODE_WATCH = "1";

	private static String NODE_PHONE = "2";

	/**
	 * 与RFCOMM Service的连接
	 */
	private ServiceConnection rfcommConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogTool.d(TAG, "[onServiceDisconnected] name:" + name);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogTool.d(TAG, "[onServiceConnected] name:" + name);
			mRfcommClientService = IRfcommClientService.Stub.asInterface(service);
			try {
				mRfcommClientService.registerCallback(rfcommCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private IRfcommClientCallback.Stub rfcommCallback = new IRfcommClientCallback.Stub() {

		@Override
		public void onRFCOMMSocketConnected(BluetoothDevice device) throws RemoteException {
			LogTool.d(TAG, "BluetoothServerSocket connected and tell ble central service can send messages.");
			mInQueueThread = new InQueueThread();
			mInQueueThread.start();

			if (!BLEPeripheralService.isRFCOMMConnected) {
				NodeHolder nodeHolder = new NodeHolder(device.getAddress(),
						device.getName() == null ? device.getAddress() : device.getName());
				onPeerConnected(nodeHolder);

				try {
					LogTool.i(TAG, "RFCOMM断开后重新启动成功");
					// RFCOMM服务重新启动成功,则发送通知告知手机端
					BluetoothGattService service = mBTGattServer.getService(UUID
							.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
					BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID
							.fromString(BleUuid.CHAR_RFCOMM_CONNECTED_STATUS));
					characteristic.setValue(RFCOMM_CONNECTED_SUCCESS
							+ (mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress()));
					LogTool.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
					mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
				} catch (Exception e) {
					LogTool.e(TAG, "notifyCharacteristicChanged error", e);
				}

			}
		}

		@Override
		public void onRFCOMMSocketDisconnected(int cause) throws RemoteException {
			LogTool.d(TAG, "RFCOMM 连接断开,cause = " + cause + " isEmpty = " + mInQueue.isEmpty());
			if (mInQueueThread != null) {
				// RFCOMM断开后,再次发送消息建立发送时,InQueueThread线程还没有销毁,此时InQueue会将该消息消费掉
				drainQueue();
				mInQueueThread.cancel();
				mInQueueThread = null;
				LogTool.d(TAG, "RFCOMM断开后,取消InQueueThread.");
			}
		}

		@Override
		public void onDataReceived(byte[] bytes) throws RemoteException {
			TransportData transportData = TransportParser.dataUnpack(bytes);
			LogTool.e(TAG, "BBBBBBBBB TransportData = " + transportData.toString());
			String uuid = transportData.getUuid();
			long count = transportData.getCount();
			byte protocolType = transportData.getProtocolType();
			String filepath = null;
			if (count == 1) {
				LogTool.d(TAG, "包总数为1,直接进行上传到上层");
				filepath = FileUtil.createFilePath(uuid);
			} else {
				if (!mCacheTransportDataMap.containsKey(uuid)) {
					LogTool.d(TAG, "[onDataReceived] mCacheTransportDataMap不包含该uuid = " + uuid);
					List<TransportData> dataList = new ArrayList<TransportData>();
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
						LogTool.d(TAG, String.format("[onDataReceived] 数据包(uuid = %s)齐全 size = %d count =  %d,合并数据包",
								transportData.getUuid(), dataList.size(), count));
						mCacheTransportDataMap.remove(uuid);
						LogTool.d(TAG, "mCacheTransportDataMap size = " + mCacheTransportDataMap.size());
						filepath = FileUtil.createFilePath(uuid);
					} else {
						return;
					}
				}
			}
			try {
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
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onDataSent(byte[] bytes) throws RemoteException {
			ResponseData response = ResponseParser.dataUnpack(bytes);
			String uuid = response.getUuid();
			switch (response.getStatus()) {
			case ResponseData.RESPONSE_STATUS_SUCCESS:
				LogTool.i(TAG, String.format("传输子包(UUID = %s)发送成功.", uuid));
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
			NodeHolder nodeHolder = new NodeHolder(device.getAddress(), device.getName() == null ? device.getAddress()
					: device.getName());
			onPeerDisconnected(nodeHolder);

			LogTool.i(TAG, "RFCOMM连接失败");
			// RFCOMM连接失败,则发送通知告知手机端
			try {
				BluetoothGattService service = mBTGattServer.getService(UUID
						.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID
						.fromString(BleUuid.CHAR_RFCOMM_CONNECTED_STATUS));
				characteristic.setValue(RFCOMM_CONNECTED_FAILUE
						+ (mLocalNodeId != null ? mLocalNodeId : mBTAdapter.getAddress()));
				LogTool.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
				mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
				BLEPeripheralService.isRFCOMMConnected = false;
			} catch (Exception e) {
				LogTool.e(TAG, "notifyCharacteristicChanged error", e);
			}

		}
	};

	/**
	 * handle message
	 * 
	 * @param filePath
	 *            the cache file's path
	 * @throws RemoteException
	 */
	private void handleMessage(String filePath) throws RemoteException, FileNotFoundException, IOException {
		LogTool.d(TAG, "Received message,prepare to unpack message.");
		MessageData messageData = MessageParser.dataUnpack(new RandomAccessFile(filePath, "rw"));
		MessageEventHolder holder = messageData2Holder(messageData);
		LogTool.d(TAG, "Received message is " + holder.toString());
		String uuid = messageData.getUUID();
		onTotalTransportSuccess(uuid);
		onPostMessageReceived(uuid, holder);
	}

	/**
	 * handle data bytes
	 * 
	 * @param bytes
	 * @throws RemoteException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void handleData(String filePath) throws RemoteException, FileNotFoundException, IOException {
		// 传入参数应该是文件名而不是byte[]数组 将基本信息读取后抛出
		LogTool.d(TAG, "Received data,prepare to unpack data." + filePath);
		DataInfo dataInfo = DataInfoParser.dataUnpack(new RandomAccessFile(filePath, "rw"));
		DataHolder dataHolder = dataInfo2Holder(dataInfo);
		String uuid = dataInfo.getUUID();
		LogTool.d(TAG, "[handleData] UUID = " + uuid);
		onTotalTransportSuccess(uuid);
		onPostDataReceived(uuid, dataHolder);
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
			LogTool.d(TAG, "key->" + key + " asset->" + asset.toString());
			LogTool.e(TAG, "UUID->" + asset.getUuid() + " Index->" + asset.getIndex());
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
		List<DataItemParcelable> dataItems = new ArrayList<DataItemParcelable>();
		dataItems.add(dataItem);
		List<DataEventParcelable> dataEvents = new ArrayList<DataEventParcelable>();
		DataEventParcelable dataEvent = new DataEventParcelable(DataEvent.TYPE_CHANGED, dataItem);
		LogTool.e(TAG, "dataEvent->" + dataEvent.getType());
		dataEvents.add(dataEvent);
		DataHolder dataHolder = new DataHolder(0, dataInfo.getPackageName(), dataItems, dataEvents);
		LogTool.e(TAG, "dataEvent->>>>>" + dataHolder.getDataEvents().get(0).getType());

		return dataHolder;
	}

	/**
	 * Post message to upper layer
	 * 
	 * @param dataHolder
	 */
	private void onPostMessageReceived(String uuid, MessageEventHolder messageEventHolder) {
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
			LogTool.i(TAG, "mLocalNodeId = " + mLocalNodeId + " packageName = " + listener.getPackageName());
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
				LogTool.i(
						TAG,
						"手机端：Node节点不匹配：boolean = "
								+ (listener.getPackageName().equals(messageEventHolder.getPackageName())
										&& TextUtils.isEmpty(mLocalNodeId) && mLocalNodeId.equals(messageEventHolder
										.getSourceNodeId())));
			}
		}
		FileUtil.deleteCacheFile(uuid);
	}

	/**
	 * Post data to upper layer
	 * 
	 * @param dataHolder
	 */
	private void onPostDataReceived(String uuid, DataHolder dataHolder) {
		LogTool.i(TAG, "手表端：遍历所有WearableListener接口,发送数据到上层." + "当前缓存中接口个数为" + mDataWearableListenerList.size());
		for (WearableListenerItem listener : mDataWearableListenerList) {
			if (listener.getPackageName().equals(dataHolder.getPackageName())) {
				try {
					LogTool.i(TAG, "手表端：Node节点匹配,通知上层接收数据");
					Thread thread = Thread.currentThread();
					LogTool.d(
							TAG,
							"[onPostDataReceived] current thread id = " + thread.getId() + " name = "
									+ thread.getName());
					listener.getListener().onDataChanged(dataHolder);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		FileUtil.deleteCacheFile(uuid);
	}

	public static class WearableListenerItem {

		private String packageName;

		private IWearableListener listener;

		public WearableListenerItem() {
			super();
		}

		public WearableListenerItem(String packageName, IWearableListener listener) {
			super();
			LogTool.d(TAG, "WearableListenerItem constructor ->packageName = " + packageName + " listener = "
					+ listener);
			this.packageName = packageName;
			this.listener = listener;
		}

		public String getPackageName() {
			return packageName;
		}

		public void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public IWearableListener getListener() {
			return listener;
		}

		public void setListener(IWearableListener listener) {
			this.listener = listener;
		}

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
			checkCommunicateService();
			// 若是多个应用同时进入sendMessage 那如何处理? TODO
			LogTool.d(TAG, "发送消息当前线程 id = " + Thread.currentThread().getId() + " name = "
					+ Thread.currentThread().getName());
			synchronized (this) {
				String packageName = callback.getPackageName();
				MessageData messageData = new MessageData(getDeviceId(), new byte[] { 1, 0 }, data,
						new Date().getTime(), packageName, path, node);
				try {
					MappedInfo mappedInfo = MessageParser.dataPack(messageData);
					if (mappedInfo == null) {
						LogTool.e(TAG, "MessageParser.dataPack return null!");
						return;
					}
					String messageId = messageData.getUUID();
					// 将已处理-待返回Response消息缓存在Map中
					mCacheInfoMap.put(messageId, messageData);
					mCacheCallbackMap.put(messageId, new Callback(Callback.TYPE_MESSAGE, callback));
					LogTool.i(TAG, "mCacheInfoMap size = " + mCacheInfoMap.size());
					long contentLength = new File(mappedInfo.getFilepath()).length();
					long count = contentLength / TRANSPORT_PACKAGE_LENGTH + 1;
					int packIndex = 0;
					int index = 0;
					long priority = new Date().getTime();
					while (index < contentLength) {
						int readLength = (int) (contentLength - index >= TRANSPORT_PACKAGE_LENGTH ? TRANSPORT_PACKAGE_LENGTH
								: contentLength - index);
						TransportData transportData = new TransportData(packageName, "path = " + path + " node = "
								+ node, messageData.getUUID(), TransportData.PROTOCOL_MESSAGE_TYPE, mappedInfo,
								contentLength, index, readLength, count, packIndex);
						LogTool.d(TAG, "正在分包, " + transportData.toString());
						QueuePriorityTask task = new QueuePriorityTask(priority + packIndex
								- MESSAGE_ORDINARY_PRIORITY_OFFSET, transportData);
						index += readLength;
						putQueue(task);
						packIndex++;
					}
				} catch (Exception e) {
					e.printStackTrace();
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
			// 若是多个应用同时进入putDataItem 那如何处理? TODO
			LogTool.d(TAG, "发送数据当前线程 id = " + Thread.currentThread().getId() + " name = "
					+ Thread.currentThread().getName());
			synchronized (this) {
				String packageName = callback.getPackageName();
				DataInfo dataInfo = new DataInfo(putDataRequest.getVersionCode(), putDataRequest.getUri(),
						putDataRequest.getBundle(), putDataRequest.getData(), getDeviceId(), new Date().getTime(),
						packageName);
				try {
					MappedInfo mappedInfo = DataInfoParser.dataPack(dataInfo);
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
						int readLength = (int) (contentLength - index >= TRANSPORT_PACKAGE_LENGTH ? TRANSPORT_PACKAGE_LENGTH
								: contentLength - index);
						TransportData transportData = new TransportData(packageName,
								putDataRequest.getUri().toString(), dataInfo.getUUID(),
								TransportData.PROTOCOL_DATA_TYPE, mappedInfo, contentLength, index, readLength, count,
								packIndex);
						LogTool.d(TAG, "正在分包, " + transportData.toString());
						QueuePriorityTask task = new QueuePriorityTask(priority + packIndex
								- DATA_ORDINARY_PRIORITY_OFFSET, transportData);
						index += readLength;
						putQueue(task);
						packIndex++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			checkCommunicateService();
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
					// LogTool.d(TAG,
					// "process assets: replace data with FD in asset: " + asset
					// + " read:" + pfds[0]
					// + " write:" + pfds[1]);
					FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new DataCallable(pfds[1], childAsset));
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

			List<Node> nodeList = new ArrayList<Node>();
			for (String key : nodeMap.keySet()) {
				nodeList.add(nodeMap.get(key));
			}

			response.setNodes(nodeList);
			callback.setGetConnectedNodesRsp(response);
			LogTool.d(TAG, "setGetConnectedNodesRsp " + nodeList.size());
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
				LogTool.d(TAG, "HANDLER_CACHE_CHECK = " + sdf.format(new Date()));
				if (mCachePriorityTaskMap != null && mCachePriorityTaskMap.size() > 0) {
					Iterator<String> uuidsIterator = mCachePriorityTaskMap.keySet().iterator();
					while (uuidsIterator.hasNext()) {
						String uuid = uuidsIterator.next();
						QueuePriorityTask task = mCachePriorityTaskMap.get(uuid);
						long currentTime = new Date().getTime();
						if (currentTime - task.getTime() > TIMEOUT) {
							LogTool.w(TAG, String.format("超时机制检测传输子包(UUID = %s)已超时,重新传送.", uuid));
							reputQueue(uuid);
						} else {
							continue;
						}
					}
				}
				LogTool.d(TAG, "重新发送HANDLER_CACHE_CHECK消息 Time = " + sdf.format(new Date()));
				sendEmptyMessageDelayed(HANDLER_CACHE_CHECK, 5000);
				break;

			default:
				break;
			}
		}

	};

	private void checkCommunicateService() throws RemoteException {
		if (!isBLEServiceAvailable()) {
			// 1.判定BLE连接正常
			LogTool.i(TAG, "当前BLE连接断开,等待主设备连接");
			startBLE();
			// 若BLE广播断开的话，则应该重启广播，从设备不应该负责重新连接
		} else if (!isRfcommServiceAvailable() || mRfcommClientService == null) {
			LogTool.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
			bindRfcommService();
		} else if (!mRfcommClientService.isConnected()) {
			LogTool.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
			// RFCOMM服务未启动,则发送通知告知手机端启动
			BluetoothGattService service = mBTGattServer
					.getService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
			BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID
					.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
			characteristic.setValue("start");
			LogTool.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
			mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
		} else if (mInQueueThread == null || mInQueueThread.isCancel) {
			LogTool.i(TAG, "InQueue服务未启动,启动该服务");
			mInQueueThread = new InQueueThread();
			mInQueueThread.start();
		} else {
			LogTool.i(TAG, "服务都已经准备就绪");
		}
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
	 * @param packageName
	 * @param message
	 */
	private void putQueue(QueuePriorityTask task) {
		if (task == null) {
			return;
		}
		// 1.CMS ready，put the task in queue
		// 2.CMS isn't ready，put the task in queue to wait InQueue Thread to
		// handle
		mInQueue.offer(task);
		LogTool.d(TAG, "mInQueue size is " + mInQueue.size());
	}

	/**
	 * reput PriorityTask in queue
	 * 
	 * @param uuid
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
		// 提高重发任务优先级,根据Data和Message区分 重发Message优先级更高,值越小
		LogTool.d(TAG, "reput queue id = " + id + "  uuid = " + uuid);
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
			LogTool.d(TAG, "[handleTotalDataSentSuccess] uuid = " + uuid + " invoke callback");
			Callback callback = mCacheCallbackMap.remove(uuid);
			if (callback != null) {
				switch (callback.getType()) {
				case Callback.TYPE_MESSAGE:
					LogTool.d(TAG, "[handleTotalDataSentSuccess] handle message callback");
					if (mCacheInfoMap.containsKey(uuid)) {
						mCacheInfoMap.remove(uuid);
						SendMessageResponse response = new SendMessageResponse();
						response.setRequestId(1);
						response.setStatusCode(0);
						callback.getCallback().setSendMessageRsp(response);
						LogTool.d(TAG, "[handleTotalDataSentSuccess] invoke setSendMessageRsp");
					}
					break;
				case Callback.TYPE_DATA:
					LogTool.d(TAG, "[handleTotalDataSentSuccess] handle data callback");
					if (mCacheInfoMap.containsKey(uuid)) {
						DataInfo dataInfo = (DataInfo) mCacheInfoMap.remove(uuid);
						Bundle assets = dataInfo.getAssets();
						Bundle bundle = new Bundle();
						// 暂时传空的吧 TODO
						// if (assets != null) {
						// Iterator<String> iterator =
						// assets.keySet().iterator();
						// while (iterator.hasNext()) {
						// String key = iterator.next();
						// Asset currentAsset = (Asset) assets.get(key);
						// DataItemAssetParcelable assetParcelable = new
						// DataItemAssetParcelable(
						// currentAsset.getDigest(), key);
						// bundle.putParcelable(key, assetParcelable);
						// }
						// }
						DataItemParcelable dataItem = new DataItemParcelable(dataInfo.getUri(), bundle,
								dataInfo.getData());
						PutDataResponse putDataResponse = new PutDataResponse(1, 0, dataItem);
						callback.getCallback().setPutDataRsp(putDataResponse);
						LogTool.d(TAG, "[handleTotalDataSentSuccess] invoke setPutDataRsp");

					}
					break;
				default:
					break;
				}
			} else {
				// already handle the timeout exception so do nothing
			}
			FileUtil.deleteCacheFile(uuid);
		} else {
			LogTool.e(TAG, "[handleTotalDataSentSuccess] uuid = " + uuid + " failed invoke callback");
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LogTool.d(TAG, "onCreate...");
		String address = BluetoothAdapter.getDefaultAdapter().getAddress();
		LogTool.e(TAG, "current mac address = " + address);
		init();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LogTool.d(TAG, "onDestroy...");

		if (mBluetoothReceiver != null) {
			unregisterReceiver(mBluetoothReceiver);
			mBluetoothReceiver = null;
		}

		unbindRfcommService();
		stopCloudWatchAdvertising();

	}

	private void init() {

		bindRfcommService();

		mBluetoothReceiver = new BluetoothReceiver();
		IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBluetoothReceiver, filter);

		/**
		 * wake up timeout scan
		 */
		LogTool.e(TAG, "start HANDLER_CACHE_CHECK");
		mCacheHandler.sendEmptyMessageDelayed(HANDLER_CACHE_CHECK, 1000);
	}

	/**
	 * start cloud watch advertising
	 */
	private void startBLE() {
		if (!BleUtil.isBLESupported(this)) {
			LogTool.e(TAG, "BLE is not supported");
			return;
		}

		// BT check
		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}

		if (mBTAdapter == null) {
			LogTool.e(TAG, "Bluetooth unavailable");
			// Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!mBTAdapter.isEnabled()) {
			LogTool.e(TAG, "Bluetooth disenable,enabling.");
			mBTAdapter.enable();
			return;
		}

		startCloudWatchAdvertising();

	}

	private void startCloudWatchAdvertising() {
		LogTool.d(TAG, "Bluetooth is ready,start CloudWatch Advertising.");
		if (mBTAdvertiser == null) {
			mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
		}
		LogTool.d(TAG, "mBTAdapter = " + mBTAdapter + " mBTAdvertiser = " + mBTAdvertiser + " isStartSuccess = "
				+ isStartSuccess + " mBTGattServer = " + mBTGattServer);
		if (mBTAdvertiser != null && (!isStartSuccess || mBTGattServer == null)) {
			LogTool.d(TAG, "startCloudWatchAdvertising Open gatt Server");
			mBTGattServer = BleUtil.getManager(this).openGattServer(this, new BluetoothGattServerCallback() {

				@Override
				public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
						BluetoothGattCharacteristic characteristic) {
					super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
					LogTool.d(
							TAG,
							String.format(
									"onCharacteristicReadRequest device = %s, requestId = %d, offset = %d, characteristic = %s.",
									device.toString(), requestId, offset, characteristic));
				}

				@SuppressLint("SimpleDateFormat")
				@Override
				public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
						BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
						int offset, byte[] value) {
					super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
							responseNeeded, offset, value);
					LogTool.d(
							TAG,
							String.format(
									"onCharacteristicWriteRequest device = %s, requestId = %d, characteristic = %s, value = %s.",
									device.toString(), requestId, characteristic.getUuid(), new String(value)));

					if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING))) {
						int status = -1;
						String mac = "";
						String content = new String(value);
						if (content.contains("&")) {
							String[] stringArray = content.split("&");
							status = Integer.parseInt(stringArray[0]);
							mac = stringArray[1];
						} else if (content.contains("#")) {
						} else {
							LogTool.e(TAG, String.format("格式错误 %s,应该是type&Bluetooth MAC address", content));
							return;
						}
						LogTool.d(TAG, "Cloud watch rfcomm command is " + new String(value));
						// 将蓝牙地址作为启动RFCOMM标志位
						try {
							if (isRfcommServiceAvailable() && mRfcommClientService != null) {
								if (status == RFCOMMClientService.RFCOMM_CONNECT_READY
										&& BleUtil.isBluetoothAddress(mac)) {
									LogTool.d(TAG, "手表端启动Socket连接服务");
									mRfcommClientService.setStatus(RFCOMMClientService.RFCOMM_CONNECT_READY);
									mRfcommClientService.start(mac);
								} else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXPIRED
										|| status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_STOP
										|| status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXCEPTION) {
									mRfcommClientService.setStatus(status);
									LogTool.d(TAG, "手表端：收到指令为 " + status);
								} else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_RESTART) {
									LogTool.d(TAG, "手表端：手机端服务启动");
								} else {
									LogTool.e(TAG, "Unknow status&MAC address");
								}
							} else {
								// 应该启动rfcomm服务并且根据所给地址建立rfcomm通道 TODO
								bindRfcommService();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
						}

					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING))) {
						String content = new String(value);
						LogTool.d(TAG, "Cloud watch heartbeat or disconnect command is " + new String(value));
						if (content.contains("#")) {
							String[] stringArray = content.split("#");
							int type = Integer.parseInt(stringArray[0]);
							String command = stringArray[1];
							switch (type) {
							case 1:
								int heartBeat = Integer.parseInt(command);
								LogTool.d(TAG, "接收到心跳包 value = " + heartBeat);
								if (mBTGattServer != null) {
									BluetoothGattService service = mBTGattServer.getService(UUID
											.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
									BluetoothGattCharacteristic heartbeatCharacteristic = service
											.getCharacteristic(UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING));
									heartBeat++;
									LogTool.d(TAG, "设置心跳包 value = " + heartBeat);
									heartbeatCharacteristic.setValue(heartBeat + "");
									boolean isSuccess = mBTGattServer.notifyCharacteristicChanged(mBLEDevice,
											heartbeatCharacteristic, false);
									LogTool.d(TAG, "notifyCharacteristicChanged isSuccess = " + isSuccess
											+ " heartbeat  = " + new String(heartbeatCharacteristic.getValue()));
								}
								break;
							case 2:
								// LogTool.d(TAG, "current commad is " +
								// command);
								// if (command.equals("disconnect")) {
								// LogTool.d(TAG, "save shared toggle = " +
								// command);
								// Utils.saveSharedBondToggle(BLEPeripheralService.this,
								// false);
								// } else {
								// LogTool.e(TAG, "Unknown command");
								// }
								break;
							default:
								break;
							}
						} else {
							LogTool.e(TAG, String.format("格式错误 %s,应该是type#commad", content));
							return;
						}
					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_CURRENT_TIME))) {
						if (value != null && value.length == 10) {
							Calendar cal = Calendar.getInstance();
							int l1 = value[0] < 0 ? value[0] + 256 : value[0];
							int l0 = value[1] < 0 ? value[1] + 256 : value[1];
							int year = (l0 << 8) | l1;
							cal.set(Calendar.YEAR, year);

							cal.set(Calendar.MONTH, value[2]);
							cal.set(Calendar.DAY_OF_MONTH, value[3]);
							LogTool.d(TAG, "value[0]:" + value[0] + " value[1]" + value[1] + " value[2]:" + value[2]
									+ " value[3]:" + value[3]);
							cal.set(Calendar.HOUR_OF_DAY, value[4]);
							cal.set(Calendar.MINUTE, value[5]);
							cal.set(Calendar.SECOND, value[6]);
							cal.set(Calendar.DAY_OF_WEEK, value[7]);
							// 无符号byte转化 MILLISECOND分为256份
							int millisecond = value[8];
							millisecond = value[8] & 0xff;
							cal.set(Calendar.MILLISECOND, (int) (1000 * millisecond / 256f));

							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
							LogTool.i(TAG, "手表端：时间同步 调整时间为" + sdf.format(cal.getTime()));
							// 将调整的时间通过广播方式发送出去
							long currentTime = cal.getTime().getTime();
							sendSyncTimeBroadcast(currentTime, value[8]);
							try {
								boolean isSuccess = SystemClock.setCurrentTimeMillis(currentTime);
								LogTool.e(TAG, "广播并设置当前时间 currentTime = " + sdf.format(new Date(currentTime))
										+ " isSuccess = " + isSuccess);
							} catch (Exception e) {
								LogTool.e(TAG, "Exception", e);
							}

						} else {
							LogTool.e(TAG, "手表端：时间同步,格式错误");
						}
					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_NODE))) {
						if (value != null) {
							String nodeType = new String(value).substring(0, 1);
							String nodeId = new String(value).substring(1);
							if (nodeType.equals(NODE_PHONE)) {
								NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId
										: device.getName());
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
						// int heartBeat = Integer.parseInt(new String(value));
						// LogTool.d(TAG, "接收到心跳包 value = " + heartBeat);
						// if (mBTGattServer != null) {
						// BluetoothGattService service =
						// mBTGattServer.getService(UUID
						// .fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
						// BluetoothGattCharacteristic heartbeatCharacteristic =
						// service.getCharacteristic(UUID
						// .fromString(BleUuid.CHAR_SYSTEM_ID_STRING));
						// heartBeat++;
						// LogTool.d(TAG, "设置心跳包 value = " + heartBeat);
						// heartbeatCharacteristic.setValue(heartBeat + "");
						// boolean isSuccess =
						// mBTGattServer.notifyCharacteristicChanged(mBLEDevice,
						// heartbeatCharacteristic, false);
						// LogTool.d(TAG,
						// "notifyCharacteristicChanged isSuccess = " +
						// isSuccess + " heartbeat  = "
						// + new String(heartbeatCharacteristic.getValue()));
						// }
					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_SERIAL_NUMBER_STRING))) {
						String command = new String(value);
						LogTool.d(TAG, "current commad is " + command);
						if (command.equals("disconnect")) {
							LogTool.d(TAG, "save shared toggle = " + command);
							Utils.saveSharedBondToggle(BLEPeripheralService.this, false);
						} else {
							LogTool.e(TAG, "Unknown command");
						}
					} else {
						LogTool.e(TAG, "无匹配Characteristic");
					}
					mBTGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[] {});
				}

				@Override
				public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
					super.onConnectionStateChange(device, status, newState);
					LogTool.d(TAG, String.format("onConnectionStateChange device = %s, status = %d, newState = %d.",
							device, status, newState));
					mBLEState = newState;
					if (BluetoothGattServer.STATE_CONNECTED == newState) {
						// 连接成功
						mBLEDevice = device;
						Utils.saveSharedBondToggle(BLEPeripheralService.this, true);
						LogTool.e(TAG, "BLE服务成功连接");
					} else {
						// 连接失败
						mBLEDevice = null;
						LogTool.e(TAG, "BLE服务断开连接");

						NodeHolder nodeHolder = new NodeHolder(device.getAddress(), device.getName() == null ? device
								.getAddress() : device.getName());
						if (isRFCOMMConnected) {
							onPeerDisconnected(nodeHolder);
						}
					}
				}

				@Override
				public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
						BluetoothGattDescriptor descriptor) {
					super.onDescriptorReadRequest(device, requestId, offset, descriptor);
				}

				@Override
				public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
						BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset,
						byte[] value) {
					super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
							offset, value);
				}

				@Override
				public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
					super.onExecuteWrite(device, requestId, execute);
					LogTool.d(TAG, String.format("onExecuteWrite device = %s, requestId = %d, execute = %s.", device,
							requestId, execute + ""));
				}

				@Override
				public void onNotificationSent(BluetoothDevice device, int status) {
					super.onNotificationSent(device, status);
					LogTool.d(TAG, String.format("onNotificationSent device = %s, status = %d.", device, status));
				}

				@Override
				public void onServiceAdded(int status, BluetoothGattService service) {
					super.onServiceAdded(status, service);
					if (status == BluetoothGatt.GATT_SUCCESS) {
						LogTool.d(TAG, "onServiceAdded status=GATT_SUCCESS service=" + service.getUuid().toString()
								+ " size=" + service.getCharacteristics().size());
					} else {
						LogTool.d(TAG, "onServiceAdded status != GATT_SUCCESS");
					}
				}

			});

			setupCloudWatchService();

			mAdvertiseCallback = new AdvertiseCallback() {

				@Override
				public void onStartFailure(int errorCode) {
					super.onStartFailure(errorCode);
					LogTool.e(TAG, "广播服务启动失败");
					isStartSuccess = false;
				}

				@Override
				public void onStartSuccess(AdvertiseSettings settingsInEffect) {
					super.onStartSuccess(settingsInEffect);
					LogTool.e(TAG, "广播服务启动成功");
					isStartSuccess = true;
				}
			};

			mBTAdvertiser.startAdvertising(BleUtil.createAdvSettings(true, 0), BleUtil.createCloudWatchAdvertiseData(),
					mAdvertiseCallback);
		} else {
			if (isStartSuccess) {
				LogTool.d(TAG, "alreay start CloudWatch Advertising.");
			} else {
				LogTool.d(TAG, "startCloudWatchAdvertising failed.");
			}

		}

	}

	private void stopCloudWatchAdvertising() {
		if (mBTGattServer != null) {
			mBTGattServer.clearServices();
			mBTGattServer.close();
			mBTGattServer = null;
		}
		if (mBTAdvertiser != null) {
			mBTAdvertiser.stopAdvertising(mAdvertiseCallback);
			mBTAdvertiser = null;
		}

		isStartSuccess = false;
	}

	private void setupCloudWatchService() {

		/**
		 * For Now 暂时将Device Info的协议替代为Cloud Watch
		 */
		{ // device information
			BluetoothGattService dis = new BluetoothGattService(UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
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

			mBTGattServer.addService(dis);
		}

		{
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

			mBTGattServer.addService(cloudService);
		}

		{
			BluetoothGattService timeService = new BluetoothGattService(UUID.fromString(BleUuid.SERVICE_CURRENT_TIME),
					BluetoothGattService.SERVICE_TYPE_PRIMARY);
			BluetoothGattCharacteristic ctsc = new BluetoothGattCharacteristic(
					UUID.fromString(BleUuid.CHAR_CURRENT_TIME), BluetoothGattCharacteristic.PROPERTY_READ
							| BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

			timeService.addCharacteristic(ctsc);

			mBTGattServer.addService(timeService);
		}

	}

	/**
	 * 绑定Rfcomm服务连接
	 */
	private void bindRfcommService() {
		if (!isRfcommServiceAvailable()) {
			Intent intent = new Intent(this, RFCOMMClientService.class);
			bindService(intent, rfcommConnection, Context.BIND_AUTO_CREATE);
		} else {
			LogTool.d(TAG, "RFCOMM Service is already alive.");
		}
	}

	private void unbindRfcommService() {
		if (rfcommConnection != null) {
			unbindService(rfcommConnection);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogTool.d(TAG, "onStartCommand...");
		startBLE();
		return START_REDELIVER_INTENT;
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
				iterator.remove();
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
		return BleUtil.isServiceRunning(this, "com.cms.android.wearable.service.impl.RFCOMMClientService");
	}

	private boolean isBLEServiceAvailable() {
		return mBLEState == BluetoothProfile.STATE_CONNECTED;
	}

	public enum LinkDecoderState {
		ReadA,
		ReadBCDEFG
	}

	public class BluetoothReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				LogTool.d(TAG, "Bluetooth state changed(ON=12,OFF=10,TURNING_ON=11,TURNING_OFF=13),current state = "
						+ state);
				if (BluetoothAdapter.STATE_ON == state) {
					startCloudWatchAdvertising();
				} else if (BluetoothAdapter.STATE_OFF == state) {
					LogTool.e(TAG, "Bluetooth close");
					isStartSuccess = false;
				}
			}
		}
	}

	public static class Callback {

		public static final int TYPE_MESSAGE = 1;

		public static final int TYPE_DATA = 2;

		private int type;

		private IBLEPeripheralCallback callback;

		public Callback(int type, IBLEPeripheralCallback callback) {
			super();
			this.type = type;
			this.callback = callback;
		}

		public int getType() {
			return type;
		}

		public IBLEPeripheralCallback getCallback() {
			return callback;
		}

	}

	public static class MappedInfo {

		private String filepath;

		private MappedByteBuffer buffer;

		public MappedInfo(String filepath, MappedByteBuffer buffer) {
			super();
			this.filepath = filepath;
			this.buffer = buffer;
		}

		public String getFilepath() {
			return filepath;
		}

		public void setFilepath(String filepath) {
			this.filepath = filepath;
		}

		public MappedByteBuffer getBuffer() {
			return buffer;
		}

		public void setBuffer(MappedByteBuffer buffer) {
			this.buffer = buffer;
		}

	}

	public static class QueuePriorityTask implements Comparable<QueuePriorityTask> {

		private TransportData data;

		private long priority;

		private long time;

		private int repeat;

		private QueuePriorityTask(long priority, TransportData data) {
			super();
			this.priority = priority;
			this.data = data;
			this.time = new Date().getTime();
			this.repeat = 0;
		}

		public void setPriority(long priority) {
			this.priority = priority;
		}

		public long getPriority() {
			return priority;
		}

		public TransportData getData() {
			return data;
		}

		@Override
		public int compareTo(QueuePriorityTask another) {
			// 数字小，优先级高
			return this.priority > another.priority ? 1 : this.priority < another.priority ? -1 : 0;
		}

		public long getTime() {
			return time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public int getRepeat() {
			return repeat;
		}

		public void setRepeat(int repeat) {
			this.repeat = repeat;
		}

	}

	private class InQueueThread extends Thread {

		private volatile boolean isCancel = false;

		@Override
		public void run() {
			while (!isCancel) {
				LogTool.i(TAG,
						"手表端：启动InQueueThread,接收队列中的消息 isCancel = " + isCancel + " mInQueue size : " + mInQueue.size());
				while (!isCancel) {
					try {
						LogTool.d(TAG, "手表端：InQueueThread等待接收手表上层发出的的消息...");
						QueuePriorityTask task = mInQueue.take();
						LogTool.d(TAG, "手表端： 剩余处理的Task size = " + mInQueue.size());
						String id = task.getData().getId();
						task.setTime(new Date().getTime());
						mCachePriorityTaskMap.put(id, task);
						TransportData data = task.getData();
						byte[] bytes = TransportParser.dataPack(data);
						LogTool.e(TAG, "发送传输包 size = " + bytes.length + " TransportData = " + data.toString());
						if (!mRfcommClientService.write(bytes)) {
							LogTool.e(TAG, String.format("传输子包(UUID = %s)发送失败,重新发送", id));
							// reset before repeat
							resetCachePriority(task);
							int repeat = task.getRepeat();
							task.setRepeat(++repeat);
							putQueue(task);
						}
					} catch (InterruptedException e) {
						if (mInQueueThread != null) {
							drainQueue();
							mInQueueThread.cancel();
							mInQueue = null;
						}
						LogTool.d(TAG, "InterruptedException Queue remained size : " + mInQueue.size(), e);
						break;
					} catch (Exception e) {
						if (mInQueueThread != null) {
							drainQueue();
							mInQueueThread.cancel();
							mInQueue = null;
						}
						LogTool.d(TAG, "Exception Queue remained size " + mInQueue.size(), e);
						break;
					}
				}
			}
		}

		public void cancel() {
			isCancel = true;
		}
	}

	// private void setSendMessageRsp(QueueItem item, boolean isSuccess) throws
	// RemoteException {
	// int statusCode = isSuccess ? 0 : 8;// CommonStatusCodes中8表示内部错误
	// IBLEPeripheralCallback callback = item.getCallback();
	// SendMessageResponse response = new SendMessageResponse();
	// response.setRequestId(1);
	// response.setStatusCode(statusCode);
	// LogTool.d(TAG, "手表端：通知上层消息已处理 isSuccess = " + isSuccess);
	// callback.setSendMessageRsp(response);
	// }

	private void onPeerConnected(NodeHolder nodeHolder) {
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
			mInQueue = new PriorityBlockingQueue<QueuePriorityTask>();
		} else {
			PriorityBlockingQueue<QueuePriorityTask> newInQueue = new PriorityBlockingQueue<QueuePriorityTask>();
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

}