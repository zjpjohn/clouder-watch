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
import java.util.Arrays;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.cms.android.wearable.internal.NodeHolder;
import com.cms.android.wearable.service.codec.ResponseData;
import com.cms.android.wearable.service.codec.ResponseParser;
import com.cms.android.wearable.service.codec.StringUtil;
import com.cms.android.wearable.service.codec.TransportParser;
import com.cms.android.wearable.service.common.BleUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.impl.BLEPeripheralService.LinkDecoderState;

/**
 * ClassName: RFCOMMClientService
 * 
 * @description RFCOMMClientService
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class RFCOMMClientService extends Service {

	private static final String TAG = "RFCOMMClientService";

	// Constants that indicate the current rfcomm connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	// now listening for incoming connections
	public static final int STATE_LISTEN = 1;
	// now initiating an outgoing connection
	public static final int STATE_CONNECTING = 2;
	// now connected to a remote device
	public static final int STATE_CONNECTED = 3;

	private int mState = STATE_NONE;

	public static final int RFCOMM_DISCONNECT_CAUSE_EXPIRED = 0;

	public static final int RFCOMM_DISCONNECT_CAUSE_STOP = 1;

	public static final int RFCOMM_DISCONNECT_CAUSE_EXCEPTION = 2;

	public static final int RFCOMM_DISCONNECT_CAUSE_RESTART = 3;

	public static final int RFCOMM_CONNECT_READY = 4;

	private int RECONNECT_MAX_TIMES = 5;

	private int mRemainingConnectCount = 0;

	private int mPhoneSideStatus = -1;

	// Unique UUID for this application
	private static final UUID RFCOMM_UUID = UUID.fromString("00002101-0000-1000-8000-00805F9B34FB");

	private ConnectThread mConnectThread;

	private ConnectedThread mConnectedThread;

	private String mRfcommAddress;

	private IRfcommClientCallback mRfcommClientCallback;

	private IRfcommClientService.Stub mStub = new IRfcommClientService.Stub() {

		@Override
		public IBinder asBinder() {
			return null;
		}

		@Override
		public void registerCallback(IRfcommClientCallback callback) throws RemoteException {
			mRfcommClientCallback = callback;
		}

		@Override
		public void start(String address) throws RemoteException {
			/**
			 * 每次主动连接时设置5次最大连接次数
			 */
			mRemainingConnectCount = RECONNECT_MAX_TIMES;
			startThread(address);
		}

		@Override
		public void restart() throws RemoteException {
			startThread();
		}

		@Override
		public void stop() throws RemoteException {
			LogTool.d(TAG, "stop rfcomm thread.");
			stopThread(RFCOMM_DISCONNECT_CAUSE_STOP);
		}

		@Override
		public boolean write(byte[] bytes) throws RemoteException {
			return writeBytes(bytes);
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return isServiceConnected();
		}

		@Override
		public void setStatus(int status) throws RemoteException {
			mPhoneSideStatus = status;
		}

		@Override
		public void setTotalTransportSuccess(String uuid) throws RemoteException {
			setTotalTransportRsp(uuid);
		}
	};

	private boolean isServiceConnected() {
		return mState == STATE_CONNECTED;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mStub;
	}

	private class ConnectThread extends Thread {

		private final BluetoothSocket socket;
		private final BluetoothDevice device;

		public ConnectThread(String address) {
			this.device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
			BluetoothSocket tmp = null;
			try {
				int sdk = VERSION.SDK_INT;
				if (sdk >= 10) {
					tmp = this.device.createInsecureRfcommSocketToServiceRecord(RFCOMM_UUID);
				} else {
					tmp = this.device.createRfcommSocketToServiceRecord(RFCOMM_UUID);
				}
				// tmp =
				// this.device.createRfcommSocketToServiceRecord(RFCOMM_UUID);
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket = tmp;
			LogTool.d(TAG, "手表端：socket hashcode = " + socket.hashCode());
		}

		public void run() {
			setName("ConnectThread");
			LogTool.d(TAG, "当前连接设备为 " + this.device.getName() + " 地址为 " + this.device.getAddress());
			// Make a connection to the BluetoothSocket
			try {
				mRemainingConnectCount--;
				if (mRemainingConnectCount < 0) {
					mRemainingConnectCount = 0;
				}
				// This is a blocking call and will only return on a
				// successful connection or an exception
				socket.connect();
				onPostRFCOMMSocketConnected(device);
				BLEPeripheralService.isRFCOMMConnected = true;
			} catch (Exception e) {
				// Close the socket
				try {
					socket.close();
				} catch (Exception e2) {
					LogTool.e(TAG, "unable to close() " + " socket during connection failure", e2);
				}
				LogTool.e(TAG, "ConnectThread创建通道异常", e);
				setState(STATE_NONE);
				if (mPhoneSideStatus == RFCOMM_CONNECT_READY && mRemainingConnectCount > 0) {
					LogTool.i(TAG, "手机端准备就绪,但是连接发生异常(经常性),重新连接");
					reconnect();
				}
				
				if (mRemainingConnectCount == 0) {
					onConnectFailure(device);
				}
				return;
			}
			// Reset the ConnectThread because we're done
			synchronized (RFCOMMClientService.this) {
				mConnectedThread = null;
			}

			setState(STATE_CONNECTED);
			connected(socket);
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				LogTool.e(TAG, "close() of connect " + " socket failed", e);
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

		private volatile boolean isStop = false;

		public ConnectedThread(BluetoothSocket socket) {
			LogTool.d(TAG, "create ConnectedThread");
			this.socket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				LogTool.e(TAG, "temp socket's InStream and OutStream not created", e);
			}

			inStream = tmpIn;
			outStream = tmpOut;
		}

		public void run() {

			while (!isStop) {
				LinkDecoderState state = LinkDecoderState.ReadA;
				byte[] ABCDEFG = null;
				try {
					switch (state) {
					case ReadA:
						int readLength = inStream.read();
						if (readLength == -1) {
							throw new IOException("Connection has been closed!");
						}
						byte A1 = (byte) readLength;

						if (A1 != TransportParser.KEY_REQUEST_START) {
							LogTool.e(TAG,
									String.format("The package is invaild start 0 (%s)", StringUtil.getHexString(A1)));
							continue;
						}
						readLength = inStream.read();
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
						readLength = inStream.read(B);
						if (readLength == -1) {
							throw new IOException("Connection has been closed!");
						}
						int l0 = B[0] < 0 ? B[0] + 256 : B[0];
						int l1 = B[1] < 0 ? B[1] + 256 : B[1];
						int l2 = B[2] < 0 ? B[2] + 256 : B[2];
						int l3 = B[3] < 0 ? B[3] + 256 : B[3];
						int packageLength = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00)
								| l0 & 0xFF;
						LogTool.d(TAG, "The package all length = " + String.valueOf(packageLength));

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
							readLength = inStream.read(CDEFG);
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

						if (ABCDEFG[ABCDEFG.length - 2] == TransportParser.KEY_END_0
								&& ABCDEFG[ABCDEFG.length - 1] == TransportParser.KEY_END_1) {
							state = LinkDecoderState.ReadA;
							if (TransportParser.checkDataPack(ABCDEFG)) {
								byte type = ABCDEFG[6];
								LogTool.d(TAG, "type->" + type);
								if (mRfcommClientCallback != null) {
									switch (type) {
									case TransportParser.REQUEST_PARSER_TYPE:
										LogTool.d(TAG, "type-> REQUEST");
										setTransportRsp(ABCDEFG, ResponseData.RESPONSE_STATUS_SUCCESS);
										mRfcommClientCallback.onDataReceived(ABCDEFG);
										break;
									case TransportParser.RESPONSE_PARSER_TYPE:
										LogTool.d(TAG, "type-> RESPONSE");
										mRfcommClientCallback.onDataSent(ABCDEFG);
										break;
									default:
										LogTool.e(TAG, "Unknown type");
										break;
									}
								} else {
									LogTool.e(TAG, "mRfcommServerCallback is null");
								}
							} else {
								LogTool.e(
										TAG,
										String.format("The package is invaild crc. (%s)",
												StringUtil.getHexString(ABCDEFG)));
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
						throw new Error("Shouldn't reach here.");
					}
				} catch (IOException e) {
					String message = "Cannot get package data.";
					if (ABCDEFG != null) {
						message = Arrays.toString(ABCDEFG);
					}
					LogTool.e(TAG, "IOException :" + message, e);
					state = LinkDecoderState.ReadA;
					LogTool.e(TAG, "当前异常后状态 isStop = " + isStop + " status = " + mPhoneSideStatus);
					if (!isStop
							&& (mPhoneSideStatus != RFCOMM_DISCONNECT_CAUSE_EXPIRED && mPhoneSideStatus != RFCOMM_DISCONNECT_CAUSE_STOP)) {
						// 1.过期为主动断开 2.stop也是主动断开 这2者为手机端主动断开连接,则无需重连
						LogTool.d(TAG, "ConnectedThread run 中发生异常,RFCOMM client重新建立连接.");
						mRemainingConnectCount = RECONNECT_MAX_TIMES;
						reconnect();
					} else {
						// 非主动连接,则如实关闭连接，通知BLE关闭InQueueThread
						stopThread(mPhoneSideStatus);
					}
					break;
				} catch (RemoteException e) {
					LogTool.e(TAG, "RemoteException", e);
					e.printStackTrace();
				}
			}
		}

		public synchronized boolean writeBytes(byte[] bytes) {
			try {
				outStream.write(bytes);
				return true;
			} catch (Exception e) {
				LogTool.e(TAG, "写入Byte数组异常", e);
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
				LogTool.e(TAG, "close() of connect socket failed", e);
			}
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
	}

	private void startThread(String address) {
		stopThread(RFCOMM_DISCONNECT_CAUSE_RESTART);
		mRfcommAddress = address;
		LogTool.i(TAG, "start rfcomm connection and address is " + address);
		if (!TextUtils.isEmpty(address) && BleUtil.isBluetoothAddress(address)) {
			setState(STATE_CONNECTING);
			mConnectThread = new ConnectThread(address);
			mConnectThread.start();
		} else {
			LogTool.e(TAG, "Try to start rfcomm connection but address is null or invalid.");
		}
	}

	private void startThread() {
		startThread(mRfcommAddress);
	}

	/**
	 * Stop all threads include AcceptThread InQueueThread and ConnectedThread
	 */
	public synchronized void stopThread(int cause) {

		onPostRFCOMMSocketDisconnected(cause);

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		setState(STATE_NONE);
	}

	private void reconnect() {
		LogTool.d(TAG, "RFCOMM连接异常,断开连接,尝试重新连接");
		try {
			LogTool.d(TAG, "休眠 2000ms");
			Thread.sleep(2000);
			LogTool.d(TAG, "手表端尝试重新连接...");
			Thread startThread = new Thread(new Runnable() {
				@Override
				public void run() {
					startThread();
				}
			});
			startThread.start();
		} catch (InterruptedException e) {
			LogTool.e(TAG, "reconnect InterruptedException", e);
		}
	}

	private synchronized void connected(BluetoothSocket socket) {
		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
	}

	/**
	 * 通知上层RFCOMM建立连接
	 */
	private void onPostRFCOMMSocketConnected(BluetoothDevice device) {
		try {
			if (mRfcommClientCallback != null) {
				mRfcommClientCallback.onRFCOMMSocketConnected(device);
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 通知上层RFCOMM连接断开
	 * 
	 * @param cause
	 *            断开原因
	 */
	private void onPostRFCOMMSocketDisconnected(int cause) {
		if (mRfcommClientCallback != null) {
			try {
				mRfcommClientCallback.onRFCOMMSocketDisconnected(cause);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
		setState(STATE_NONE);
	}
	
	/**
	 * 通知上层RFCOMM连接失败
	 */
	private void onConnectFailure(BluetoothDevice device) {
		try {
			if (mRfcommClientCallback != null) {
				mRfcommClientCallback.onConnectFailure(device);
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * send response
	 * 
	 * @param data
	 * @param flag
	 */
	private void setTransportRsp(byte[] data, int flag) {
		if (data == null) {
			LogTool.e(TAG, "[setTransportRsp] data null error");
			return;
		}
		byte[] ids = new byte[36];
		if (data.length < 44) {
			LogTool.e(TAG, "[setTransportRsp] data length = " + (data == null ? 0 : data.length)
					+ " but want size >= 44");
			return;
		}
		System.arraycopy(data, 7, ids, 0, 36);
		ResponseData responseData = new ResponseData(new String(ids), flag);
		// 若是外层包不符合要求，则获取其UUID发送失败标志位
		LogTool.i(TAG, String.format(
				"[setTransportRsp(0 = failed,1 = success,2 = total success)] uuid  = %s and flag = %s.",
				new String(ids), flag));
		writeBytes(ResponseParser.dataPack(responseData));
	}

	private void setTotalTransportRsp(String uuid) {
		if (TextUtils.isEmpty(uuid)) {
			LogTool.e(TAG, "[setTotalTransportRsp] uuid null error");
			return;
		}
		ResponseData responseData = new ResponseData(uuid, ResponseData.RESPONSE_STATUS_TOTAL_SUCCESS);
		LogTool.i(TAG, String.format("[setTotalTransportRsp] uuid  = %s.", uuid));
		writeBytes(ResponseParser.dataPack(responseData));
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		LogTool.d(TAG, "手机端：setState() RFCOMM连接状态 " + getStateText(mState) + " -> " + getStateText(state));
		mState = state;
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
