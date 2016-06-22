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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

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
import com.cms.android.wearable.service.codec.CloudWatchResponseData;
import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.BleUuid;
import com.cms.android.wearable.service.vo.MessageInfo;

public class BLECentralService extends Service {

	private static final String TAG = "BLECentralService";

	public static final String BLE_CENTRAL_SERVICE_ACTION = "com.hoperun.ble.central.service";

	public static final String BLE_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.BLECentralService";

	public static final String RFCOMM_SERVICE_PACKAGE_PATH = "com.cms.android.wearable.service.impl.RFCOMMServerService";

	private LinkedBlockingQueue<QueueItem> mInQueue = new LinkedBlockingQueue<QueueItem>();

	private List<WearableListenerItem> mWearableListenerList = new ArrayList<WearableListenerItem>();

	private List<NodeListenerItem> mNodeListenerList = new ArrayList<NodeListenerItem>();
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private Map<String, Node> nodeMap = new HashMap<String, Node>();

	/**
	 * BLE连接地址
	 */
	private String mBLEAddress;

	private BluetoothGatt mBluetoothGatt;

	/**
	 * 判断当前连接服务是否支持Cloud Watch Service
	 */
	private boolean isCloudWatchServiceAvailabled = false;

	/**
	 * BLE连接状态
	 */
	private int mBLEState = BluetoothProfile.STATE_DISCONNECTED;

	private IRfcommServerService mRfcommService;

	private InQueueThread mInQueueThread;

	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.i(TAG, String.format("[onConnectionStateChange] status is %d, state from %d to %d.", status, mBLEState,
					newState));
			mBLEState = newState;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to discovery service :" + mBluetoothGatt.discoverServices());
				
				NodeHolder nodeHolder = new NodeHolder(gatt.getDevice().getAddress(), gatt.getDevice().getName() == null ?
						gatt.getDevice().getAddress() : gatt.getDevice().getName());
				nodeMap.put(nodeHolder.getId(), nodeHolder);
				onPeerConnected(nodeHolder);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.e(TAG, "Disconnected from GATT server.");
				
				NodeHolder nodeHolder = new NodeHolder(gatt.getDevice().getAddress(), gatt.getDevice().getName() == null ?
						gatt.getDevice().getAddress() : gatt.getDevice().getName());
				
				nodeMap.remove(nodeHolder.getId());
				onPeerDisconnected(nodeHolder);
				
