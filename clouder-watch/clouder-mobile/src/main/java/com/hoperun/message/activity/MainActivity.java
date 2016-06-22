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
package com.hoperun.message.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.TextView;

import com.clouder.watch.mobile.R;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataMap;
import com.cms.android.wearable.PutDataMapRequest;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.internal.DataEventParcelable;
import com.cms.android.wearable.internal.DataHolder;
import com.cms.android.wearable.internal.DataItemParcelable;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetFdForAssetResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;
import com.cms.android.wearable.internal.MessageEventHolder;
import com.cms.android.wearable.internal.NodeHolder;
import com.cms.android.wearable.internal.PutDataResponse;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.service.codec.CRCUtil;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;
import com.cms.android.wearable.service.impl.IBLECentralCallback;
import com.cms.android.wearable.service.impl.IBLECentralService;
import com.cms.android.wearable.service.impl.IWearableListener;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ClassName: MainActivity
 *
 * @description MainActivity
 * @author xing_pengfei
 * @Date 2015-7-29
 *
 */
@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "MainActivity";

	private Button mStartBtn, mSwitchBtn, mDisconnectBtn, mSendMessageBtn, mSyncTimeBtn, mImageBtn, mAddListenerBtn,
			mDataBtn, mQueryBtn, mDeleteFileBtn;

	private EditText mMsgEditText;

	private TextView mSyncTimeText;

	private Gallery mGallery;

	private ImageAdapter mImageAdapter;

	private List<Bitmap> mBitmapList = new ArrayList<Bitmap>();

	private int[] personImageArray = new int[] { R.drawable.person1, R.drawable.person2, R.drawable.person3,
			R.drawable.person4, R.drawable.person5, R.drawable.person6 };

	private int currentIndex = 0;

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private String mDeviceName, mDeviceAddress;

	private String mNextDeviceAddress;

	private IBLECentralService mBLECentralService;

	private ServiceConnection connection;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case 1112:
					byte[] bytes = (byte[]) msg.obj;
					Log.d(TAG, "data length = " + bytes.length);
					mBitmapList.add(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
					mImageAdapter.notifyDataSetChanged();
					break;
				case 1113:
					DataHolder holder = (DataHolder) msg.obj;
					List<DataItemParcelable> dataItemList = holder.getDataItems();
					List<DataEventParcelable> dataEventList = holder.getDataEvents();
					for (DataItemParcelable dataItem : dataItemList) {
						Log.d(TAG, "dataItem->" + dataItem.toString());
					}

					for (DataEventParcelable dataEvent : dataEventList) {
						Log.d(TAG, "dataEvent->" + dataEvent.toString());
					}
					Log.d(TAG, "holder = " + holder.toString());
					break;

				default:
					break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		// 将地址设为另一部手表地址
		mNextDeviceAddress = "90:E7:C4:1D:8B:36";
		Log.d(TAG, "DeviceName = " + mDeviceName + " DeviceAddress = " + mDeviceAddress + "  mNextDeviceAddress = "
				+ mNextDeviceAddress);

		init();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connection != null) {
			unbindService(connection);
		}
	}

	private void init() {
		mMsgEditText = (EditText) findViewById(R.id.et_msg);

		mStartBtn = (Button) findViewById(R.id.btn_start_service);
		mSwitchBtn = (Button) findViewById(R.id.btn_switch_service);
		mDisconnectBtn = (Button) findViewById(R.id.btn_disconnect);
		mSendMessageBtn = (Button) findViewById(R.id.btn_send_message);
		mSyncTimeBtn = (Button) findViewById(R.id.btn_sync_time);
		mAddListenerBtn = (Button) findViewById(R.id.btn_add_listener);
		mImageBtn = (Button) findViewById(R.id.btn_send_beauty);
		mDataBtn = (Button) findViewById(R.id.btn_put_data);
		mQueryBtn = (Button) findViewById(R.id.btn_query);
		mDeleteFileBtn = (Button) findViewById(R.id.btn_delete);
		mSyncTimeText = (TextView) findViewById(R.id.tv_sync_time);
		mStartBtn.setOnClickListener(this);
		mSwitchBtn.setOnClickListener(this);
		mDisconnectBtn.setOnClickListener(this);
		mSendMessageBtn.setOnClickListener(this);
		mSyncTimeBtn.setOnClickListener(this);
		mImageBtn.setOnClickListener(this);
		mAddListenerBtn.setOnClickListener(this);
		mDataBtn.setOnClickListener(this);
		mQueryBtn.setOnClickListener(this);
		mDeleteFileBtn.setOnClickListener(this);

		mGallery = (Gallery) findViewById(R.id.gallery);
		mImageAdapter = new ImageAdapter(this, mBitmapList);
		mGallery.setAdapter(mImageAdapter);
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_start_service:
				startService();
				break;
			case R.id.btn_switch_service:
				switchService();
				break;
			case R.id.btn_disconnect:
				disconnect();
				break;
			case R.id.btn_send_message:
				sendMessage();
				break;
			case R.id.btn_sync_time:
				syncTime();
				break;
			case R.id.btn_send_beauty:
				sendBeauty();
				break;
			case R.id.btn_put_data:
				putDataItem();
				break;
			case R.id.btn_query:
				queryCacheFile();
				break;
			case R.id.btn_delete:
				deletCacheFile();
				break;
			case R.id.btn_add_listener:
				addListener();
				break;

			default:
				break;
		}
	}

	@SuppressLint("SimpleDateFormat")
	private void queryCacheFile() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "//cloudwatchcache";
				File directory = new File(basePath);
				Log.d("spencer", "directory->" + directory);
				if (directory.isDirectory()) {
					File[] files = directory.listFiles();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					for (File file : files) {
						Log.d("spencer",
								"手机端：name = " + file.getName() + " length = " + file.length() + " time = "
										+ sdf.format(new Date(file.lastModified()))
										+ CRCUtil.makeCrcToBytes(file.getAbsolutePath()));
					}
				} else {
					Log.d("spencer", "not a directory");
				}
			}
		}).start();

	}

	private void deletCacheFile() {
		File deleteDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "//cloudwatchcache");
		Log.d("spencer", "deleteDirectory->" + deleteDirectory);
		if (deleteDirectory.isDirectory()) {
			File[] files = deleteDirectory.listFiles();
			for (File file : files) {
				String name = file.getName();
				long length = file.length();
				boolean isSuccess = file.delete();
				Log.d("spencer", "name = " + name + " length = " + length + " isSuccess = " + isSuccess);
			}
		} else {
			Log.d("spencer", "not a directory");
		}
	}

	private void startService() {
		Log.d(TAG, "connect to BLE.");
		Intent serviceIntent = new Intent();
		serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
		// serviceIntent.putExtra("bluetooth_bond_name",
		// "Onetouch Idol 3 (4.7)");
		// serviceIntent.putExtra("bluetooth_bond_name", "Onetouch Idol 3 100");
		Log.e(TAG, "startService DeviceName = " + mDeviceName);
		// serviceIntent.putExtra("bluetooth_bond_name", mDeviceName);
		serviceIntent.putExtra("bluetooth_bond_address", "3C:CB:7C:8B:2F:69");
		Intent newIntent = Utils.createExplicitFromImplicitIntent(this, serviceIntent);
		if (newIntent == null) {
			return;
		}
		startService(newIntent);
		connection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {

			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(TAG, "[onServiceConnected] name:" + name);
				mBLECentralService = IBLECentralService.Stub.asInterface(service);
				// try {
				// mBLECentralService.connect(mDeviceAddress);
				// } catch (RemoteException e) {
				// e.printStackTrace();
				// }
			}
		};
		bindService(Utils.createExplicitFromImplicitIntent(this, serviceIntent), connection, Context.BIND_AUTO_CREATE);
	}

	private void switchService() {
		Log.d(TAG, "switch to BLE.");
		Intent serviceIntent = new Intent();
		serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
		serviceIntent.putExtra("bluetooth_bond_address", mNextDeviceAddress);
		String tmp = mDeviceAddress;
		mDeviceAddress = mNextDeviceAddress;
		mNextDeviceAddress = tmp;
		Intent newIntent = Utils.createExplicitFromImplicitIntent(this, serviceIntent);
		if (newIntent == null) {
			return;
		}
		startService(newIntent);
		connection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {

			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.d(TAG, "[onServiceConnected] name:" + name);
				mBLECentralService = IBLECentralService.Stub.asInterface(service);
			}
		};
		bindService(Utils.createExplicitFromImplicitIntent(this, serviceIntent), connection, Context.BIND_AUTO_CREATE);
	}

	private void disconnect() {
		Log.d(TAG, "disconnect BLE.");
		Intent serviceIntent = new Intent();
		serviceIntent.setAction(BLECentralService.BLE_CENTRAL_SERVICE_ACTION);
		Log.e(TAG, "disconnect DeviceName = " + mDeviceName);
		serviceIntent.putExtra("bluetooth_bond_toggle", false);
		Intent newIntent = Utils.createExplicitFromImplicitIntent(this, serviceIntent);
		if (newIntent == null) {
			return;
		}
		startService(newIntent);
	}

	private void sendMessage() {
		Log.d(TAG, "onClick send message.");
		if (mBLECentralService != null) {
			try {
				mBLECentralService.sendMessage(new IBLECentralCallback() {

					@Override
					public IBinder asBinder() {
						return null;
					}

					@Override
					public void setStatusRsp(Status status) throws RemoteException {
						Log.d(TAG, ">>>><<<<setStatusRsp " + status.toString());
					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
						Log.d(TAG, ">>>><<<<setSendMessageRsp");

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {

					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						Log.d(TAG, ">>>><<<<setPutDataRsp " + response.toString());
					}

					@Override
					public String getPackageName() throws RemoteException {
						return null;
					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub

					}

				}, getPackageName(), getPackageName(), mMsgEditText.getText().toString().getBytes());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	private void syncTime() {
		Log.d(TAG, "onClick syncTime.");
		if (mBLECentralService != null) {
			try {
				Date date = new Date();
				long time = date.getTime();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
				mSyncTimeText.setText("当前同步时间为" + sdf.format(date));
				mBLECentralService.syncTime(time, 0x1);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "mBLECentralService is null");
		}
	}

	private void putDataItem() {
		PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/count");
		DataMap dataMap = putDataMapRequest.getDataMap();
		dataMap.putInt("COUNT_KEY", 1);
		dataMap.putString("TestString", "TestStringValue");

		Asset asset1 = Asset.createFromBytes(new byte[] { 0, 9, 8 });
		dataMap.putAsset("key1", asset1);

		Asset asset2 = Asset.createFromBytes(new byte[] { 0, 9, 8, 1, 1, 1, 1, 4, 3, 2, 3, 9, 0, 1 });
		dataMap.putAsset("key2", asset2);

		Asset asset3 = Asset.createFromBytes(new byte[] { 0, 9, 8, 8, 9, 0, 0, 1 });
		dataMap.putAsset("key3", asset3);

		// Asset asset2 = new Asset(1, new byte[] {}, "tesrtsgdsj1", null,
		// null);
		// dataMap.putAsset("key2", asset2);
		//
		File file = Environment.getExternalStorageDirectory();
		Log.d(TAG, "file path  = " + file.getAbsolutePath());
		File txtFile = new File(file.getAbsolutePath() + "/test.txt");
		BufferedOutputStream bos = null;
		try {
			if (!txtFile.exists()) {
				txtFile.createNewFile();
			}
			bos = new BufferedOutputStream(new FileOutputStream(txtFile));
			bos.write("abcdefg".getBytes());
			bos.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		ParcelFileDescriptor pfd = null;
		try {
			pfd = ParcelFileDescriptor.open(txtFile, ParcelFileDescriptor.MODE_READ_WRITE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "statSize = " + pfd.getStatSize());
		// Asset testAsset = new Asset(1, new byte[] { 1, 2, 3, 4, 5 },
		// "1252662", pfd, Uri.parse("/testAsset"));
		// dataMap.putAsset("key3", testAsset);
		// Asset testAsset1 = new Asset(1, new byte[] { 1, 2, 3, 4, 5 },
		// "1252662", pfd, Uri.parse("/testAsset1"));
		// dataMap.putAsset("key4", testAsset1);
		// Asset testAsset2 = new Asset(2, new byte[] {}, "asdsdsds", null,
		// Uri.parse("/testAsset2"));
		// dataMap.putAsset("key5", testAsset2);

		PutDataRequest request = putDataMapRequest.asPutDataRequest();
		if (mBLECentralService != null) {
			try {
				mBLECentralService.putDataItem(new IBLECentralCallback() {

					@Override
					public IBinder asBinder() {
						return null;
					}

					@Override
					public void setStatusRsp(Status status) throws RemoteException {

					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {

					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {

					}

					@Override
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub

					}
				}, request);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendBeauty() {
		Drawable beauty = getResources().getDrawable(personImageArray[currentIndex++ % personImageArray.length]);
		BitmapDrawable bd = (BitmapDrawable) beauty;
		Bitmap bitmap = bd.getBitmap();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] content = baos.toByteArray();
		Log.d(TAG, "传输数据长度为：" + content.length);
		if (mBLECentralService != null) {
			try {
				mBLECentralService.sendMessage(new IBLECentralCallback() {

					@Override
					public IBinder asBinder() {
						return null;
					}

					@Override
					public void setStatusRsp(Status status) throws RemoteException {
						Log.d(TAG, ">>>><<<<setStatusRsp " + status.toString());
					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
						Log.d(TAG, ">>>><<<<setSendMessageRsp");

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {

					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						Log.d(TAG, ">>>><<<<setPutDataRsp " + response.toString());
					}

					@Override
					public String getPackageName() throws RemoteException {
						return null;
					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub

					}

				}, getPackageName(), "/beauty", content);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void addListener() {
		Log.d(TAG, "添加接口");
		if (mBLECentralService != null) {
			try {
				mBLECentralService.addListener(2, new IBLECentralCallback() {

					@Override
					public IBinder asBinder() {
						return null;
					}

					@Override
					public void setStatusRsp(Status status) throws RemoteException {

					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {

					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
					}

					@Override
					public String getPackageName() throws RemoteException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub

					}
				}, new IWearableListener() {

					@Override
					public IBinder asBinder() {
						return null;
					}

					@Override
					public void onMessageReceived(MessageEventHolder messageEventHolder) throws RemoteException {
						Log.d(TAG, "执行回调方法 onMessageReceived");
						if (messageEventHolder != null) {
							handler.sendMessage(handler.obtainMessage(1112, messageEventHolder.getData()));
						}
					}

					@Override
					public void onDataChanged(DataHolder dataHolder) throws RemoteException {
						Log.d(TAG, "执行回调方法 onDataChanged");
						if (dataHolder != null) {
							handler.sendMessage(handler.obtainMessage(1113, dataHolder));
						}
					}

					@Override
					public String id() throws RemoteException {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public void onPeerConnected(NodeHolder nodeHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void onPeerDisconnected(NodeHolder nodeHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}
				});
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

}
