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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.wearable.service.codec.CloudWatchParser;
import com.cms.android.wearable.service.codec.StringUtil;

public class RFCOMMServerService extends Service {

	private static final String TAG = "RFCOMMService";

	private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

	public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

	public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

	public static final int RFCOMM_DISCONNECT_CAUSE_EXCEPTION = 2;

	public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

	public static final int RFCOMM_CONNECT_READY = 4;

	private static final int MESSAGE_TYPE_TIMER_OCCUR = 1;

	private static final int MESSAGE_TIME_INITAL = 15;

	private static final int MESSAGE_TIME_INTERVAL = 15;

	private ScheduledFuture<?> future;

	public static final String RFCOMM_SERVER_SERVICE_ACTION = "com.hoperun.rfcomm.server.service";

	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private AcceptThread mAcceptThread;

	private ConnectedThread mConnectedThread;

	private String mBluetoothMacAddress;

	private IRfcommServerCallback mRfcommServerCallback;

	private ScheduledExecutorService mScheduledService = Executors.newSingleThreadScheduledExecutor();

	private long mExpiredTime = new Date().getTime() + 30 * 1000;

	// Constants that indicate the current rfcomm connection state
	// we're doing nothing
	public static final int STATE_NONE = 0;
	// now listening for incoming connections
	public static final int STATE_LISTEN = 1;
	// now initiating an outgoing connection
	public static final int STATE_CONNECTING = 2;
	// now connected to a remote device
	public static final int STATE_CONNECTED = 3;

	private int mState = STATE_NONE;

	@SuppressLint("HandlerLeak")
	private Handler mTimeHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_TYPE_TIMER_OCCUR:
				if (isRFCOMMExpired()) {
					Log.e(TAG, "手机端：当前已经超时,则关闭RFCOMM通道");
					stopThread(RFCOMM_DISCONNECT_CAUSE_EXPIRED);
				} else {
					Log.d(TAG, "手机端：当前未超时,则不做任何处理");
				}
				break;

