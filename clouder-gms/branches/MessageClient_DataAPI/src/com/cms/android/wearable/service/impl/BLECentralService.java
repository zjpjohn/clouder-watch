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
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
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
import com.cms.android.wearable.service.codec.CRCUtil;
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

public class BLECentralService extends Service {

	private static final String TAG = "BLECentralService";

	private static final int TRANSPORT_PACKAGE_LENGTH = 4196;

	// 通过sleep 1ms可以保证对同byte数组封成多个子包后,后面的包设置时间更大,优先级更低
	// 普通Message优先级为time - 6 * 60 * 60 * 1000 普通Data优先级为time(当前时间)
	// 失败的Message优先级为time - 24 * 60 * 60 * 1000 失败的Data优先级为time - 12
	// *
	// 60 * 60 * 1000
	// 1方面保证失败能够优先执行,2能保证失败Message优先失败Data先执行
	private static final int MESSAGE_ORDINARY_PRIORITY_OFFSET = 12 * 60 * 60 * 1000;

	private static final int MESSAGE_FAILED_PRIORITY_OFFSET = 24 * 60 * 60 * 1000;

	private static final int DATA_ORDINARY_PRIORITY_OFFSET = 0;

	private static final int DATA_FAILED_PRIORITY_OFFSET = 6 * 60 * 60 * 1000;

	private static final int HANDLER_CACHE_CHECK = 8;

	private static final int TIMEOUT = 10 * 1000;

	// 10秒后停止查找搜索.
	private static final long SCAN_PERIOD = 10000;

	public static final String BLE_CENTRAL_SERVICE_ACTION = "com.hoperun.ble.central.service";

	public static final String BLE_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.BLECentralService";

	public static final String RFCOMM_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.RFCOMMServerService";

	private boolean mScanning = false;

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private PriorityBlockingQueue<QueuePriorityTask> mInQueue = new PriorityBlockingQueue<QueuePriorityTask>();

	private Map<String, IParser> mCacheInfoMap = new HashMap<String, IParser>();

	private Map<String, Callback> mCacheCallbackMap = new HashMap<String, Callback>();

	// task cache
	private Map<String, QueuePriorityTask> mCachePriorityTaskMap = new ConcurrentHashMap<String, QueuePriorityTask>();

	private Map<String, List<TransportData>> mCacheTransportDataMap = new ConcurrentHashMap<String, List<TransportData>>();

	private Map<String, ChildAsset> mCacheAssetDataMap = new ConcurrentHashMap<String, ChildAsset>();

	private Map<String, Node> nodeMap = new HashMap<String, Node>();

	private static final int TYPE_MESSAGE_LISTENER = 1;

	private static final int TYPE_DATA_LISTENER = 2;

	private static final int TYPE_NODE_LISTENER = 3;

	private List<WearableListenerItem> mMessageWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<WearableListenerItem> mDataWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<WearableListenerItem> mNodeWearableListenerList = new ArrayList<WearableListenerItem>();

	private BluetoothAdapter mBluetoothAdapter;

	/**
	 * BLE address
	 */
	private String mBLEAddress;

	private BluetoothGatt mBluetoothGatt;

	/**
	 * Heartbeat
	 */
	private int mHeartbeat = 0;

	private boolean isBLERsp = false;

	/**
	 * BLE reconnect count
	 */
	private int bleReconnectCount = 0;

	/**
	 * 判断当前连接服务是否支持Cloud Watch Service
	 */
	private boolean isCloudWatchServiceAvailabled = false;

	/**
	 * BLE current state
	 */
	private int mBLEState = BluetoothProfile.STATE_DISCONNECTED;

	private IRfcommServerService mRfcommService;

	private InQueueThread mInQueueThread;

	private DateTimeReceiver mDateTimeReceiver;

	private BroadcastReceiver mBluetoothReceiver;

	private Intent mLastIntent;

	/**
	 * RFCOMM connected state
	 */
	private static boolean isRFCOMMConnected = true;

	private static final String RFCOMM_CONNECTED_SUCCESS = "1";

	/**
	 * node type
	 */
	private static String NODE_WATCH = "1";

	private static String NODE_PHONE = "2";

	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			LogTool.i(TAG, String.format("[onConnectionStateChange] status is %d, state from %d to %d.", status,
					mBLEState, newState));
			mBLEState = newState;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				LogTool.e(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				if (mBluetoothGatt != null) {
					LogTool.i(TAG, "Attempting to discovery service :" + mBluetoothGatt.discoverServices());
				} else {
					LogTool.i(TAG, "onConnectionStateChange gatt == null");
				}
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				LogTool.e(TAG, "Disconnected from GATT server.");

				BluetoothDevice device = gatt.getDevice();
				String nodeId = device.getAddress();
				NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId : device.getName());

				if (isRFCOMMConnected) {
					onPeerDisconnected(nodeHolder);
				}

