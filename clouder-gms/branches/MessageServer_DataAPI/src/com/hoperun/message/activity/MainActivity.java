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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;

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
import com.cms.android.wearable.service.impl.BLEPeripheralService;
import com.cms.android.wearable.service.impl.IBLEPeripheralCallback;
import com.cms.android.wearable.service.impl.IBLEPeripheralService;
import com.cms.android.wearable.service.impl.IWearableListener;
import com.hoperun.message.R;

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

	private EditText mMsgEdit;

	private Button mStartBtn, mStopBtn, mMsgBtn, mAddListenerBtn, mRemoveListenerBtn, mImageBtn, mPutDataBtn,
			mQueryBtn, mDeleteFileBtn;

	private Gallery mGallery;

	private ImageAdapter mImageAdapter;

	private int[] personImageArray = new int[] { R.drawable.beauty1, R.drawable.beauty2, R.drawable.beauty3,
			R.drawable.beauty4, R.drawable.beauty5, R.drawable.beauty6, R.drawable.beauty7, R.drawable.beauty8 };

	private int currentIndex = 0;

	private List<Bitmap> mBitmapList = new ArrayList<Bitmap>();

	private IBLEPeripheralService mBLEPeripheralService;

	private IWearableListener wearableListener;

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

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "[onServiceConnected] name:" + name);
			mBLEPeripheralService = IBLEPeripheralService.Stub.asInterface(service);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
	}

	private void init() {
		mMsgEdit = (EditText) findViewById(R.id.et_msg);

		mStartBtn = (Button) findViewById(R.id.btn_start);
		mStopBtn = (Button) findViewById(R.id.btn_stop);
		mMsgBtn = (Button) findViewById(R.id.btn_msg);
		mAddListenerBtn = (Button) findViewById(R.id.btn_add_listener);
		mRemoveListenerBtn = (Button) findViewById(R.id.btn_remove_listener);
		mImageBtn = (Button) findViewById(R.id.btn_image);
		mPutDataBtn = (Button) findViewById(R.id.btn_put_data);
		mQueryBtn = (Button) findViewById(R.id.btn_query);
		mDeleteFileBtn = (Button) findViewById(R.id.btn_delete);

		mStartBtn.setOnClickListener(this);
		mStopBtn.setOnClickListener(this);
		mMsgBtn.setOnClickListener(this);
		mAddListenerBtn.setOnClickListener(this);
		mImageBtn.setOnClickListener(this);
		mPutDataBtn.setOnClickListener(this);
		mRemoveListenerBtn.setOnClickListener(this);
		mQueryBtn.setOnClickListener(this);
		mDeleteFileBtn.setOnClickListener(this);

		mGallery = (Gallery) findViewById(R.id.gallery);
		mImageAdapter = new ImageAdapter(this, mBitmapList);
		mGallery.setAdapter(mImageAdapter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			startService();
			break;
		case R.id.btn_stop:
			stopService();
			break;
		case R.id.btn_add_listener:
			addListener();
			break;
		case R.id.btn_remove_listener:
			removeListener();
			break;
		case R.id.btn_image:
			sendImage();
			break;
		case R.id.btn_put_data:
			putDataItem();
			break;
		case R.id.btn_msg:
			sendMessage();
			break;
		case R.id.btn_query:
			queryCacheFile();
			break;
		case R.id.btn_delete:
			deleteCacheFile();
			break;

		default:
			break;
		}
	}

	private void startService() {
		Intent intent = new Intent(this, BLEPeripheralService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		startService(intent);
	}

	private void stopService() {
		if (mServiceConnection != null) {
			unbindService(mServiceConnection);
		}
	}

	private void sendImage() {
		Drawable beauty = getResources().getDrawable(personImageArray[currentIndex++ % personImageArray.length]);
		BitmapDrawable bd = (BitmapDrawable) beauty;
		Bitmap bitmap = bd.getBitmap();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] content = baos.toByteArray();
		Log.d(TAG, "传输数据长度为：" + content.length);
		if (mBLEPeripheralService != null) {
			try {
				mBLEPeripheralService.sendMessage(new IBLEPeripheralCallback.Stub() {

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
						// TODO Auto-generated method stub

					}

					@Override
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
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
		if (mBLEPeripheralService != null) {
			try {
				wearableListener = new IWearableListener.Stub() {

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
				};
				mBLEPeripheralService.addListener(2, new IBLEPeripheralCallback.Stub() {

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
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub
						
					}
				}, wearableListener);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void removeListener() {
		Log.d(TAG, "添加接口");
		if (mBLEPeripheralService != null) {
			try {
				mBLEPeripheralService.removeListener(2, new IBLEPeripheralCallback.Stub() {

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
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub
						
					}
				}, wearableListener);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void putDataItem() {
		PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/count");
		DataMap dataMap = putDataMapRequest.getDataMap();
		dataMap.putInt("COUNT_KEY", 1102);
		dataMap.putString("TestString", "BLEPeripheralService");

		Asset asset1 = Asset.createFromBytes(new byte[] { 0, 9, 8, 1, 2, 8, 9, 6, 1, 0, 2, 1 });
		dataMap.putAsset("key6", asset1);

		Asset asset2 = Asset.createFromBytes(new byte[] { 1, 5, 0, 2, 9, 3, 4, 5, 8 });
		dataMap.putAsset("key7", asset2);

		Asset asset3 = Asset.createFromBytes(new byte[] { 0, 1, 2, 7, 9, 1, 9, 5, 9, 8 });
		dataMap.putAsset("key8", asset3);

		// Asset asset2 = new Asset(1, new byte[] {}, "tesrtsgdsj1", null,
		// null);
		// dataMap.putAsset("key7", asset2);
		//
		// File file = Environment.getExternalStorageDirectory();
		// Log.d(TAG, "file path  = " + file.getAbsolutePath());
		// File txtFile = new File(file.getAbsolutePath() + "/test.txt");
		// BufferedOutputStream bos = null;
		// try {
		// if (!txtFile.exists()) {
		// txtFile.createNewFile();
		// }
		// bos = new BufferedOutputStream(new FileOutputStream(txtFile));
		// bos.write("测试BOS".getBytes());
		// bos.flush();
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// if (bos != null) {
		// try {
		// bos.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// ParcelFileDescriptor pfd = null;
		// try {
		// pfd = ParcelFileDescriptor.open(txtFile,
		// ParcelFileDescriptor.MODE_READ_WRITE);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// }
		// Asset testAsset = new Asset(1, new byte[] { 1, 2, 3, 4, 5 },
		// "1252662", pfd, Uri.parse("/testAsset"));
		// dataMap.putAsset("key8", testAsset);
		// Asset testAsset1 = new Asset(1, new byte[] { 1, 2, 3, 4, 5 },
		// "1252662", pfd, Uri.parse("/testAsset1"));
		// dataMap.putAsset("key9", testAsset1);
		// Asset testAsset2 = new Asset(2, new byte[] {}, "asdsdsds", null,
		// Uri.parse("/testAsset2"));
		// dataMap.putAsset("key10", testAsset2);

		PutDataRequest request = putDataMapRequest.asPutDataRequest();
		if (mBLEPeripheralService != null) {
			try {
				mBLEPeripheralService.putDataItem(new IBLEPeripheralCallback.Stub() {

					@Override
					public void setStatusRsp(Status status) throws RemoteException {

					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
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
								"手表端：name = " + file.getName() + " length = " + file.length() + " time = "
										+ sdf.format(new Date(file.lastModified()))
										+ CRCUtil.makeCrcToBytes(file.getAbsolutePath()));
					}
				} else {
					Log.d("spencer", "not a directory");
				}

			}
		}).start();
	}

	private void deleteCacheFile() {
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

	private void sendMessage() {
		if (mBLEPeripheralService != null) {
			try {
				mBLEPeripheralService.sendMessage(new IBLEPeripheralCallback.Stub() {

					@Override
					public void setStatusRsp(Status status) throws RemoteException {

					}

					@Override
					public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
						Log.d(TAG, "server setSendMessageRsp&&&");

					}

					@Override
					public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {

					}

					@Override
					public String getPackageName() throws RemoteException {
						return MainActivity.this.getPackageName();
					}

					@Override
					public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {

					}

					@Override
					public void setPutDataRsp(PutDataResponse response) throws RemoteException {

					}

					@Override
					public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse)
							throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
						// TODO Auto-generated method stub

					}

					@Override
					public void setAssetRsp() throws RemoteException {
						// TODO Auto-generated method stub
						
					}
				}, "com.cms.android", "/servertest", mMsgEdit.getText().toString().getBytes());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