				if (!TextUtils.isEmpty(mBLEAddress)) {
					connectBLE(mBLEAddress);
				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(TAG, "[onServicesDiscovered] status :" + status);
				isCloudWatchServiceAvailabled = false;
				try {
					// 检验是否支持Cloud Watch Service 若是不支持Clouder
					// Watch这一套BLE通信，是否需要给予提示？毕竟下面已经走不下去了
					checkCloudWatchServiceSupported();
					isCloudWatchServiceAvailabled = true;
				} catch (Exception e) {
					isCloudWatchServiceAvailabled = false;
					e.printStackTrace();
				}
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
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
			Log.w(TAG, "onCharacteristicChanged UUID:" + characteristic.getUuid().toString() + " value:"
					+ new String(characteristic.getValue()));
			if (BleUuid.CHAR_MANUFACTURER_NAME_STRING.equals(characteristic.getUuid().toString())) {
				String command = new String(characteristic.getValue());
				try {
					if (command.equals("start")) {
						Log.i(TAG, "手表端通知手机端启动RFCOMM Server服务");
						if (!isRfcommServiceAvailable() || mRfcommService == null) {
							Log.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
							bindRfcommService();
						} else if (!mRfcommService.isConnected() && !mRfcommService.isConnecting()) {
							Log.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
							mRfcommService.start();
						} else if (mInQueueThread == null || mInQueueThread.isCancel) {
							Log.i(TAG, "InQueue服务未启动,启动该服务");
							mInQueueThread = new InQueueThread();
							mInQueueThread.start();
						}
					} else if (command.equals("stop")) {
						// TODO
						Log.i(TAG, "手表端通知手机端停止RFCOMM Server Socket");
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			Log.i(TAG, "onCharacteristicWrite UUID:" + characteristic.getUuid().toString() + " value:"
					+ new String(characteristic.getValue()) + " status:" + status);
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorRead(gatt, descriptor, status);
			Log.w(TAG, "onDescriptorRead :" + status);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			Log.w(TAG, "onDescriptorWrite :" + status);
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			super.onReliableWriteCompleted(gatt, status);
			Log.w(TAG, "onReliableWriteCompleted :" + status);
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
			Log.w(TAG, "onReadRemoteRssi :" + status);
		}

	};

	private HashMap<String, IBLECentralCallback> callBackMap = new HashMap<String, IBLECentralCallback>();

	private IBLECentralService.Stub mStub = new IBLECentralService.Stub() {

		@Override
		public void connect(String address) throws RemoteException {
			Log.i(TAG, String.format("尝试连接BLE,地址为: %s.", address));
			connectBLE(address);
		}

		@Override
		public void disconnect() throws RemoteException {
			Log.i(TAG, "disconnect from BLE");
			disconnectBLE();
		}

		@Override
		public void registerCallback(String packageName, IBLECentralCallback callback) throws RemoteException {
			Log.d(TAG, String.format("[registerCallback] packageName: %s.", packageName));
			// 保证最新的callback,最好添加一套定时刷新的机制
			if (callBackMap.containsKey(packageName)) {
				callBackMap.remove(callBackMap.get(packageName));
			}
			callBackMap.put(packageName, callback);
		}

		@Override
		public void sendMessage(IBLECentralCallback callback, String node, String path, byte[] data)
				throws RemoteException {
			if (!isServiceAvailable()) {
				// 1.判定BLE连接正常
				Log.i(TAG, "BLE未连接,进行BLE连接");
				connect(mBLEAddress);
			} else if (!isRfcommServiceAvailable() || mRfcommService == null) {
				Log.i(TAG, "BLE已连接但RFCOMM未连接,进行RFCOMM连接");
				bindRfcommService();
			} else if (!mRfcommService.isConnected() && !mRfcommService.isConnecting()) {
				Log.i(TAG, "BLE和RFCOMM已连接但服务未启动,启动RFCOMM服务");
				mRfcommService.start();
			} else if (mInQueueThread == null || mInQueueThread.isCancel) {
				Log.i(TAG, "InQueue服务未启动,启动该服务");
				mInQueueThread = new InQueueThread();
				mInQueueThread.start();
			} else {
				Log.i(TAG, "服务都已经准备就绪");
			}
			MessageInfo info = new MessageInfo();
			info.setPackageName(node);
			info.setTimeStamp(new Date().getTime());
			info.setContent(data);
			info.setPath(path);
			Log.e(TAG, "发送消息已进入队列,待处理");
			putQueue(info, callback);
		}

		@Override
		public void addListener(IBLECentralCallback callback, IWearableListener listener, String node)
				throws RemoteException {
			Log.d(TAG, "手机端：准备添加监听器,当前监听器count = " + mWearableListenerList.size());
			if (callback == null) {
				Log.e(TAG, "添加监听接口失败");
				return;
			}
			if (listener == null || TextUtils.isEmpty(node)) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			mWearableListenerList.add(new WearableListenerItem(node, listener));
			callback.setStatusRsp(new Status(0));
			Log.d(TAG, "手机端：添加监听器成功,当前监听器count = " + mWearableListenerList.size());
		}

		@Override
		public void removeListener(IBLECentralCallback callback, IWearableListener listener, String node)
				throws RemoteException {
			Log.d(TAG, "手机端：准备移除监听器,Node = " + node + " 当前缓存中接口个数 = " + mWearableListenerList.size());
			if (callback == null) {
				Log.e(TAG, "添加监听接口失败");
				return;
			}
			if (listener == null || TextUtils.isEmpty(node)) {
				callback.setStatusRsp(new Status(3006));
				return;
			}
			int index = -1;
			int size = mWearableListenerList.size();
			if (size > 0) {
				for (int i = 0; i < mWearableListenerList.size(); i++) {
					WearableListenerItem item = mWearableListenerList.get(i);
					if (item.getListener().equals(listener) && item.getNode().equals(node)) {
						index = i;
						break;
					}
				}
				if (index >= 0 && index < mWearableListenerList.size()) {
					mWearableListenerList.remove(index);
				}
			}
			Log.d(TAG, "手机端：移除监听器成功,当前监听器count = " + mWearableListenerList.size());
			callback.setStatusRsp(new Status(0));
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
			if (!isBLEServiceOK()) {
				Log.e(TAG, "BLE服务未连接,同步时间失败.");
				connect(mBLEAddress);
				return false;
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
			Log.d(TAG, "手机端：同步时间 time = " + sdf.format(time));
			byte[] timeBytes = new byte[10];
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(time);
			int year = cal.get(Calendar.YEAR);
			// year 低位在前,高位在后
			timeBytes[0] = (byte) (year & 0xff);
			timeBytes[1] = (byte) (year >> 8 & 0xff);

			int l1 = timeBytes[0] < 0 ? timeBytes[0] + 256 : timeBytes[0];
			int l0 = timeBytes[1] < 0 ? timeBytes[1] + 256 : timeBytes[1];
			int as = (l0 << 8) | l1;

			Log.d(TAG, "复原as" + as);

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

			return writeCharacteristic(BleUuid.SERVICE_CURRENT_TIME, BleUuid.CHAR_CURRENT_TIME, timeBytes);
		}

		@Override
		public void addNodeListener(IBLECentralCallback callback, IWearableListener listener) throws RemoteException {
			Log.d(TAG, "手机端：准备添加node监听器,当前监听器count = " + mNodeListenerList.size());
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
			Log.d(TAG, "手机端：添加node监听器成功,当前监听器count = " + mNodeListenerList.size());
		}

		@Override
		public void removeNodeListener(IBLECentralCallback callback, IWearableListener listener) throws RemoteException {
			Log.d(TAG, "手机端：准备移除Node监听器, 当前缓存中接口个数 = " + mNodeListenerList.size());
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
			Log.d(TAG, "手机端：移除Node监听器成功,当前监听器count = " + mNodeListenerList.size());
			callback.setStatusRsp(new Status(0));			
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
			Log.d(TAG, "setGetConnectedNodesRsp " + nodeList.size());
		}

		@Override
		public void getLocalNode(IBLECentralCallback callback) throws RemoteException {
			GetLocalNodeResponse response = new GetLocalNodeResponse();
			NodeHolder node = new NodeHolder(mBluetoothAdapter.getAddress(), mBluetoothAdapter.getName());
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
	private void putQueue(MessageInfo msg, IBLECentralCallback callback) {
		// 1.CMS已经准备就绪，将消息放入Queue中等待处理
		// 2.xCMS仍然未准备就绪，将消息放入Queue,等待InQueue线程准备调用take处理
		QueueItem item = new QueueItem(msg, callback);
		mInQueue.offer(item);
		Log.d(TAG, "mInQueue size is " + mInQueue.size());
	}

	// private Timer mCountdownTimer;
	//
	// private int count = 0;

	// @SuppressLint("HandlerLeak")
	// private Handler handler = new Handler() {
	//
	// @Override
	// public void handleMessage(Message msg) {
	// super.handleMessage(msg);
	// switch (msg.what) {
	// case TIMER_START:
	// mCountdownTimer = new Timer();
	// mCountdownTimer.schedule(new TimerTask() {
	//
	// @Override
	// public void run() {
	// count++;
	// Log.d(TAG, "current count:" + count);
	// if (count == TIMER_INTERVAL) {
	// Log.d(TAG, "时间到,关闭RFCOMM通信");
	// }
	// }
	// }, new Date(), 1000);
	// break;
	// case TIMER_RESET:
	// count = 0;
	// break;
	//
	// default:
	// break;
	// }
	// }
	//
	// };

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
			Log.i(TAG, "手机端告知手表端RFCOMM服务端已准备就绪 isSuccess = " + isSuccess + " address = " + address);
			return isSuccess;
		}

		@Override
		public void onRFCOMMSocketConnected() throws RemoteException {
			Log.d(TAG, "BluetoothServerSocket connected and tell ble central service can send messages.");
			mInQueueThread = new InQueueThread();
			mInQueueThread.start();
		}

		@Override
		public void onRFCOMMSocketDisconnected(int cause, String address) throws RemoteException {
			Log.d(TAG, String.format("手机端：RFCOMM Server Socket已经断开连接 cause %d,停止InQueue Thread线程.", cause));
			if (mInQueueThread != null) {
				Log.d(TAG, "InQueueThread 已经被销毁");
				drainQueue();
				mInQueueThread.cancel();
				mInQueueThread = null;
			}
			switch (cause) {
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_EXPIRED:
				// 通知另一端断开连接
				Log.d(TAG, "手机端过期关闭RFCOMM服务,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_STOP:
				// 通知另一端关闭连接
				Log.d(TAG, "手机端关闭RFCOMM服务,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_EXCEPTION:
				// 通知另一端异常发生
				Log.d(TAG, "手机端RFCOMM发生异常,通知手表端");
				writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION, BleUuid.CHAR_MANUFACTURER_NAME_STRING, cause
						+ "&" + address);
				break;
			case RFCOMMServerService.RFCOMM_DISCONNECT_CAUSE_RESTART:
				Log.d(TAG, "手机端RFCOMM重新启动");
				// writeCharacteristic(BleUuid.SERVICE_DEVICE_INFORMATION,
				// BleUuid.CHAR_MANUFACTURER_NAME_STRING,
				// "restart");
				break;

			default:
				break;
			}

		}

		@Override
		public void onMessageReceived(byte[] bytes) throws RemoteException {
			Log.d(TAG, "Received message,prepare to unpack message.");
			CloudWatchResponseData cloudWatchResponseData = CloudWatchParser.dataUnpack(bytes);
			MessageEventHolder holder = new MessageEventHolder();
			holder.setNodeId(new String(cloudWatchResponseData.getPackageName()));
			holder.setPath(new String(cloudWatchResponseData.getPath()));
			holder.setData(cloudWatchResponseData.getData());
			Log.d(TAG, "Received message is " + holder.toString());
			onPostMessageReceived(holder);
		}

		@Override
		public void onMessageSend(String requestId, String statusCode, String versionCode) throws RemoteException {
			Iterator<Entry<String, IBLECentralCallback>> iterator = callBackMap.entrySet().iterator();
			while (iterator.hasNext()) {
				IBLECentralCallback callback = iterator.next().getValue();
				SendMessageResponse response = new SendMessageResponse();
				response.setRequestId(1);
				response.setStatusCode(0);
				response.setVersionCode(2);
				callback.setSendMessageRsp(response);
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isCloudWatchServiceAvailabled = false;
		// 解绑RFCOMM Service
		unBindRfcommService();
		// 断开BLE连接
		disconnectBLE();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "MessageClientService onBind");
		return mStub;
	}

	private void init() {
		if (!BleUtil.isBLESupported(this)) {
			Log.e(TAG, "BLE is not supported");
			return;
		}

		BluetoothManager manager = BleUtil.getManager(this);
		if (manager != null) {
			mBluetoothAdapter = manager.getAdapter();
		}
		if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
			Log.e(TAG, "Bluetooth unavailable");
			return;
		}
		bindRfcommService();
	}

	/**
	 * BLE连接
	 * 
	 * @param address
	 * @return
	 */
	private boolean connectBLE(String address) {
		if (mBluetoothAdapter == null || TextUtils.isEmpty(address)) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
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

		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBLEAddress = address;
		mBLEState = BluetoothProfile.STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnectBLE() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
		close();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * 判断BLE Service是否可用：连接成功+可用服务完全
	 * 
	 * @return
	 */
	private boolean isServiceAvailable() {
		Log.d(TAG,
				("判定BLE服务是否可用isConnected = " + (mBLEState == BluetoothProfile.STATE_CONNECTED) + " isAvailabled = " + isCloudWatchServiceAvailabled));
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
	 * 判断RFCOMM Service是否存在
	 * 
	 * @return
	 */
	private boolean isRfcommServiceAvailable() {
		return BleUtil.isServiceRunning(this, RFCOMM_SERVICE_PACKAGE_PATH);
	}

	/**
	 * 绑定Rfcomm服务连接
	 */
	private void bindRfcommService() {
		if (!isRfcommServiceAvailable()) {
			Intent intent = new Intent(this, RFCOMMServerService.class);
			bindService(intent, rfcommConnection, Context.BIND_AUTO_CREATE);
		} else {
			Log.d(TAG, "RFCOMM Service is alive.");
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
		Log.d(TAG, "service char size: "
				+ mBluetoothGatt.getService(UUID.fromString(BleUuid.SERVICE_CLOUDER_WATCH)).getCharacteristics().size());
		return mBluetoothGatt.getServices();
	}

	private boolean writeCharacteristic(String serviceUuid, String characteristicUuid, String value) {
		if (mBluetoothGatt == null) {
			return false;
		}
		BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
		if (service == null) {
			Log.w(TAG, "Service NOT found :" + serviceUuid);
			return false;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
		if (characteristic == null) {
			Log.w(TAG, "Characteristic NOT found :" + characteristicUuid);
			return false;
		}
		characteristic.setValue(value);
		Log.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	private boolean writeCharacteristic(String serviceUuid, String characteristicUuid, byte[] bytes) {
		if (mBluetoothGatt == null) {
			return false;
		}
		BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceUuid));
		if (service == null) {
			Log.w(TAG, "Service NOT found :" + serviceUuid);
			return false;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
		if (characteristic == null) {
			Log.w(TAG, "Characteristic NOT found :" + characteristicUuid);
			return false;
		}
		characteristic.setValue(bytes);
		Log.d(TAG, "writeCharacteristic 写入值为" + new String(characteristic.getValue()));
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
			Log.e(TAG, "Cloud Watch Service不支持");
			return;
		}

		BluetoothGattCharacteristic rfcommCharacteristic = cloudWatchGattService.getCharacteristic(UUID
				.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING));
		if (rfcommCharacteristic == null) {
			Log.e(TAG, "RFCOMM BLUETOOTH COMMAND不支持");
		} else {
			mBluetoothGatt.setCharacteristicNotification(rfcommCharacteristic, true);
		}
	}

	public static class QueueItem {
		MessageInfo info;

		IBLECentralCallback callback;

		private QueueItem(MessageInfo info, IBLECentralCallback callback) {
			super();
			this.info = info;
			this.callback = callback;
		}

		public MessageInfo getInfo() {
			return info;
		}

		public IBLECentralCallback getCallback() {
			return callback;
		}

	}

	private class InQueueThread extends Thread {

		private volatile boolean isCancel = false;

		@Override
		public void run() {
			Log.i(TAG, "手机端：启动InQueueThread,接收队列中的消息 isCancel = " + isCancel + " mInQueue size : " + mInQueue.size());
			while (!isCancel) {
				try {
					Log.d(TAG, "手机端：InQueueThreadd等待接收队列中的消息...");
					QueueItem item = mInQueue.take();
					MessageInfo messageInfo = item.getInfo();
					Log.d(TAG, "手机端：inQueue take " + messageInfo.toString());
					CloudWatchRequestData data = new CloudWatchRequestData(getDeviceId().getBytes(
							Charset.forName("utf-8")), new byte[] { 1, 0 }, messageInfo.getContent(),
							ByteUtil.getByteArrayByLong(messageInfo.getTimeStamp(), 6), messageInfo.getPackageName()
									.getBytes(), messageInfo.getPath().getBytes());
					boolean isSuccess = mRfcommService.write(CloudWatchParser.dataPack(data));
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

		public void cancel() {
			isCancel = true;
		}
	}

	private void setSendMessageRsp(QueueItem item, boolean isSuccess) throws RemoteException {
		int statusCode = isSuccess ? 0 : 8;// CommonStatusCodes中8表示内部错误
		IBLECentralCallback callback = item.getCallback();
		SendMessageResponse response = new SendMessageResponse();
		response.setRequestId(1);
		response.setStatusCode(statusCode);
		Log.d(TAG, "手机端：通知上层消息已处理 isSuccess = " + isSuccess);
		callback.setSendMessageRsp(response);
	}

	public static class WearableListenerItem {

		private String node;

		private IWearableListener listener;

		public WearableListenerItem() {
			super();
		}

		public WearableListenerItem(String node, IWearableListener listener) {
			super();
			this.node = node;
			this.listener = listener;
		}

		public String getNode() {
			return node;
		}

		public void setNode(String node) {
			this.node = node;
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

	private void onPostMessageReceived(MessageEventHolder messageEventHolder) {
		Log.i(TAG, String.format("手机端：遍历所有WearableListener接口,发送消息到上层 -> node = %s   path = %s and data = %s.",
				messageEventHolder.getSourceNodeId(), messageEventHolder.getPath(),
				new String(messageEventHolder.getData())));
		for (WearableListenerItem listener : mWearableListenerList) {
			Log.i(TAG, "手机端：当前接收消息Node为 " + messageEventHolder.getSourceNodeId() + " 遍历接口Node为 " + listener.getNode());
			if (listener.getNode().equals(messageEventHolder.getSourceNodeId())) {
				try {
					Log.i(TAG, "手机端：Node节点匹配,通知上层接收消息");
					listener.getListener().onMessageReceived(messageEventHolder);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void onPeerConnected(NodeHolder nodeHolder) {
		Log.i(TAG, String.format("手机端：遍历所有NodeListener接口,发送到上层,建立连接 -> nodeId = %s and displayName = %s.",
				nodeHolder.getId(), nodeHolder.getDisplayName()));
		for (NodeListenerItem listener : mNodeListenerList) {
			Log.i(TAG, "手机端：通知上层onPeerConnected, node =  " + nodeHolder.getId() + ", displayName = " + nodeHolder.getDisplayName());
			try {
				listener.getListener().onPeerConnected(nodeHolder);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void onPeerDisconnected(NodeHolder nodeHolder) {
		Log.i(TAG, String.format("手机端：遍历所有NodeListener接口,发送到上层 ,连接失败-> nodeId = %s and displayName = %s.",
				nodeHolder.getId(), nodeHolder.getDisplayName()));
		for (NodeListenerItem listener : mNodeListenerList) {
			Log.i(TAG, "手机端：通知上层onPeerDisconnected, node =  " + nodeHolder.getId() + ", displayName = " + nodeHolder.getDisplayName());
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
			mInQueue = new LinkedBlockingQueue<QueueItem>();
		} else {
			LinkedBlockingQueue<QueueItem> newInQueue = new LinkedBlockingQueue<QueueItem>();
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

		Log.d(TAG, "deviceId = " + deviceId);
		return deviceId;
	}

}