				boolean toggle = Utils.getShardBondToggle(BLECentralService.this);
				if (toggle) {
					if (bleReconnectCount == 0) {
						LogTool.d(TAG, "BLE try to reconnect immediately");
						handleDisconnect();
					} else {
						int interval = bleReconnectCount * 5000 > 15 * 1000 ? 15 * 1000 : bleReconnectCount * 5000;
						mCacheHandler.postDelayed(new Runnable() {

							@SuppressLint("SimpleDateFormat")
							@Override
							public void run() {
								if (!isBLERsp) {
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									LogTool.d(TAG, "BLE try to reconnect count = " + bleReconnectCount + " time  = "
											+ sdf.format(new Date()));
									handleDisconnect();
								}
							}
						}, interval);
					}
				} else {
					LogTool.d(TAG, "toggle is false and will not connect BLE.");
				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				LogTool.d(TAG, "[onServicesDiscovered] status :" + status);
				isCloudWatchServiceAvailabled = false;
				try {
					displayGattServices(mBluetoothGatt.getServices());
					// 检验是否支持Cloud Watch Service 若是不支持Clouder
					// Watch这一套BLE通信，是否需要给予提示？毕竟下面已经走不下去了
					checkCloudWatchServiceSupported();

					BluetoothDevice device = gatt.getDevice();
					BluetoothGattService service = mBluetoothGatt.getService(UUID
							.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
					if (null != service) {
						String nodeId = device.getAddress();
						NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId
								: device.getName());
						onPeerConnected(nodeHolder);
						isRFCOMMConnected = true;
						String value = NODE_WATCH + nodeId;
						boolean flag = writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_NODE,
								value);
						LogTool.d(TAG, "writeCharacteristic : " + flag);
					}

					isCloudWatchServiceAvailabled = true;
				} catch (Exception e) {
					isCloudWatchServiceAvailabled = false;
					e.printStackTrace();
				}
			} else {
				LogTool.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		private void displayGattServices(List<BluetoothGattService> gattServices) {
			LogTool.e(TAG, "displayGattServices...");
			if (gattServices == null)
				return;
			// Loops through available GATT Services.
			for (BluetoothGattService gattService : gattServices) {
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				LogTool.d(TAG, "Service UUID:" + gattService.getUuid() + " Characteristics:"
						+ (gattCharacteristics == null ? 0 : gattCharacteristics.size()));

				// Loops through available Characteristics.
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					LogTool.i(TAG, "Characteristic UUID:" + gattCharacteristic.getUuid());
				}
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			LogTool.w(TAG, "onCharacteristicChanged UUID:" + characteristic.getUuid().toString() + " value:"
					+ new String(characteristic.getValue()));
			if (BleUuid.CHAR_MANUFACTURER_NAME_STRING.equals(characteristic.getUuid().toString())) {
				String command = new String(characteristic.getValue());
				try {
					if (command.equals("start")) {
						LogTool.i(TAG, "手表端通知手机端启动RFCOMM Server服务");
						if (!isRfcommServiceAvailable() || mRfcommService == null) {
							LogTool.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
							bindRfcommService();
						} else if (!mRfcommService.isConnected() && !mRfcommService.isConnecting()) {
							LogTool.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
							mRfcommService.start();
						} else if (mInQueueThread == null || mInQueueThread.isCancel) {
							LogTool.i(TAG, "InQueue服务未启动,启动该服务");
							mInQueueThread = new InQueueThread();
							mInQueueThread.start();
						}
					} else if (command.equals("stop")) {
						// TODO
						LogTool.i(TAG, "手表端通知手机端停止RFCOMM Server Socket");
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else if (BleUuid.CHAR_RFCOMM_CONNECTED_STATUS.equals(characteristic.getUuid().toString())) {
				String value = new String(characteristic.getValue());
				String status = value.substring(0, 1);
				String nodeId = value.substring(1);
				NodeHolder nodeHolder = new NodeHolder(nodeId, nodeId);
				if (status.equals(RFCOMM_CONNECTED_SUCCESS)) {
					onPeerConnected(nodeHolder);
					isRFCOMMConnected = true;
				} else {
					onPeerDisconnected(nodeHolder);
					isRFCOMMConnected = false;
				}
			} else if (BleUuid.CHAR_MODEL_NUMBER_STRING.equals(characteristic.getUuid().toString())) {
				int newHeartBeat = Integer.parseInt(new String(characteristic.getValue()));
				LogTool.d(TAG, "手表侧返回 newHeartBeat = " + newHeartBeat + " 当前Heartbeat = " + mHeartbeat);
				if (newHeartBeat > mHeartbeat) {
					isBLERsp = true;
					mHeartbeat = newHeartBeat;
					/**
					 * 只有返回心跳包正常才认为连接OK
					 */
					bleReconnectCount = 0;
					LogTool.d(TAG, "BLE心跳正常");
				}
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			String value = new String(characteristic.getValue());
			LogTool.i(TAG, "onCharacteristicWrite UUID:" + characteristic.getUuid().toString() + " value:" + value
					+ " status:" + status);
			if (characteristic.getUuid().toString().indexOf(BleUuid.CHAR_NODE) > -1
					&& value.substring(0, 1).equals(NODE_WATCH)) {
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_NODE,
						NODE_PHONE + mBluetoothAdapter.getAddress());
				LogTool.i(TAG, "local mac" + mBluetoothAdapter.getAddress());
			} else if (characteristic.getUuid().toString().indexOf(BleUuid.CHAR_NODE) > -1
					&& value.substring(0, 1).equals(NODE_PHONE)) {
				startHeartBeatTask();
				boolean isSuccess = writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION,
						BleUuid.CHAR_MODEL_NUMBER_STRING, "1#" + mHeartbeat);
				LogTool.d(TAG, "同步心跳 isSuccess = " + isSuccess);
			} else if (characteristic.getUuid().toString().indexOf(BleUuid.CHAR_SYSTEM_ID_STRING) > -1) {
				BluetoothGattService timeService = mBluetoothGatt.getService(UUID
						.fromString(BleUuid.SERVICE_CURRENT_TIME));
				if (timeService != null) {
					syncLocalTime(new Date().getTime(), 0x01);
				} else {
					LogTool.d(TAG, "timeService == null,手机端不支持同步时间服务");
				}
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorRead(gatt, descriptor, status);
			LogTool.w(TAG, "onDescriptorRead :" + status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			LogTool.w(TAG, "onDescriptorWrite :" + status);
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			super.onReliableWriteCompleted(gatt, status);
			LogTool.w(TAG, "onReliableWriteCompleted :" + status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
			LogTool.w(TAG, "onReadRemoteRssi :" + status);
		}

	};

	private void startHeartBeatTask() {
		LogTool.d(TAG, "startHeartBeatTask...");
		isBLERsp = false;
		Looper looper = Looper.myLooper();
		if (looper == null) {
			Looper.prepare();
		}
		mCacheHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (!isBLERsp) {
					// 规定时间内无答复
					LogTool.e(TAG, "BLE response timeout,disconnect BLE.");
					disconnectBLE();
				} else {
					// 规定时间内有答复
					LogTool.i(TAG, "BLE response OK in time.");
				}

			}
		}, 5000);

	}

	// Device scan callback.
	// private BluetoothAdapter.LeScanCallback mLeScanCallback = new
	// BluetoothAdapter.LeScanCallback() {
	//
	// @Override
	// public void onLeScan(final BluetoothDevice device, int rssi, byte[]
	// scanRecord) {
	// String sharedBondName = Utils.getShardBondName(BLECentralService.this);
	// String deviceName = device.getName();
	// LogTool.e(TAG, "[onLeScan] Device Name = " + deviceName +
	// " sharedBondName = " + sharedBondName
	// + " Address = " + device.getAddress());
	// if (deviceName != null &&
	// deviceName.trim().equals(sharedBondName.trim())) {
	// scanLeDevice(false);
	// LogTool.i(TAG, String.format("尝试连接BLE,地址为: %s.", device.getAddress()));
	// connectBLE(device.getAddress());
	// }
	// }
	// };

	// private HashMap<String, IBLECentralCallback> callBackMap = new
	// HashMap<String, IBLECentralCallback>();

	private IBLECentralService.Stub mStub = new IBLECentralService.Stub() {

		@Override
		public void connect(String address) throws RemoteException {
			LogTool.i(TAG, String.format("尝试连接BLE,地址为: %s.", address));
			connectBLE(address);
		}

		@Override
		public void disconnect() throws RemoteException {
			LogTool.i(TAG, "disconnect from BLE");
			disconnectBLE();
			closeBLE();
		}

		@Override
		public void registerCallback(String packageName, IBLECentralCallback callback) throws RemoteException {
			LogTool.d(TAG, String.format("[registerCallback] packageName: %s.", packageName));
			// if (callBackMap.containsKey(packageName)) {
			// callBackMap.remove(callBackMap.get(packageName));
			// }
			// callBackMap.put(packageName, callback);
		}

		@Override
		public void sendMessage(IBLECentralCallback callback, String node, String path, byte[] data)
				throws RemoteException {
			LogTool.d(TAG, String.format("[sendMessage] node = %s  path = %s and data = %d.", node, path, data.length));
			boolean toggle = Utils.getShardBondToggle(BLECentralService.this);
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
		public void putDataItem(IBLECentralCallback callback, PutDataRequest putDataRequest) throws RemoteException {
			LogTool.d(TAG, "[putDataItem] putDataRequest = " + putDataRequest.toString());
			boolean toggle = Utils.getShardBondToggle(BLECentralService.this);
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
					// DataInfoParser.dataUnpack(randomAccessFile);
					callback.setAssetRsp();
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
		public void addListener(int type, IBLECentralCallback callback, IWearableListener listener)
				throws RemoteException {
			addCommonListener(type, callback, listener);
		}

		@Override
		public void removeListener(int type, IBLECentralCallback callback, IWearableListener listener)
				throws RemoteException {
			removeCommonListener(type, callback, listener);
		}

		@SuppressLint("SimpleDateFormat")
		@Override
		/**
		 * 4 reasons
		 * 0x1 - Manual time update
		 * 0x2 - external reference time update
		 * 0x4 - Change of time zone
		 * 0x8 - Change of DST (daylight savings time)
		 */
		public boolean syncTime(long time, int reason) throws RemoteException {
			boolean toggle = Utils.getShardBondToggle(BLECentralService.this);
			if (!toggle) {
				LogTool.i(TAG, "toggle false can't send data.");
				return false;
			}
			if (!isBLEServiceOK()) {
				LogTool.e(TAG, "BLE服务未连接,同步时间失败.");
				connectBLE();
				return false;
			}
			return syncLocalTime(time, reason);
		}

		@Override
		public void getFdForAsset(IBLECentralCallback callback, Asset asset) throws RemoteException {
			LogTool.d(TAG, "invoke getFdForAsset....." + asset.toString());
			if (callback == null) {
				LogTool.e(TAG, "添加监听接头失败");
				return;
			}
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
		public void getDataItems(IBLECentralCallback callback, Uri uri) throws RemoteException {

		}

		@Override
		public void getConnectedNodes(IBLECentralCallback callback) throws RemoteException {
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
		public void getLocalNode(IBLECentralCallback callback) throws RemoteException {
			GetLocalNodeResponse response = new GetLocalNodeResponse();
			NodeHolder node = new NodeHolder(mBluetoothAdapter.getAddress(),
					mBluetoothAdapter.getName() == null ? mBluetoothAdapter.getAddress() : mBluetoothAdapter.getName());
			response.setNode(node);
			callback.setGetLocalNodeRsp(response);
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
				// LogTool.d(TAG, "HANDLER_CACHE_CHECK = " + sdf.format(new
				// Date()));
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
		if (!isServiceAvailable()) {
			// 1.判定BLE连接正常
			LogTool.i(TAG, "BLE未连接,进行BLE连接");
			connectBLE();
		} else if (!isRfcommServiceAvailable() || mRfcommService == null) {
			LogTool.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
			bindRfcommService();
		} else if (!mRfcommService.isConnected() && !mRfcommService.isConnecting()) {
			LogTool.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
			mRfcommService.start();
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
	 * sync local time
	 * 
	 * @param time
	 * @param reason
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private boolean syncLocalTime(long time, int reason) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
		LogTool.d(TAG, "手机端：同步时间 time = " + sdf.format(time));
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

		boolean isSuccess = writeCharacteristic(BleUuid.SERVICE_CURRENT_TIME, BleUuid.CHAR_CURRENT_TIME, timeBytes);
		LogTool.d(TAG, "同步时间 isSuccess = " + isSuccess);
		return isSuccess;
	}

	/**
	 * handle total package succeed
	 * 
	 * @param uuid
	 * @throws RemoteException
	 */
	private void handleTotalDataSentSuccess(String uuid) throws RemoteException {
		Iterator<String> keys = mCacheInfoMap.keySet().iterator();
		while (keys.hasNext()) {
			LogTool.e(TAG, "[handleTotalDataSentSuccess] key = " + keys.next());
		}
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

	/**
	 * RFCOMM Service connection
	 */
	private ServiceConnection rfcommConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogTool.d(TAG, "[onServiceDisconnected] name:" + name);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogTool.d(TAG, "[onServiceConnected] name:" + name);
			mRfcommService = IRfcommServerService.Stub.asInterface(service);
			try {
				mRfcommService.registerCallback(rfcommCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private IRfcommServerCallback.Stub rfcommCallback = new IRfcommServerCallback.Stub() {

		@Override
		public boolean onRFCOMMSocketReady(String address) throws RemoteException {
			// handler.sendEmptyMessage(TIMER_START);
			// 告诉另一端BLE BluetoothServerSocket准备就绪
			// writeCharacteristic(BleUuid.SERVICE_CLOUDER_WATCH,
			// BleUuid.CHAR_CLOUDER_WATCH_RFCOMM, START_RFCOMM_COMMAND);
			boolean isSuccess = writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION,
					BleUuid.CHAR_MANUFACTURER_NAME_STRING, RFCOMMServerService.RFCOMM_CONNECT_READY + "&" + address);
			LogTool.i(TAG, "手机端告知手表端RFCOMM服务端已准备就绪 isSuccess = " + isSuccess + " address = " + address);
			return isSuccess;
		}

		@Override
		public void onRFCOMMSocketConnected() throws RemoteException {
			LogTool.d(TAG, "BluetoothServerSocket connected and tell ble central service can send messages.");
			mInQueueThread = new InQueueThread();
			mInQueueThread.start();
		}

		@Override
		public void onRFCOMMSocketDisconnected(int cause, String address) throws RemoteException {
			LogTool.d(TAG, String.format("手机端：RFCOMM Server Socket已经断开连接 cause %d,停止InQueue Thread线程.", cause));
			if (mInQueueThread != null) {
				LogTool.d(TAG, "InQueueThread 已经被销毁");
				drainQueue();
				mInQueueThread.cancel();
				mInQueueThread = null;
			}
			switch (cause) {
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_EXPIRED:
				// 通知另一端断开连接
				LogTool.d(TAG, "手机端过期关闭RFCOMM服务,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_STOP:
				// 通知另一端关闭连接
				LogTool.d(TAG, "手机端关闭RFCOMM服务,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_EXCEPTION:
				// 通知另一端异常发生
				LogTool.d(TAG, "手机端RFCOMM发生异常,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_RESTART:
				LogTool.d(TAG, "手机端RFCOMM重新启动");
				// writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION,
				// BleUuid.CHAR_MANUFACTURER_NAME_STRING,
				// "restart");
				break;

			default:
				break;
			}

		}

		@Override
		public void onDataReceived(byte[] bytes) throws RemoteException {
			TransportData transportData = TransportParser.dataUnpack(bytes);
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
						CRCUtil.makeCrcToBytes(filepath);
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
				LogTool.i(TAG, String.format("传输总包(UUID = %s)发送成功.", uuid));
				handleTotalDataSentSuccess(uuid);
				break;
			default:
				LogTool.i(TAG, "UUID = " + uuid + " onDataSent unknown status = " + response.getStatus());
				break;
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		LogTool.d(TAG, "onCreate...");
		bindRfcommService();
		/**
		 * wake up timeout scan
		 */
		LogTool.e(TAG, "start HANDLER_CACHE_CHECK");
		mCacheHandler.sendEmptyMessageDelayed(HANDLER_CACHE_CHECK, 1000);

		registerDateTimeReceiver();

		registerBluetoothReceiver();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterDateTimeReceiver();

		unregisterBluetoothReceiver();

		isCloudWatchServiceAvailabled = false;
		// 解绑RFCOMM Service
		unBindRfcommService();
		// 断开BLE连接
		disconnectBLE();
		closeBLE();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogTool.d(TAG, "onStartCommand...");
		if (!BleUtil.isBLESupported(this)) {
			LogTool.e(TAG, "BLE is not supported");
			return START_REDELIVER_INTENT;
		}

		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBluetoothAdapter = manager.getAdapter();
		}
		if ((mBluetoothAdapter == null)) {
			LogTool.e(TAG, "Bluetooth unavailable");
			return START_REDELIVER_INTENT;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			LogTool.e(TAG, "Bluetooth disenable,enabling.");
			mBluetoothAdapter.enable();
			mLastIntent = intent;
			return START_REDELIVER_INTENT;
		}
		connectBLE(intent);
		return START_REDELIVER_INTENT;
	}

	/**
	 * connect to BLE
	 * 
	 * @param intent
	 */
	private void connectBLE(Intent intent) {
		LogTool.d(TAG, "verify bond name and start to connect to BLE");
		if (intent != null) {
			String bondBluetoothAddress = intent.getStringExtra(Utils.BLUETOOTH_BOND_ADDRESS);
			boolean toggle = intent.getBooleanExtra(Utils.BLUETOOTH_BOND_TOGGLE, true);
			String sharedBondAddress = Utils.getShardBondAddress(BLECentralService.this);
			LogTool.d(TAG, "save toggle = " + toggle);
			Utils.saveSharedBondToggle(BLECentralService.this, toggle);
			if (toggle) {
				if (!TextUtils.isEmpty(bondBluetoothAddress)) {
					if (!bondBluetoothAddress.equals(sharedBondAddress)) {
						LogTool.d(TAG, "save bondBluetoothAddress = " + bondBluetoothAddress);
						Utils.saveSharedBondAddress(BLECentralService.this, bondBluetoothAddress);
						clearRfcommService();
						clearBLEService();
						if (isBLEServiceOK()) {
							LogTool.d(TAG, "disconnect last BLE connection");
							disconnectBLE();
							closeBLE();
						}
						connectBLE(bondBluetoothAddress);
					} else {
						if (isBLEServiceOK()) {
							LogTool.d(TAG,
									"bondBluetoothAddress == sharedBondAddress and BLE Service connected,do nothing.");
						} else {
							connectBLE(bondBluetoothAddress);
						}
					}
				} else {
					LogTool.e(TAG, "Bluetooth bondAddress null error");
				}
			} else {
				LogTool.d(TAG, "toggle is false ,disconnect BLE connection and clear cache.");
				clearRfcommService();
				clearBLEService();
				if (isBLEServiceOK()) {
					LogTool.d(TAG, "disconnect last BLE connection");
					disconnectBLE();
				}
			}
		}
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

	/**
	 * connect BLE
	 * 
	 * @param address
	 * @return
	 */
	private boolean connectBLE(String address) {
		LogTool.d(TAG, "connect to BLE with address = " + address);
		if (mBluetoothAdapter == null || TextUtils.isEmpty(address)) {
			LogTool.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		// 重连有问题，1.没有回调 2.返回成功但是实际未连接成功
		// Previously connected device. Try to reconnect.
		// if (!TextUtils.isEmpty(mBLEAddress) && address.equals(mBLEAddress) &&
		// mBluetoothGatt != null) {
		// Log.d(TAG,
		// "Trying to use an existing mBluetoothGatt for connection.");
		// if (mBluetoothGatt.connect()) {
		// Log.d(TAG, "Re-connect succeed.");
		// mBLEState = BluetoothProfile.STATE_CONNECTED;
		// return true;
		// } else {
		// mBLEState = BluetoothProfile.STATE_DISCONNECTED;
		// Log.d(TAG, "Re-connect failed.");
		// return false;
		// }
		// }

		// Handler handler = new Handler(getMainLooper());
		// // Connect to BLE device from mHandler
		// handler.post(new Runnable() {
		// @Override
		// public void run() {
		// mBTGatt = mBTDevice.connectGatt(mContext, false, mGattCallback);
		// }
		// });

		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			LogTool.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// BluetoothDevice macDevice =
		// mBluetoothAdapter.getRemoteDevice("3C:CB:7C:8B:2F:69");
		// LogTool.e(
		// TAG,
		// "ble and mac Name:" + device.getName() + " " + macDevice.getName() +
		// " BondState:"
		// + device.getBondState() + " " + macDevice.getBondState() + " Type:" +
		// device.getType() + " "
		// + macDevice.getType() + " " + device.equals(macDevice));
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		LogTool.d(TAG, " current thread id  = " + Thread.currentThread().getId() + " name = "
				+ Thread.currentThread().getName());
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		LogTool.d(TAG, "Trying to create a new connection, BLE address = " + address);
		mBLEAddress = address;
		mBLEState = BluetoothProfile.STATE_CONNECTING;
		return true;
	}

	/**
	 * connect BLE based saved address
	 * 
	 * @param address
	 * @return
	 */
	private void connectBLE() {
		LogTool.d(TAG, "connect to BLE based saved address.");
		connectBLE(Utils.getShardBondAddress(BLECentralService.this));
	}

	/**
	 * handle disconnect
	 */
	private void handleDisconnect() {
		bleReconnectCount++;
		String sharedBondAddress = Utils.getShardBondAddress(BLECentralService.this);
		if (!TextUtils.isEmpty(sharedBondAddress)) {
			LogTool.i(TAG, "BLE断开,尝试建立BLE连接");
			connectBLE();
		}
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnectBLE() {
		LogTool.i(TAG, "disconnect BLE");
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			LogTool.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_SERIAL_NUMBER_STRING, "disconnect");
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void closeBLE() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * start or stop BLE scan
	 * 
	 * @param enable
	 */
	// private void scanLeDevice(boolean enable) {
	// LogTool.d(TAG, "scan BLE device enable = " + enable);
	// if (enable) {
	// // Stops scanning after a pre-defined scan period.
	// Looper myLooper = Looper.myLooper();
	// LogTool.d(TAG, myLooper == null ? "当前线程Looper为空" :
	// (myLooper.getThread().getId() + " " + myLooper
	// .getThread().getName()));
	// if (myLooper == null) {
	// Looper.prepare();
	// }
	// new Handler().postDelayed(new Runnable() {
	// @Override
	// public void run() {
	// mScanning = false;
	// if (mBluetoothAdapter != null) {
	// mBluetoothAdapter.stopLeScan(mLeScanCallback);
	// }
	// }
	// }, SCAN_PERIOD);
	//
	// mScanning = true;
	// mBluetoothAdapter.startLeScan(mLeScanCallback);
	// } else {
	// mScanning = false;
	// mBluetoothAdapter.stopLeScan(mLeScanCallback);
	// }
	// }

	/**
	 * check BLE Service：connect succeed+service available
	 * 
	 * @return
	 */
	private boolean isServiceAvailable() {
		LogTool.d(TAG, ("判定BLE服务是否可用isConnected = " + (mBLEState == BluetoothProfile.STATE_CONNECTED)
				+ " isAvailabled = " + isCloudWatchServiceAvailabled));
		// return isBLEServiceOK() && isCloudWatchServiceOk();
		return isBLEServiceOK();
	}

	private boolean isBLEServiceOK() {
		return mBLEState == BluetoothProfile.STATE_CONNECTED;
	}

	private boolean isCloudWatchServiceOk() {
		return isCloudWatchServiceAvailabled;
	}

	/**
	 * common add listener operation
	 * 
	 * @param type
	 * @param callback
	 * @param listener
	 * @throws RemoteException
	 */
	private void addCommonListener(int type, IBLECentralCallback callback, IWearableListener listener)
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
				LogTool.e(TAG, "手表端：wearableListenerList NullPointerException");
				return;
			}
			clearUnusedListener(wearableListenerList);
			LogTool.d(TAG, String.format("手机端：准备添加监听器(type = %d),当前监听器count = %d.", type, wearableListenerList.size()));
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
			LogTool.d(TAG, "手机端：添加监听器成功,当前监听器 count =" + wearableListenerList.size());
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
	private void removeCommonListener(int type, IBLECentralCallback callback, IWearableListener listener)
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
			throw new NullPointerException("手机端：wearableListenerList NullPointerException");
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
		LogTool.d(TAG, String.format("手机端：准备移除监听器(type = %d),当前监听器count = %d.", type, wearableListenerList.size()));
		int index = -1;
		int size = wearableListenerList.size();
		if (size > 0) {
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
	 * register datetime receiver
	 */
	private void registerDateTimeReceiver() {
		LogTool.d(TAG, "register datetime receiver");
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_DATE_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		mDateTimeReceiver = new DateTimeReceiver();
		registerReceiver(mDateTimeReceiver, filter);
	}

	/**
	 * unregister datetime receiver
	 */
	private void unregisterDateTimeReceiver() {
		LogTool.d(TAG, "unregister datetime receiver");
		if (mDateTimeReceiver != null) {
			unregisterReceiver(mDateTimeReceiver);
			mDateTimeReceiver = null;
		}
	}

	/**
	 * register bluetooth receiver
	 */
	private void registerBluetoothReceiver() {
		LogTool.d(TAG, "register bluetooth receiver");
		mBluetoothReceiver = new BluetoothReceiver();
		IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBluetoothReceiver, filter);
	}

	/**
	 * unregister bluetooth receiver
	 */
	private void unregisterBluetoothReceiver() {
		LogTool.d(TAG, "unregister bluetooth receiver");
		if (mBluetoothReceiver != null) {
			unregisterReceiver(mBluetoothReceiver);
			mBluetoothReceiver = null;
		}
	}

	/**
	 * check if RFCOMM Service exists
	 * 
	 * @return
	 */
	private boolean isRfcommServiceAvailable() {
		return BleUtil.isServiceRunning(this, RFCOMM_SERVICE_PACKAGE_PATH);
	}

	/**
	 * bind Rfcomm service
	 */
	private void bindRfcommService() {
		if (!isRfcommServiceAvailable()) {
			Intent intent = new Intent(this, RFCOMMServerService.class);
			bindService(intent, rfcommConnection, Context.BIND_AUTO_CREATE);
		} else {
			LogTool.d(TAG, "RFCOMM Service is alive.");
		}
	}

	/**
	 * 断开Rfcomm服务连接
	 */
	private void unBindRfcommService() {
		if (rfcommConnection != null) {
			unbindService(rfcommConnection);
		}
	}

	/**
	 * Retrieves a list of CloudWatch GATT services on the connected device.
	 * This should be invoked only after
	 * {@code BluetoothGatt#discoverServices()} completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;
		LogTool.d(TAG, "service char size: "
				+ mBluetoothGatt.getService(UUID.fromString(BleUuid.SERVICE_CLOUDER_WATCH)).getCharacteristics().size());
		return mBluetoothGatt.getServices();
	}

	private boolean writeCharacteristic(String serviceUuid, String characteristicUuid, String value) {
		if (mBluetoothGatt == null) {
			return false;
		}
		BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
		if (service == null) {
			LogTool.w(TAG, "Service NOT found :" + serviceUuid);
			return false;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
		if (characteristic == null) {
			LogTool.w(TAG, "Characteristic NOT found :" + characteristicUuid);
			return false;
		}
		characteristic.setValue(value);
		LogTool.d(TAG, "writeCharacteristic serviceUuid  = " + serviceUuid + " characteristicUuid = "
				+ characteristicUuid + "写入值为" + new String(characteristic.getValue()));
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	private boolean writeCharacteristic(String serviceUuid, String characteristicUuid, byte[] bytes) {
		if (mBluetoothGatt == null) {
			return false;
		}
		BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
		if (service == null) {
			LogTool.w(TAG, "Service NOT found :" + serviceUuid);
			return false;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
		if (characteristic == null) {
			LogTool.w(TAG, "Characteristic NOT found :" + characteristicUuid);
			return false;
		}
		characteristic.setValue(bytes);
		LogTool.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * 判定BLE服务是否支持RFCOMM和Notification
	 * 
	 * @throws Exception
	 */
	private void checkCloudWatchServiceSupported() throws Exception {
		BluetoothGattService cloudWatchGattService = mBluetoothGatt.getService(UUID
				.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
		if (cloudWatchGattService == null) {
			LogTool.e(TAG, "Cloud Watch Service不支持");
			return;
		}

		BluetoothGattCharacteristic rfcommCharacteristic = cloudWatchGattService.getCharacteristic(UUID
				.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
		if (rfcommCharacteristic == null) {
			LogTool.e(TAG, "RFCOMM BLUETOOTH COMMAND不支持");
		} else {
			mBluetoothGatt.setCharacteristicNotification(rfcommCharacteristic, true);
		}

		BluetoothGattCharacteristic heartbeatCharacteristic = cloudWatchGattService.getCharacteristic(UUID
				.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING));
		if (heartbeatCharacteristic == null) {
			LogTool.e(TAG, "HEARTBEAT OR DISCONNECT COMMAND不支持");
		} else {
			mBluetoothGatt.setCharacteristicNotification(heartbeatCharacteristic, true);
		}

		BluetoothGattService currentTimeGattService = mBluetoothGatt.getService(UUID
				.fromString(BleUuid.SERVICE_CURRENT_TIME));
		if (currentTimeGattService == null) {
			LogTool.e(TAG, "Current Time Service不支持");
			return;
		}
		BluetoothGattCharacteristic syncTimeCharacteristic = currentTimeGattService.getCharacteristic(UUID
				.fromString(BleUuid.CHAR_CURRENT_TIME));
		if (syncTimeCharacteristic == null) {
			LogTool.e(TAG, "SYNCTIME COMMAND不支持");
		}
	}

	/**
	 * clear cache
	 */
	private void clearBLEService() {

		LogTool.i(TAG, "clear ble service");

		mInQueue.clear();

		mCacheInfoMap.clear();
		mCacheCallbackMap.clear();

		// task cache
		mCachePriorityTaskMap.clear();
		mCacheTransportDataMap.clear();
		mCacheAssetDataMap.clear();

		nodeMap.clear();
	}

	/**
	 * clear RFCOMM service
	 */
	private void clearRfcommService() {
		LogTool.i(TAG, "clear rfcomm service");
		if (isRfcommServiceAvailable() && mRfcommService != null) {
			try {
				LogTool.i(TAG, "rfcomm service stop");
				mRfcommService.stop();
			} catch (RemoteException e) {
				LogTool.e(TAG, "RemoteException", e);
			}
		}
	}

	public class DateTimeReceiver extends BroadcastReceiver {

		private static final String TAG = "DateTimeReceiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_TIMEZONE_CHANGED.equals(action) || Intent.ACTION_DATE_CHANGED.equals(action)
					|| Intent.ACTION_TIME_CHANGED.equals(action)) {
				LogTool.d(TAG, "current action = " + action);
				if (isBLEServiceOK()) {
					syncLocalTime(new Date().getTime(), 0x01);
				} else {
					LogTool.e(TAG, "BLE Service not OK,can't sync time.");
				}
			}
		}

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
					connectBLE(mLastIntent);
				} else if (BluetoothAdapter.STATE_OFF == state) {
					LogTool.e(TAG, "Bluetooth close");
				}
			}
		}
	}

	public static class Callback {

		public static final int TYPE_MESSAGE = 1;

		public static final int TYPE_DATA = 2;

		private int type;

		private IBLECentralCallback callback;

		public Callback(int type, IBLECentralCallback callback) {
			super();
			this.type = type;
			this.callback = callback;
		}

		public int getType() {
			return type;
		}

		public IBLECentralCallback getCallback() {
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
			LogTool.i(TAG,
					"手机端：启动InQueueThread,接收队列中的消息 isCancel = " + isCancel + " mInQueue size : " + mInQueue.size());
			while (!isCancel) {
				try {
					LogTool.d(TAG, "手机端：InQueueThreadd等待接收队列中的消息...");
					QueuePriorityTask task = mInQueue.take();
					LogTool.d(TAG, "手机端： 剩余处理的Task size = " + mInQueue.size());
					String id = task.getData().getId();
					task.setTime(new Date().getTime());
					mCachePriorityTaskMap.put(id, task);
					TransportData data = task.getData();
					byte[] bytes = TransportParser.dataPack(data);
					LogTool.e(TAG, "发送传输包 size = " + bytes.length + " TransportData = " + data.toString());
					if (!mRfcommService.write(bytes)) {
						LogTool.e(TAG, String.format("写入子包(UUID = %s)发送失败,重新发送", id));
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
					e.printStackTrace();
					LogTool.d(TAG, "Exception Queue remained size " + mInQueue.size(), e);
					break;
				}
			}
		}

		public void cancel() {
			isCancel = true;
		}
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

	/**
	 * handle message
	 * 
	 * @param filePath
	 *            the cache file's path
	 * @throws RemoteException
	 * @throws FileNotFoundException
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
	 * handle data
	 * 
	 * @param filePath
	 *            the cache file's path
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
		if (mRfcommService != null) {
			mRfcommService.setTotalTransportSuccess(id);
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

	private void onPostMessageReceived(String uuid, MessageEventHolder messageEventHolder) {
		LogTool.i(TAG, String.format(
				"手机端：遍历所有WearableListener接口(Message),发送消息到上层 -> node = %s  path = %s and data size = %d.",
				messageEventHolder.getSourceNodeId(), messageEventHolder.getPath(),
				(messageEventHolder.getData() == null ? 0 : messageEventHolder.getData().length)));
		LogTool.d(TAG, "当前缓存中接口个数为" + mMessageWearableListenerList.size());
		for (WearableListenerItem listener : mMessageWearableListenerList) {
			LogTool.i(
					TAG,
					"手机端：当前接收消息Node为 " + messageEventHolder.getSourceNodeId() + " 当前接收消息包名为 "
							+ listener.getPackageName());
			if (listener.getPackageName().equals(messageEventHolder.getPackageName())
					&& mBluetoothAdapter.getAddress().equals(messageEventHolder.getSourceNodeId())) {
				try {
					LogTool.i(TAG, "手机端：Node节点匹配,通知上层接收消息");
					listener.getListener().onMessageReceived(messageEventHolder);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
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
		LogTool.i(TAG, "手机端：遍历所有WearableListener接口(Data),发送数据到上层." + "当前缓存中接口个数为" + mDataWearableListenerList.size());
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

	private void onPeerConnected(NodeHolder nodeHolder) {
		LogTool.i(
				TAG,
				String.format("手机端：遍历所有NodeListener接口,发送到上层,建立连接 -> nodeId = %s and displayName = %s.",
						nodeHolder.getId(), nodeHolder.getDisplayName()));
		nodeMap.put(nodeHolder.getId(), nodeHolder);
		for (WearableListenerItem listener : mNodeWearableListenerList) {
			LogTool.i(
					TAG,
					"手机端：通知上层onPeerConnected, node =  " + nodeHolder.getId() + ", displayName = "
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
				String.format("手机端：遍历所有NodeListener接口,发送到上层 ,连接失败-> nodeId = %s and displayName = %s.",
						nodeHolder.getId(), nodeHolder.getDisplayName()));
		nodeMap.remove(nodeHolder.getId());
		for (WearableListenerItem listener : mNodeWearableListenerList) {
			LogTool.i(TAG, "手机端：通知上层onPeerDisconnected, node =  " + nodeHolder.getId() + ", displayName = "
					+ nodeHolder.getDisplayName());
			try {
				listener.getListener().onPeerDisconnected(nodeHolder);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
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
	 * 获取Device ID
	 * 
	 * @return
	 */
	private String getDeviceId() {
		TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String deviceId = mTm.getDeviceId();

		LogTool.d(TAG, "deviceId = " + deviceId);
		return deviceId;
	}

}
