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

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;
import com.cms.android.wearable.internal.MessageEventHolder;
import com.cms.android.wearable.internal.NodeHolder;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.service.codec.ByteUtil;
import com.cms.android.wearable.service.codec.CloudWatchParser;
import com.cms.android.wearable.service.codec.CloudWatchRequestData;
import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.BleUuid;
import com.cms.android.wearable.service.vo.MessageInfo;

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

	private LinkedBlockingQueue<QueueItem> mInQueue = new LinkedBlockingQueue<QueueItem>();

	private List<WearableListenerItem> mWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<NodeListenerItem> mNodeListenerList = new ArrayList<NodeListenerItem>();

	private BluetoothAdapter mBTAdapter;

	private BluetoothLeAdvertiser mBTAdvertiser;

	private BluetoothGattServer mBTGattServer;

	private AdvertiseCallback mAdvertiseCallback;

	private BluetoothDevice mBLEDevice;

	private InQueueThread mInQueueThread;

	private Map<String, Node> nodeMap = new HashMap<String, Node>();

	private Map<String, String> nodeIdMap = new HashMap<String, String>();

	/**
	 * BLE当前连接状态
	 */
	private int mBLEState = BluetoothGattServer.STATE_DISCONNECTED;

	private IRfcommClientService mRfcommClientService;

	private String localNodeId;

	/**
	 * 与RFCOMM Service的连接
	 */
	private ServiceConnection rfcommConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "[onServiceDisconnected] name:" + name);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "[onServiceConnected] name:" + name);
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
		public void onRFCOMMSocketConnected() throws RemoteException {
			Log.d(TAG, "BluetoothServerSocket connected and tell ble central service can send messages.");
			mInQueueThread = new InQueueThread();
			mInQueueThread.start();
		}

		@Override
		public void onRFCOMMSocketDisconnected(int cause) throws RemoteException {
			Log.d(TAG, "RFCOMM 连接断开,cause = " + cause + " isEmpty = " + mInQueue.isEmpty());
			if (mInQueueThread != null) {
				// RFCOMM断开后,再次发送消息建立发送时,InQueueThread线程还没有销毁,此时InQueue会将该消息消费掉
				drainQueue();
				mInQueueThread.cancel();
				mInQueueThread = null;
				Log.d(TAG, "RFCOMM断开后,取消InQueueThread.");
			}
		}

		@Override
		public void onMessageReceived(byte[] bytes) throws RemoteException {
			Log.d(TAG, "Received message,prepare to unpack message.");
			CloudWatchRequestData cloudWatchResponseData = CloudWatchParser.dataUnpack(bytes);
			MessageEventHolder holder = new MessageEventHolder();
			holder.setNodeId(new String(cloudWatchResponseData.getNodeId()));
			holder.setPath(new String(cloudWatchResponseData.getPath()));
			holder.setData(cloudWatchResponseData.getData());
			holder.setPackageName(new String(cloudWatchResponseData.getPackageName()));
			Log.d(TAG, "Received message is " + holder.toString());
			onPostMessageReceived(holder);
		}

		@Override
		public void onMessageSend(String requestId, String statusCode, String versionCode) throws RemoteException {

		}
	};

	private void onPostMessageReceived(MessageEventHolder messageEventHolder) {
		Log.i(TAG, String.format("手表端：遍历所有WearableListener接口,发送消息到上层 -> node = %s  path = %s and data = %s.",
				messageEventHolder.getSourceNodeId(), messageEventHolder.getPath(),
				new String(messageEventHolder.getData())));
		Log.d(TAG, "当前缓存中接口个数为" + mWearableListenerList.size());
		for (WearableListenerItem listener : mWearableListenerList) {
			Log.i(TAG, "手表端：当前接收消息Node为 " + messageEventHolder.getSourceNodeId() + " 遍历接口Node为 " + listener.getNode());
			Log.i(TAG, "手表端：listener packageName : " + listener.getPackageName() + " messagePackageName : "
					+ messageEventHolder.getPackageName() + " listener nodeId : " + listener.getNode()
					+ " message nodeId : " + messageEventHolder.getSourceNodeId());
			if (listener.getNode().equals(messageEventHolder.getSourceNodeId())
					&& listener.getPackageName().equals(messageEventHolder.getPackageName())) {
				try {
					Log.i(TAG, "手表端：Node节点匹配,通知上层接收消息");
					listener.getListener().onMessageReceived(messageEventHolder);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class WearableListenerItem {

		private String node;

		private IWearableListener listener;

		private String packageName;

		public WearableListenerItem() {
			super();
		}

		public WearableListenerItem(String node, String packageName, IWearableListener listener) {
			super();
			this.node = node;
			this.listener = listener;
			this.packageName = packageName;
		}

		public String getNode() {
			return node;
		}

		public void setNode(String node) {
			this.node = node;
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

	public static class NodeListenerItem {

		private IWearableListener listener;

		public NodeListenerItem() {
			super();
		}

		public NodeListenerItem(IWearableListener listener) {
			super();
			this.listener = listener;
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
			Log.d(TAG, String.format("[sendMessage] node = %s  path = %s and data = %s.", node, path, new String(data)));

			if (!isBLEServiceAvailable()) {
				// 1.判定BLE连接正常
				Log.i(TAG, "当前BLE连接断开,等待主设备连接");
				// 若BLE广播断开的话，则应该重启广播，从设备不应该负责重新连接
				// TODO
				// connect(mBLEAddress);
			} else if (!isRfcommServiceAvailable() || mRfcommClientService == null) {
				Log.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
				bindRfcommService();
			} else if (!mRfcommClientService.isConnected()) {
				Log.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
				// RFCOMM服务未启动,则发送通知告知手机端启动
				BluetoothGattService service = mBTGattServer.getService(UUID
						.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID
						.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
				characteristic.setValue("start");
				Log.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
				mBTGattServer.notifyCharacteristicChanged(mBLEDevice, characteristic, false);
			} else if (mInQueueThread == null || mInQueueThread.isCancel) {
				Log.i(TAG, "InQueue服务未启动,启动该服务");
				mInQueueThread = new InQueueThread();
				mInQueueThread.start();
			} else {
				Log.i(TAG, "服务都已经准备就绪");
			}
			MessageInfo info = new MessageInfo();
			info.setNodeId(node);
			info.setTimeStamp(new Date().getTime());
			info.setContent(data);
			info.setPath(path);
			info.setPackageName(callback.getPackageName());
			Log.e(TAG, "packageName : " + callback.getPackageName());
			Log.e(TAG, "发送消息已进入队列,待处理");
			putQueue(info, callback);
		}

		@Override
		public void registerCallback(String packageName, IBLEPeripheralCallback callback) throws RemoteException {

		}

		@Override
		public void disconnect() throws RemoteException {

		}

		@Override
		public void addListener(IBLEPeripheralCallback callback, IWearableListener listener) throws RemoteException {
			String node = localNodeId;
			Log.d(TAG, "手表端：准备添加监听器,当前监听器count = " + mWearableListenerList.size());
			if (callback == null) {
				Log.e(TAG, "添加监听接头失败");
				return;
			}
			if (listener == null || node == null) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			String packageName = callback.getPackageName();
			mWearableListenerList.add(new WearableListenerItem(node, packageName, listener));
			callback.setStatusRsp(new Status(0));
			Log.d(TAG, "手表端：添加监听器成功,当前监听器count =" + mWearableListenerList.size());
		}

		@Override
		public void removeListener(IBLEPeripheralCallback callback, IWearableListener listener) throws RemoteException {
			String node = localNodeId;
			Log.d(TAG, "手表端：准备移除监听器,Node = " + node + " 当前缓存中接口个数 = " + mWearableListenerList.size());
			if (callback == null) {
				Log.e(TAG, "添加监听接口失败");
				return;
			}
			if (listener == null || TextUtils.isEmpty(node)) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			String packageName = callback.getPackageName();
			int index = -1;
			int size = mWearableListenerList.size();
			if (size > 0) {
				for (int i = 0; i < mWearableListenerList.size(); i++) {
					WearableListenerItem item = mWearableListenerList.get(i);
					if (item.getListener().equals(listener) && item.getNode().equals(node)
							&& item.getPackageName().equals(packageName)) {
						index = i;
						break;
					}
				}
				if (index >= 0 && index < mWearableListenerList.size()) {
					mWearableListenerList.remove(index);
				}
			}
			Log.d(TAG, "移除监听器成功,当前监听器count = " + mWearableListenerList.size());
			callback.setStatusRsp(new Status(0));
		}

		@Override
		public void addNodeListener(IBLEPeripheralCallback callback, IWearableListener listener) throws RemoteException {
			Log.d(TAG, "手表端：准备添加node监听器,当前监听器count = " + mNodeListenerList.size());
			if (callback == null) {
				Log.e(TAG, "添加node监听接口失败");
				return;
			}
			if (listener == null) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			mNodeListenerList.add(new NodeListenerItem(listener));
			callback.setStatusRsp(new Status(0));
			Log.d(TAG, "手表端：添加node监听器成功,当前监听器count = " + mNodeListenerList.size());
		}

		@Override
		public void removeNodeListener(IBLEPeripheralCallback callback, IWearableListener listener)
				throws RemoteException {
			Log.d(TAG, "手表端：准备移除Node监听器,  当前缓存中接口个数 = " + mNodeListenerList.size());
			if (callback == null) {
				Log.e(TAG, "移除Node监听接口失败");
				return;
			}
			if (listener == null) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			int index = -1;
			int size = mNodeListenerList.size();
			if (size > 0) {
				for (int i = 0; i < mNodeListenerList.size(); i++) {
					NodeListenerItem item = mNodeListenerList.get(i);
					if (item.getListener().equals(listener)) {
						index = i;
						break;
					}
				}
				if (index >= 0 && index < mNodeListenerList.size()) {
					mNodeListenerList.remove(index);
				}
			}
			Log.d(TAG, "移除Node监听器成功,当前监听器count = " + mNodeListenerList.size());
			callback.setStatusRsp(new Status(0));
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
			Log.d(TAG, "setGetConnectedNodesRsp " + nodeList.size());
		}

		@Override
		public void getLocalNode(IBLEPeripheralCallback callback) throws RemoteException {
			GetLocalNodeResponse response = new GetLocalNodeResponse();
			String nodeId = localNodeId != null ? localNodeId : mBTAdapter.getAddress();
			String displayName = mBTAdapter.getName() != null ? mBTAdapter.getName() : nodeId;
			NodeHolder node = new NodeHolder(localNodeId != null ? localNodeId : mBTAdapter.getAddress(), displayName);
			response.setNode(node);
			callback.setLocalNodeRsp(response);
			Log.d(TAG, "set local node : " + node.getId());
		}

	};

	/**
	 * 将消息放入队列中
	 * 
	 * @param packageName
	 * @param message
	 */
	private void putQueue(MessageInfo msg, IBLEPeripheralCallback callback) {
		// 1.CMS已经准备就绪，将消息放入Queue中等待处理
		// 2.xCMS仍然未准备就绪，将消息放入Queue,等待InQueue线程准备调用take处理
		QueueItem item = new QueueItem(msg, callback);
		mInQueue.offer(item);
		Log.d(TAG, "mInQueue size is " + mInQueue.size());
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate...");
		init();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindRfcommService();
		stopCloudWatchAdvertising();
	}

	private void init() {
		if (!BleUtil.isBLESupported(this)) {
			Log.e(TAG, "BLE is not supported");
			return;
		}

		// BT check
		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBTAdapter = manager.getAdapter();
		}
		if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
			Log.e(TAG, "Bluetooth unavailable");
			return;
		}

		startCloudWatchAdvertising();

		bindRfcommService();
	}

	private void startCloudWatchAdvertising() {
		Log.d(TAG, "startIASAdvertise...");
		if (mBTAdvertiser == null) {
			mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
		}
		if (mBTAdvertiser != null) {
			mBTGattServer = BleUtil.getManager(this).openGattServer(this, new BluetoothGattServerCallback() {

				@Override
				public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
						BluetoothGattCharacteristic characteristic) {
					super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
					Log.d(TAG,
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
					// TODO 手机端修改Character，提醒Watch端打开rfcomm连接等
					Log.d(TAG,
							String.format(
									"onCharacteristicWriteRequest device = %s, requestId = %d, characteristic = %s, value = %s.",
									device.toString(), requestId, characteristic, new String(value)));

					if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING))) {
						int status = -1;
						String mac = "";
						String content = new String(value);
						if (content.contains("&")) {
							String[] stringArray = content.split("&");
							status = Integer.parseInt(stringArray[0]);
							mac = stringArray[1];
						} else {
							Log.e(TAG, String.format("格式错误 %s,应该是type&Bluetooth MAC address", content));
							return;
						}
						Log.d(TAG, "Cloud watch rfcomm command is " + new String(value));
						// 将蓝牙地址作为启动RFCOMM标志位
						try {
							if (isRfcommServiceAvailable() && mRfcommClientService != null) {
								if (status == RFCOMMClientService.RFCOMM_CONNECT_READY
										&& BleUtil.isBluetoothAddress(mac)) {
									Log.d(TAG, "手表端启动Socket连接服务");
									mRfcommClientService.setStatus(RFCOMMClientService.RFCOMM_CONNECT_READY);
									mRfcommClientService.start(mac);
								} else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXPIRED
										|| status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_STOP
										|| status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_EXCEPTION) {
									mRfcommClientService.setStatus(status);
									Log.d(TAG, "手表端：收到指令为 " + status);
								} else if (status == RFCOMMClientService.RFCOMM_DISCONNECT_CAUSE_RESTART) {
									Log.d(TAG, "手表端：手机端服务启动");
								} else {
									Log.e(TAG, "Unknow status&MAC address");
								}
							} else {
								// 应该启动rfcomm服务并且根据所给地址建立rfcomm通道 TODO
								bindRfcommService();
							}
						} catch (RemoteException e) {
							e.printStackTrace();
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
							Log.d(TAG, "value[0]:" + value[0] + " value[1]" + value[1] + " value[2]:" + value[2]
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
							Log.i(TAG, "手表端：时间同步 调整时间为" + sdf.format(cal.getTime()));

							// 将调整的时间通过广播方式发送出去
							sendSyncTimeBroadcast(cal.getTime().getTime(), value[8]);
						} else {
							Log.e(TAG, "手表端：时间同步,格式错误");
						}
					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_NODE))) {
						if (value != null) {
							String nodeId = new String(value);
							NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId : device
									.getName());

							nodeIdMap.put(device.getAddress(), nodeId);
							nodeMap.put(nodeHolder.getId(), nodeHolder);
							onPeerConnected(nodeHolder);
						} else {
							Log.e(TAG, "无NodeId");
						}
					} else if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_NODE_LOCAL))) {
						if (value != null) {
							String nodeId = new String(value);
							localNodeId = nodeId;
						} else {
							Log.e(TAG, "无NodeId");
						}
					} else {
						Log.e(TAG, "无匹配Characteristic");
					}
					mBTGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[] {});
				}

				@Override
				public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
					super.onConnectionStateChange(device, status, newState);
					Log.d(TAG, String.format("onConnectionStateChange device = %s, status = %d, newState = %d.",
							device, status, newState));
					mBLEState = newState;
					if (BluetoothGattServer.STATE_CONNECTED == newState) {
						// 连接成功
						mBLEDevice = device;
						Log.e(TAG, "BLE服务成功连接");
					} else {
						// 连接失败
						mBLEDevice = null;
						Log.e(TAG, "BLE服务断开连接");

						String nodeId = nodeIdMap.get(device.getAddress());
						NodeHolder nodeHolder = new NodeHolder(nodeId, device.getName() == null ? nodeId : device
								.getName());
						nodeMap.remove(nodeHolder.getId());
						nodeIdMap.remove(nodeId);
						onPeerDisconnected(nodeHolder);
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
					Log.d(TAG, String.format("onExecuteWrite device = %s, requestId = %d, execute = %s.", device,
							requestId, execute + ""));
				}

				@Override
				public void onNotificationSent(BluetoothDevice device, int status) {
					super.onNotificationSent(device, status);
					Log.d(TAG, String.format("onNotificationSent device = %s, status = %d.", device, status));
				}

				@Override
				public void onServiceAdded(int status, BluetoothGattService service) {
					super.onServiceAdded(status, service);
					if (status == BluetoothGatt.GATT_SUCCESS) {
						Log.d(TAG, "onServiceAdded status=GATT_SUCCESS service=" + service.getUuid().toString()
								+ " size=" + service.getCharacteristics().size());
					} else {
						Log.d(TAG, "onServiceAdded status != GATT_SUCCESS");
					}
				}

			});

			setupCloudWatchService();

			mAdvertiseCallback = new AdvertiseCallback() {

				@Override
				public void onStartFailure(int errorCode) {
					super.onStartFailure(errorCode);
					Log.e(TAG, "广播服务启动失败");
				}

				@Override
				public void onStartSuccess(AdvertiseSettings settingsInEffect) {
					super.onStartSuccess(settingsInEffect);
					Log.e(TAG, "广播服务启动成功");
				}
			};

			mBTAdvertiser.startAdvertising(BleUtil.createAdvSettings(true, 0), BleUtil.createCloudWatchAdvertiseData(),
					mAdvertiseCallback);
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

			// serial number string char.
			BluetoothGattCharacteristic snsc = new BluetoothGattCharacteristic(
					UUID.fromString(BleUuid.CHAR_SERIAL_NUMBER_STRING), BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattCharacteristic sysIdsc = new BluetoothGattCharacteristic(
					UUID.fromString(BleUuid.CHAR_SYSTEM_ID_STRING), BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattCharacteristic nodebc = new BluetoothGattCharacteristic(UUID.fromString(BleUuid.CHAR_NODE),
					BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE
							| BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ
							| BluetoothGattCharacteristic.PERMISSION_WRITE);

			BluetoothGattCharacteristic localNodebc = new BluetoothGattCharacteristic(
					UUID.fromString(BleUuid.CHAR_NODE_LOCAL), BluetoothGattCharacteristic.PROPERTY_READ
							| BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

			dis.addCharacteristic(mansc);
			dis.addCharacteristic(monsc);
			dis.addCharacteristic(snsc);
			dis.addCharacteristic(sysIdsc);
			dis.addCharacteristic(nodebc);
			dis.addCharacteristic(localNodebc);
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
			Log.d(TAG, "RFCOMM Service is already alive.");
		}
	}

	private void unbindRfcommService() {
		if (rfcommConnection != null) {
			unbindService(rfcommConnection);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mStub;
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

	public static class QueueItem {
		MessageInfo info;

		IBLEPeripheralCallback callback;

		public QueueItem(MessageInfo info, IBLEPeripheralCallback callback) {
			super();
			this.info = info;
			this.callback = callback;
		}

		public MessageInfo getInfo() {
			return info;
		}

		public IBLEPeripheralCallback getCallback() {
			return callback;
		}

	}

	private class InQueueThread extends Thread {

		private volatile boolean isCancel = false;

		@Override
		public void run() {
			while (!isCancel) {
				Log.i(TAG,
						"手表端：启动InQueueThread,接收队列中的消息 isCancel = " + isCancel + " mInQueue size : " + mInQueue.size());
				while (!isCancel) {
					try {
						Log.i(TAG, "手表端：InQueueThreadd等待处理发送消息...");
						QueueItem item = mInQueue.take();
						MessageInfo messageInfo = item.getInfo();
						Log.d(TAG, "当前所处线程ID为" + Thread.currentThread().getId() + " Name为"
								+ Thread.currentThread().getName());
						Log.d(TAG, "手表端：inQueue take " + messageInfo.toString());
						CloudWatchRequestData data = new CloudWatchRequestData(getDeviceId().getBytes(
								Charset.forName("utf-8")), new byte[] { 1, 0 }, messageInfo.getContent(),
								ByteUtil.getByteArrayByLong(messageInfo.getTimeStamp(), 6), messageInfo.getNodeId()
										.getBytes(), messageInfo.getPackageName().getBytes(), messageInfo.getPath()
										.getBytes());
						boolean isSuccess = mRfcommClientService.write(CloudWatchParser.dataPack(data));
						setSendMessageRsp(item, isSuccess);
					} catch (InterruptedException e) {
						if (mInQueueThread != null) {
							drainQueue();
							mInQueueThread.cancel();
							mInQueue = null;
						}
						Log.d(TAG, "InterruptedException Queue remained size : " + mInQueue.size(), e);
						break;
					} catch (Exception e) {
						if (mInQueueThread != null) {
							drainQueue();
							mInQueueThread.cancel();
							mInQueue = null;
						}
						Log.d(TAG, "Exception Queue remained size " + mInQueue.size(), e);
						break;
					}
				}
			}
		}

		public void cancel() {
			isCancel = true;
		}
	}

	private void setSendMessageRsp(QueueItem item, boolean isSuccess) throws RemoteException {
		int statusCode = isSuccess ? 0 : 8;// CommonStatusCodes中8表示内部错误
		IBLEPeripheralCallback callback = item.getCallback();
		SendMessageResponse response = new SendMessageResponse();
		response.setRequestId(1);
		response.setStatusCode(statusCode);
		Log.d(TAG, "手表端：通知上层消息已处理 isSuccess = " + isSuccess);
		callback.setSendMessageRsp(response);
	}

	private String getDeviceId() {
		TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String deviceId = mTm.getDeviceId();

		Log.d(TAG, "deviceId = " + deviceId);
		return deviceId;
	}

	/**
	 * 转移队列中的数据
	 */
	private void drainQueue() {
		if (mInQueue == null || mInQueue.isEmpty()) {
			mInQueue = new LinkedBlockingQueue<QueueItem>();
		} else {
			LinkedBlockingQueue<QueueItem> newInQueue = new LinkedBlockingQueue<QueueItem>();
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

	private void onPeerConnected(NodeHolder nodeHolder) {
		Log.i(TAG,
				String.format("手表端：遍历所有NodeListener接口,发送到上层,建立连接 -> nodeId = %s and displayName = %s.",
						nodeHolder.getId(), nodeHolder.getDisplayName()));
		for (NodeListenerItem listener : mNodeListenerList) {
			Log.i(TAG,
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
		Log.i(TAG,
				String.format("手表端：遍历所有NodeListener接口,发送到上层 ,连接失败-> nodeId = %s and displayName = %s.",
						nodeHolder.getId(), nodeHolder.getDisplayName()));
		for (NodeListenerItem listener : mNodeListenerList) {
			Log.i(TAG,
					"手表端：通知上层onPeerDisconnected, node =  " + nodeHolder.getId() + ", displayName = "
							+ nodeHolder.getDisplayName());
			try {
				listener.getListener().onPeerDisconnected(nodeHolder);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

}