			default:
				break;
			}
		}
	};

	private IRfcommServerService.Stub mStub = new IRfcommServerService.Stub() {

		@Override
		public IBinder asBinder() {
			return null;
		}

		@Override
		public void stop() throws RemoteException {
			Log.d(TAG, "stop rfcomm thread.");
			stopThread(RFCOMM_DISCONNECT_CAUSE_STOP);
		}

		@Override
		public void start() throws RemoteException {
			validateExpireTime();
			startThread();
		}

		@Override
		public void registerCallback(IRfcommServerCallback callback) throws RemoteException {
			mRfcommServerCallback = callback;
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return isServiceConnected();
		}

		@Override
		public boolean write(byte[] bytes) throws RemoteException {
			validateExpireTime();
			return writeBytes(bytes);
		}

		@Override
		public boolean isConnecting() throws RemoteException {
			return isServiceConnecting();
		}

	};

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mScheduledService.shutdownNow();
		stopThread(RFCOMM_DISCONNECT_CAUSE_STOP);
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

	private class AcceptThread extends Thread {

		private BluetoothServerSocket serverSocket = null;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			try {
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				// int sdk = VERSION.SDK_INT;
				tmp = adapter.listenUsingRfcommWithServiceRecord("CloudWatchService", RFCOMM_UUID);
				// if (sdk >= 10) {
				// tmp =
				// adapter.listenUsingInsecureRfcommWithServiceRecord("CloudWatchService",
				// RFCOMM_UUID);
				// } else {
				// tmp =
				// adapter.listenUsingRfcommWithServiceRecord("CloudWatchService",
				// RFCOMM_UUID);
				// }
				mBluetoothMacAddress = adapter.getAddress();
				Log.d(TAG, "当前监听地址为 " + mBluetoothMacAddress);
				setState(STATE_LISTEN);
			} catch (IOException e) {
				setState(STATE_NONE);
				Log.e(TAG, "AcceptThread 监听RFCOMM接口通道创建异常", e);
				return;
			} // 服务仅监听
			serverSocket = tmp;
			Log.d(TAG, "手机端：serverSocket hashcode = " + serverSocket.hashCode());
		}

		public void run() {
			setName("RfcommThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
//			int count = 0;
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					// 发送个回调消息给BLE Service告诉他rfcomm已经准备就绪了
					if (mRfcommServerCallback != null) {
						Log.d(TAG, "RFCOMM Thread Socket 已经准备就绪");
						boolean isSuccess = mRfcommServerCallback.onRFCOMMSocketReady(mBluetoothMacAddress);
//						count++;
//						if (!isSuccess) {
//							Log.e(TAG, "手机端告知手表端准备就绪失败,则重新告知 count = ");
//							if (count == 3) {
//								Log.e(TAG, "已告知3次都失败,则断开");
//								break;
//							} else {
//								continue;
//							}
//						}
					}
					setState(STATE_LISTEN);
					Log.d(TAG, "RfcommThread serverSocket accept waiting...");
					socket = serverSocket.accept();
				} catch (IOException e) {
					Log.d(TAG, "Accept Thread IOException", e);
					setState(STATE_NONE);
				} catch (RemoteException e) {
					Log.d(TAG, "Accept Thread RemoteException", e);
					setState(STATE_NONE);
				}
				// If a connection was accepted
				Log.d(TAG, "RFCOMM Thread serverSocket pass accept and mState = " + mState);
				if (socket != null) {
					synchronized (RFCOMMServerService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							Log.e(TAG, "STATE_LISTEN or STATE_CONNECTING connectedRfcomm");
							// Situation normal. Start the connected thread.
							connectedRfcomm(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							Log.e(TAG, "STATE_NONE or STATE_CONNECTED socket close");
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
		}

		public void cancel() {
			// 取消套接字连接，然后线程返回
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {

		private final BluetoothSocket socket;

		private final InputStream inStream;
		private final OutputStream outStream;

		public volatile boolean isStop = false;

		public ConnectedThread(BluetoothSocket socket) {
			this.socket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "ConnectedThread InputStream and OutputStream Exception", e);
			}
			inStream = tmpIn;
			outStream = tmpOut;
		}

		public void run() {
			future = mScheduledService.scheduleAtFixedRate(new Runnable() {

				@SuppressLint("SimpleDateFormat")
				@Override
				public void run() {
					Log.d(TAG, "当前计时任务触发,当前时间为" + sdf.format(new Date()));
					mTimeHandler.sendEmptyMessage(MESSAGE_TYPE_TIMER_OCCUR);
				}
			}, MESSAGE_TIME_INITAL, MESSAGE_TIME_INTERVAL, TimeUnit.SECONDS);

			onPostRFCOMMSocketConnected();

			while (!isStop) {
				LinkDecoderState state = LinkDecoderState.ReadA;
				byte[] ABCDEFG = null;
				try {
					switch (state) {
					case ReadA:
						int readLength = inStream.read();
						validateExpireTime();
						if (readLength == -1) {
							throw new IOException("Connection has been closed!");
						}
						byte A1 = (byte) readLength;

						if (A1 != CloudWatchParser.KEY_REQUEST_START) {
							Log.e(TAG,
									String.format("The package is invaild start 0 (%s)", StringUtil.getHexString(A1)));
							continue;
						}
						readLength = inStream.read();
						validateExpireTime();
						if (readLength == -1) {
							throw new IOException("Connection has been closed!");
						}
						byte A2 = (byte) readLength;
						if (A2 != CloudWatchParser.KEY_REQUEST_START) {
							Log.e(TAG,
									String.format("The package is invaild start 1 (%s)", StringUtil.getHexString(A2)));
							continue;
						}
						state = LinkDecoderState.ReadBCDEFG;
					case ReadBCDEFG:
						byte[] B = new byte[4];
						readLength = inStream.read(B);
						validateExpireTime();
						if (readLength == -1) {
							throw new IOException("Connection has been closed!");
						}
						int l0 = B[0] < 0 ? B[0] + 256 : B[0];
						int l1 = B[1] < 0 ? B[1] + 256 : B[1];
						int l2 = B[2] < 0 ? B[2] + 256 : B[2];
						int l3 = B[3] < 0 ? B[3] + 256 : B[3];
						
						int packageLength = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00)
								| l0 & 0xFF;
						Log.d(TAG, "The package all length = " + String.valueOf(packageLength));

						ABCDEFG = new byte[packageLength];
						ABCDEFG[0] = CloudWatchParser.KEY_REQUEST_START;
						ABCDEFG[1] = CloudWatchParser.KEY_REQUEST_START;
						ABCDEFG[2] = B[0];
						ABCDEFG[3] = B[1];
						ABCDEFG[4] = B[2];
						ABCDEFG[5] = B[3];
						// read C D E F G
						byte[] CDEFG = new byte[packageLength - 6];
						int readIndex = 0;
						while (readIndex < packageLength - 6) {
							readLength = inStream.read(CDEFG);
							validateExpireTime();
							Log.d(TAG, "The byte length from io = " + readLength);
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

						if (ABCDEFG[ABCDEFG.length - 2] == CloudWatchParser.KEY_END_0
								&& ABCDEFG[ABCDEFG.length - 1] == CloudWatchParser.KEY_END_1) {
							state = LinkDecoderState.ReadA;
							if (CloudWatchParser.checkDataPack(ABCDEFG)) {
								if (mRfcommServerCallback != null) {
									mRfcommServerCallback.onMessageReceived(ABCDEFG);
								}
							} else {
								Log.e(TAG,
										String.format("The package is invaild crc. (%s)",
												StringUtil.getHexString(ABCDEFG)));
							}
						} else {
							state = LinkDecoderState.ReadA;
							Log.e(TAG,
									String.format("The package is invaild ending. (%s)",
											StringUtil.getHexString(ABCDEFG)));
						}
						break;
					default:
						state = LinkDecoderState.ReadA;
						throw new Error("Shouldn't reach here.");
					}

				} catch (Exception e) {
					String message = "Cannot get package data.";
					if (ABCDEFG != null) {
						message = Arrays.toString(ABCDEFG);
					}
					Log.e(TAG, "PackageData Exception: " + message, e);
					state = LinkDecoderState.ReadA;
					// 即不是主动断开连接的,则发生了异常
					if (future != null) {
						future.cancel(false);
					}
					if (!isStop) {
						Log.d(TAG, "isStop = " + isStop + ",非主动关闭,则重新启动RFCOMM Server服务,通知手表端手机端发生异常");
						reconnect();
						onPostRFCOMMSocketDisconnected(RFCOMM_DISCONNECT_CAUSE_EXCEPTION);
					}
					break;
				}
			}
		}

		public boolean writeBytes(byte[] bytes) {
			try {
				outStream.write(bytes);
				return true;
			} catch (Exception e) {
				Log.e(TAG, "手机端：写入Byte数组异常", e);
			}
			return false;
		}

		public void cancel() {
			isStop = true;
			try {
				if (inStream != null) {
					inStream.close();
				}
				if (outStream != null) {
					outStream.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private void reconnect() {
		Log.d(TAG, "手机端：RFCOMM连接异常,断开连接,尝试重新连接");
		try {
			Log.d(TAG, "手机端：休眠 1000ms");
			Thread.sleep(1000);
			Log.d(TAG, "手机端：尝试重新创建RFCOMM Server...");
			startThread();
		} catch (InterruptedException e) {
			Log.e(TAG, "手机端：reconnect InterruptedException", e);
		}
	}

	/**
	 * 通知上层RFCOMM连接断开
	 * 
	 * @param cause
	 *            断开原因
	 */
	private void onPostRFCOMMSocketDisconnected(int cause) {
		if (mRfcommServerCallback != null) {
			try {
				mRfcommServerCallback.onRFCOMMSocketDisconnected(cause, mBluetoothMacAddress);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
		setState(STATE_NONE);
	}

	/**
	 * 通知上层RFCOMM建立连接
	 */
	private void onPostRFCOMMSocketConnected() {
		try {
			if (mRfcommServerCallback != null) {
				mRfcommServerCallback.onRFCOMMSocketConnected();
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

	private synchronized void startThread() {
		Log.i(TAG, "创建线程,准备启动RFCOMM服务");
		stopThread(RFCOMM_DISCONNECT_CAUSE_RESTART);
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connectedRfcomm(BluetoothSocket socket, BluetoothDevice device) {

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_NONE);

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		setState(STATE_CONNECTED);
	}

	private synchronized void stopThread(int cause) {
		onPostRFCOMMSocketDisconnected(cause);
		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// Cancel the accept thread because we only want to connect to one
		// device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean writeBytes(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return false;
			r = mConnectedThread;
			return r.writeBytes(out);
		}
		// Perform the write unsynchronized
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		Log.d(TAG, "手机端：setState() RFCOMM连接状态 " + getStateText(mState) + " -> " + getStateText(state));
		mState = state;
		// Give the new state to the Handler so the UI Activity can update
	}

	private boolean isServiceConnected() {
		return mState == STATE_CONNECTED;
	}

	private boolean isServiceConnecting() {
		return mState == STATE_CONNECTING;
	}

	public enum LinkDecoderState {
		ReadA, ReadBCDEFG
	}

	/**
	 * 更新RFCOMM服务过期时间
	 */
	private void validateExpireTime() {
		// 暂时使过期时间设长些
		mExpiredTime = new Date().getTime() + 30 * 1000;
		Log.d(TAG, "手机端：过期时间更新为" + sdf.format(mExpiredTime));
	}

	/**
	 * 判定RFCOMM是否过期
	 * 
	 * @param time
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	private boolean isRFCOMMExpired() {
		Log.d(TAG, "判定是否超时 当前时间 = " + sdf.format(new Date()) + " 超时时间 = " + sdf.format(mExpiredTime));
		return new Date().getTime() > mExpiredTime;
	}

	private String getStateText(int state) {
		String stateText = "";
		switch (state) {
		case STATE_NONE:
			stateText = "NONE";
			break;
		case STATE_LISTEN:
			stateText = "LISTEN";
			break;
		case STATE_CONNECTING:
			stateText = "CONNECTING";
			break;
		case STATE_CONNECTED:
			stateText = "CONNECTED";
			break;

		default:
			break;
		}
		return stateText;
	}

}
