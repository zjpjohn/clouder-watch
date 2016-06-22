package com.cms.android.wearable.service.codec;
///*****************************************************************************
// *
// *                      HOPERUN PROPRIETARY INFORMATION
// *
// *          The information contained herein is proprietary to HopeRun
// *           and shall not be reproduced or disclosed in whole or in part
// *                    or used for any design or manufacture
// *              without direct written authorization from HopeRun.
// *
// *            Copyright (c) 2012 by HopeRun.  All rights reserved.
// *
// *****************************************************************************/
//package com.hoperun.link;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.Socket;
//import java.net.SocketException;
//import java.net.SocketTimeoutException;
//import java.nio.charset.Charset;
//import java.util.Arrays;
//
//import android.app.Service;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.Messenger;
//import android.os.RemoteException;
//import android.util.Log;
//
///**
// * 
// * ClassName: DaemonService
// * 
// * @description
// * @author hu_wg
// * @Date Jan 14, 2014
// * 
// */
//
//public class DaemonService extends Service {
//	Context context=null;
//	private final static String LINK_SERVER_HOST = "10.20.70.222";
//	private final static int LINK_SERVER_PORT = 8024;
//
//	public final static byte KEY_REQUEST_START = 64;// @
//	public final static byte KEY_RESPONSE_START = 36;// $
//	public final static byte KEY_END_0 = 0x0d;// \r
//	public final static byte KEY_END_1 = 0x0a;// \n
//
//	private static final String JOYPLUS_SERVICE_PACKAGE_NAME = "com.hoperun.feiying.searcher";
//	private static final String JOYPLUS_SERVICE_CLASS_NAME = "com.hoperun.feiying.searcher.service.JoyplusSearchService";
//
//	// private static byte[] deviceNo = new byte[] { 01, 01, 01, 01, 01, 01, 01,
//	// 01, 01, 48, 49, 50, 52, 53, 48, 48, 51,
//	// 54, 52, 0 };
//	private static final byte[] deviceNo = ServiceApplication.get20DeviceId();
//
//	private Messenger joyplusServiceMessenger = null;
//
//	private ServiceConnection serviceConnection = new ServiceConnection() {
//		@Override
//		public void onServiceConnected(ComponentName className, IBinder service) {
//			LogUtil.v("***** Daemon Service connected joyplus search service *****");
//			joyplusServiceMessenger = new Messenger(service);
//
//		}
//
//		@Override
//		public void onServiceDisconnected(ComponentName className) {
//			getApplicationContext().unbindService(serviceConnection);
//			LogUtil.v("222222222222222***** Daemon Service disconnected joyplus search service *****");
//			// 绑定到joyplus search service
//			ComponentName componetName = new ComponentName(JOYPLUS_SERVICE_PACKAGE_NAME, JOYPLUS_SERVICE_CLASS_NAME);
//			Intent intent = new Intent();
//			intent.setComponent(componetName);
//			if(context==null){
//				context = DaemonService.this;
//			}
//			context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//		}
//	};
//
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		context = this;
//		LogUtil.v("***** DaemonService *****: onCreate");
//		// StrictMode.setThreadPolicy(new
//		// StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
//		// StrictMode.setVmPolicy(new
//		// StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath().build());
//
//	}
//
//	@Override
//	public void onStart(Intent i, int startId) {
//		LogUtil.v("***** DaemonService *****: onStart");
//		// 绑定到joyplus search service
//		ComponentName componetName = new ComponentName(JOYPLUS_SERVICE_PACKAGE_NAME, JOYPLUS_SERVICE_CLASS_NAME);
//		Intent intent = new Intent();
//		intent.setComponent(componetName);
//		this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//		connect();
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see android.app.Service#onDestroy()
//	 */
//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		try {
//			destroy();
//		} catch (Exception e) {
//			LogUtil.e("***** DaemonService *****: onStart", e);
//		}
//		// 从joyplus search service
//		this.unbindService(serviceConnection);
//	}
//
//	@Override
//	public IBinder onBind(Intent I) {
//		LogUtil.v("***** DaemonService *****: onBind");
//		connect();
//		return mServiceMessenger.getBinder();
//	}
//
//	@Override
//	public boolean onUnbind(Intent intent) {
//		LogUtil.d("***** DaemonService *****: onUnbind");
//		try {
//			if (clientThread != null && clientThread.getOutputStream() != null) {
//				send_20_03(clientThread.getOutputStream());
//			}
//		} catch (Exception e) {
//			LogUtil.e("***** DaemonService *****: onUnbind", e);
//		}
//		return super.onUnbind(intent);
//	}
//
//	public enum LinkDecoderState {
//		ReadA,
//		ReadBCDEFG
//	}
//
//	private ClentThread clientThread;
//
//	private void connect() {
//		try {
//			LogUtil.v("DaemonService: connect");
//
//			if (clientThread != null) {
//				LogUtil.d("DaemonService clientThread: stop");
//				clientThread.stopThread();
//				clientThread = null;
//			}
//
//			clientThread = new ClentThread();
//			LogUtil.d("DaemonService clientThread: create");
//			clientThread.start();
//			LogUtil.d("DaemonService clientThread: start");
//			// thread.join();
//			// socket.close();
//		} catch (Exception e) {
//			LogUtil.e("Connect method exception:", e);
//			reconnect();
//		}
//	}
//
//	private void reconnect() {
//		LogUtil.d("connect close!");
//		try {
//			LogUtil.d("sleep 5000");
//			Thread.sleep(5000);
//			LogUtil.d("re-connect!");
//			Thread startThread = new Thread(new Runnable() {
//				@Override
//				public void run() {
//					connect();
//				}
//			});
//			startThread.start();
//		} catch (InterruptedException e1) {
//		}
//	}
//
//	private class ClentThread extends Thread {
//
//		private boolean isStop = false;
//		private Socket socket;
//		private OutputStream outputStream;
//
//		public ClentThread() {
//		}
//
//		public void stopThread() {
//			isStop = true;
//		}
//
//		public OutputStream getOutputStream() {
//			return outputStream;
//		}
//
//		@Override
//		public void run() {
//			LogUtil.d(String.format("ClentThread [%s] starting.", Thread.currentThread().getName()));
//
//			try {
//
//				socket = new Socket(LINK_SERVER_HOST, LINK_SERVER_PORT);
//				// Socket socket = new Socket("71.126.252.38",
//				// 8enclosing021);
//				outputStream = socket.getOutputStream();
//				send_20_01(outputStream);
//				InputStream is = socket.getInputStream();
//				while (!isStop) {
//					LinkDecoderState state = LinkDecoderState.ReadA;
//					byte[] ABCDEFG = null;
//					try {
//						switch (state) {
//						case ReadA:
//							int readLength = is.read();
//							if (readLength == -1) {
//								throw new IOException("Connection has been closed!");
//							}
//							byte A1 = (byte) readLength;
//
//							if (A1 != KEY_REQUEST_START) {
//								LogUtil.e(String.format("The package is invaild start 0 (%s)",
//										StringUtil.getHexString(A1)));
//								continue;
//							}
//							readLength = is.read();
//							if (readLength == -1) {
//								throw new IOException("Connection has been closed!");
//							}
//							byte A2 = (byte) readLength;
//							if (A2 != KEY_REQUEST_START) {
//								LogUtil.e(String.format("The package is invaild start 1 (%s)",
//										StringUtil.getHexString(A2)));
//								continue;
//							}
//							state = LinkDecoderState.ReadBCDEFG;
//						case ReadBCDEFG:
//							byte[] B = new byte[2];
//							readLength = is.read(B);
//							if (readLength == -1) {
//								throw new IOException("Connection has been closed!");
//							}
//							int l1 = B[0] < 0 ? B[0] + 256 : B[0];
//							int l0 = B[1] < 0 ? B[1] + 256 : B[1];
//							int packageLength = (l0 << 8) | l1;
//							LogUtil.d("The package all length = " + String.valueOf(packageLength));
//
//							ABCDEFG = new byte[packageLength];
//							ABCDEFG[0] = KEY_REQUEST_START;
//							ABCDEFG[1] = KEY_REQUEST_START;
//							ABCDEFG[2] = B[0];
//							ABCDEFG[3] = B[1];
//
//							// read C D E F G
//							byte[] CDEFG = new byte[packageLength - 4];
//							int readIndex = 0;
//							while (readIndex < packageLength - 4) {
//								readLength = is.read(CDEFG);
//								LogUtil.d("The byte length from io = " + readLength);
//								if (readLength == -1) {
//									throw new IOException("Connection has been closed!");
//								}
//								// if(readLength < packageLength - 4){
//								System.arraycopy(CDEFG, 0, ABCDEFG, 4 + readIndex, readLength);
//								// }
//								CDEFG = new byte[packageLength - 4 - readLength];
//								readIndex += readLength;
//							}
//
//							debugLog(ABCDEFG);
//
//							if (ABCDEFG[ABCDEFG.length - 2] == KEY_END_0 && ABCDEFG[ABCDEFG.length - 1] == KEY_END_1) {
//
//								state = LinkDecoderState.ReadA;
//								if (CloudWatchParser.checkDataPack(ABCDEFG)) {
//									LogUtil.d("Data ok, do command: "
//											+ StringUtil.getHexString(ABCDEFG, StringUtil.HEX_STRING_BLANK_SPLIT));
//									LinkRequestData linkRequestData = getLinkRequestData(ABCDEFG);
//									byte[] command = linkRequestData.getCommand();
//									if (command[0] == 0x10 && command[0] != 0x01) {
//										doCommand(linkRequestData, command);
//									}
//								} else {
//									LogUtil.e(String.format("The package is invaild crc. (%s)",
//											StringUtil.getHexString(ABCDEFG)));
//								}
//							} else {
//								state = LinkDecoderState.ReadA;
//								LogUtil.e(String.format("The package is invaild ending. (%s)",
//										StringUtil.getHexString(ABCDEFG)));
//							}
//							break;
//						default:
//							state = LinkDecoderState.ReadA;
//							throw new Error("Shouldn't reach here.");
//						}
//
//					} catch (Exception e) {
//						String message = "Cannot get package data.";
//						if (ABCDEFG != null) {
//							message = Arrays.toString(ABCDEFG);
//						}
//						LogUtil.e("PackageData Exception: " + message, e);
//						state = LinkDecoderState.ReadA;
//						reconnect();
//						break;
//					}
//				}
//			} catch (SocketException e) {
//				LogUtil.w("ClientThread Socket Exception: " + e.getMessage());
//				reconnect();
//			} catch (SocketTimeoutException e) {
//				LogUtil.w("ClientThread Socket Timeout Exception: " + e.getMessage());
//				reconnect();
//			} catch (IOException e) {
//				LogUtil.e("ClientThread IOException:", e);
//				reconnect();
//			} catch (Exception e) {
//				LogUtil.e("ClientThread exception:", e);
//				reconnect();
//			} finally {
//				if (outputStream != null) {
//					try {
//						outputStream.close();
//					} catch (IOException e) {
//					}
//					outputStream = null;
//				}
//				if (socket != null) {
//					try {
//						socket.close();
//					} catch (IOException e) {
//					}
//					socket = null;
//				}
//			}
//
//			LogUtil.d(String.format("ClentThread [%s] end.", Thread.currentThread().getName()));
//		}
//
//	}
//
//	private void debugLog(byte[] ABCDEFG) {
//		byte[] deviceNo = ByteUtil.getCopyByteArray(ABCDEFG, 4, 20);
//		byte[] command = ByteUtil.getCopyByteArray(ABCDEFG, 24, 2);
//		byte[] crc = ByteUtil.getCopyByteArray(ABCDEFG, ABCDEFG.length - 4, 2);
//		StringBuffer sb = new StringBuffer();
//		sb.append("Handle Up Command: ");
//		sb.append(String.format("command = [%s], ", StringUtil.getHexString(command)));
//		sb.append(String.format("strDeviceNo = [%s], deviceNo = [%s], crc = [%s], package = [%s]",
//				new String(deviceNo), StringUtil.getHexString(deviceNo), StringUtil.getHexString(crc),
//				StringUtil.getHexString(ABCDEFG)));
//
//		LogUtil.d(sb.toString());
//	}
//
//	protected LinkRequestData getLinkRequestData(byte[] packageData) {
//
//		byte[] deviceNo = new byte[20];
//		System.arraycopy(packageData, 4, deviceNo, 0, 20);
//		byte[] command = new byte[2];
//		command[0] = packageData[24];
//		command[1] = packageData[25];
//		byte[] data = new byte[packageData.length - 30];
//		if (packageData.length - 30 > 0) {
//			System.arraycopy(packageData, 26, data, 0, packageData.length - 30);
//		}
//		return new LinkRequestData(deviceNo, command, data);
//	}
//
//	// login
//	private void send_20_01(OutputStream os) throws Exception {
//		LinkRequestData request = new LinkRequestData(deviceNo, new byte[] { (byte) 0x20, 0x01 }, new byte[] {});
//		byte[] data = CloudWatchParser.dataPack(request, false);
//		os.write(data);
//	}
//
//	// logout
//	private void send_20_03(OutputStream os) throws Exception {
//		LinkRequestData request = new LinkRequestData(deviceNo, new byte[] { (byte) 0x20, 0x03 }, new byte[] {});
//		byte[] data = CloudWatchParser.dataPack(request, false);
//		os.write(data);
//	}
//
//	private void doCommand(LinkRequestData linkRequestData, byte[] command) {
//		LogUtil.d(linkRequestData.toString());
//		if (linkRequestData.getData() != null) {
//			String jsonString = new String(StringUtil.getByteArrayByHexString(
//					StringUtil.getHexString(linkRequestData.getData(), StringUtil.HEX_STRING_NOT_SPLIT),
//					StringUtil.HEX_STRING_NOT_SPLIT), Charset.forName("UTF-8"));
//			LogUtil.d(jsonString);
//			if (jsonString != null && jsonString.trim().length() > 0) {
//				try {
//					sendCMD(jsonString, command);
//				} catch (RemoteException e) {
//					Log.e(this.getClass().getName(), "Send request error!", e);
//				}
//			}
//
//		}
//	}
//
//	private void destroy() throws Exception {
//	}
//
//	/**
//	 * @uml.property name="mIncomingHandler"
//	 * @uml.associationEnd multiplicity="(1 1)"
//	 */
//	private Handler mIncomingHandler = new Handler() {
//		@Override
//		public void handleMessage(Message msg) {
//			Bundle params = msg.getData();
//			// HashMap<String, Object> paramsMap = (HashMap<String, Object>)
//			// params.get("COMAMND_PARAMS_MAP");
//			try {
//				sendCMD("", new byte[] {});
//			} catch (Exception e) {
//				Log.e(this.getClass().getName(), "Send request error!", e);
//			}
//		}
//
//	};
//
//	private void sendCMD(String jsonString, byte[] command) throws RemoteException {
//		if (joyplusServiceMessenger == null) {
//			Log.d(this.getClass().getName(), "joyplus service messenger is null");
//			return;
//		}
//		Message message = new Message();
//		Bundle results = new Bundle();
//		results.putString("COMAMND_PARAMS_JSON", jsonString);
//		results.putByteArray("COMAMND_PARAMS_COMMAND_TYPE", command);
//		// results.putString("COMAMND_PARAMS_DEVICE_ID",
//		// UUID.randomUUID().toString());
//		message.setData(results);
//		joyplusServiceMessenger.send(message);
//	}
//
//	/**
//	 * @uml.property name="mServiceMessenger"
//	 * @uml.associationEnd multiplicity="(1 1)"
//	 */
//	private final Messenger mServiceMessenger = new Messenger(mIncomingHandler);
//
//}
