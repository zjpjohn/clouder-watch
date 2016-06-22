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
package com.cms.android.activity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.TextView;

import com.cms.android.R;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataApi.DataItemResult;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataMap;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.NodeApi.GetConnectedNodesResult;
import com.cms.android.wearable.PutDataMapRequest;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.Wearable;

/**
 * ClassName: MainActivity
 * 
 * @description MainActivity
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements OnConnectionFailedListener, ConnectionCallbacks,
		MessageApi.MessageListener, DataApi.DataListener, NodeApi.NodeListener, OnClickListener {

	private static final String TAG = "MainActivity";

	private static final String IMAGE_PATH = "/image";

	private EditText mMsgEdit;

	private Button mMsgBtn, mFdBtn;

	private TextView mContentText;

	private Gallery mGallery;

	private ImageAdapter mImageAdapter;

	private List<Bitmap> mBitmapList = new ArrayList<Bitmap>();

	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private MobvoiApiClient mobvoiApiClient;

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1112:
				Bitmap bitmap = (Bitmap) msg.obj;
				Log.d(TAG, "展示图片");
				mBitmapList.add(bitmap);
				mImageAdapter.notifyDataSetChanged();
				break;
			case 1113:
				MessageEvent messageEvent = (MessageEvent) msg.obj;
				Log.d(TAG, "展示消息");
				String content = mContentText.getText().toString();
				if (TextUtils.isEmpty(content)) {
					mContentText.setText(new String(messageEvent.getData()));
				} else {
					mContentText.setText(content + " " + new String(messageEvent.getData()));
				}
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
		initViews();

		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
	}

	private void initViews() {
		mMsgEdit = (EditText) findViewById(R.id.et_msg);
		mMsgBtn = (Button) findViewById(R.id.btn_msg);
		mContentText = (TextView) findViewById(R.id.tv_content);

		mGallery = (Gallery) findViewById(R.id.gallery);
		mImageAdapter = new ImageAdapter(this, mBitmapList);
		mGallery.setAdapter(mImageAdapter);

		mMsgBtn.setOnClickListener(this);

		mFdBtn = (Button) findViewById(R.id.btn_getfdasset);
		mFdBtn.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mobvoiApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Wearable.MessageApi.removeListener(mobvoiApiClient,
		// this).setResultCallback(new ResultCallback<Status>() {
		// @Override
		// public void onResult(Status result) {
		// Log.d(TAG, "MessageApi removeListener: " + result.isSuccess());
		// }
		// });
		// Wearable.DataApi.removeListener(mobvoiApiClient,
		// this).setResultCallback(new ResultCallback<Status>() {
		// @Override
		// public void onResult(Status result) {
		// Log.d(TAG, "DataApi removeListener: " + result.isSuccess());
		// }
		// });
		mobvoiApiClient.disconnect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
		// Wearable.MessageApi.removeListener(mobvoiApiClient, this);
		// Wearable.DataApi.removeListener(mobvoiApiClient, this);
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.e(TAG, "手机端：CMS服务已连接");
		Wearable.MessageApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
			@Override
			public void onResult(Status result) {
				Log.d(TAG, "MessageApi : " + result.isSuccess());
			}
		});
		Wearable.DataApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
			@Override
			public void onResult(Status result) {
				Log.d(TAG, "DataApi : " + result.isSuccess());
			}
		});
		Wearable.NodeApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
			@Override
			public void onResult(Status result) {
				Log.d(TAG, "NodeApi : " + result.isSuccess());
			}
		});
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e(TAG, "手机端：CMS服务挂起 cause:" + cause);
		// mobvoiApiClient.connect();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.e(TAG, "手机端：CMS接收到消息,消息为" + messageEvent.toString());
		handler.sendMessage(handler.obtainMessage(1113, messageEvent));
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEventBuffer) {
		Log.d(TAG, "onDataChanged...");
		for (DataEvent event : dataEventBuffer) {
			Log.d(TAG, "" + event.getType() + " URI = " + event.getDataItem().getUri() + " path = "
					+ event.getDataItem().getUri().getPath());
			if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/image")) {
				Log.d(TAG, "[onDataChanged] handle uri(/image)");
				DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
				Asset key1Asset = dataMapItem.getDataMap().getAsset("key1");
				Bitmap bitmap = loadBitmapFromAsset(key1Asset);
				Log.d(TAG, "bitmap -> " + bitmap);
				Log.d(TAG, "执行回调方法 onDataChanged");
				if (bitmap != null) {
					handler.sendMessage(handler.obtainMessage(1112, bitmap));
				}

				Asset key2Asset = dataMapItem.getDataMap().getAsset("key2");
				Bitmap bitmap2 = loadBitmapFromAsset(key2Asset);
				Log.d(TAG, "bitmap -> " + bitmap2);
				Log.d(TAG, "执行回调方法 onDataChanged");
				if (bitmap2 != null) {
					handler.sendMessage(handler.obtainMessage(1112, bitmap2));
				}
			}
		}
	}

	public Bitmap loadBitmapFromAsset(Asset asset) {
		if (asset == null) {
			return null;
			// throw new IllegalArgumentException("Asset must be non-null");
		}
		Log.d(TAG, "[loadBitmapFromAsset] " + asset.getDigest());
		// ConnectionResult result = mobvoiApiClient.blockingConnect(TIMEOUT_MS,
		// TimeUnit.MILLISECONDS);
		// mobvoiApiClient
		// if (!result.isSuccess()) {
		// return null;
		// }
		// convert asset into a file descriptor and block until it's ready
		Thread thread = Thread.currentThread();
		Log.d(TAG, "thread id = " + thread.getId() + " name = " + thread.getName());
		InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mobvoiApiClient, asset).await().getInputStream();
		// mobvoiApiClient.disconnect();

		if (assetInputStream == null) {
			Log.w(TAG, "Requested an unknown Asset.");
			return null;
		}
		// decode the stream into a bitmap
		return BitmapFactory.decodeStream(assetInputStream);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_msg:
			sendMessage();
			break;
		case R.id.btn_getfdasset:
			Log.d(TAG, "onClick putDataItem");
			putDataItem();
			break;

		default:
			break;
		}
	}

	private void sendMessage() {

		Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).setResultCallback(
				new ResultCallback<GetConnectedNodesResult>() {
					@SuppressLint("SimpleDateFormat")
					@Override
					public void onResult(GetConnectedNodesResult result) {

						List<Node> nodes = result.getNodes();
						if (null != nodes && !nodes.isEmpty()) {
							for (Node node : nodes) {
								Log.d(TAG, "nodeId : + " + node.getId() + " | displayName : " + node.getDisplayName());
								String content = mMsgEdit.getText().toString();
								Log.d(TAG, "手机端：发送消息时间:" + sdf.format(new Date()) + " ,内容:"
										+ mMsgEdit.getText().toString());

								PendingResult<SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(
										mobvoiApiClient, node.getId(), "/test", content.getBytes());
								final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								Log.e(TAG, "sendMessage before time = " + sdf.format(new Date()));
								// pendingResult.setResultCallback(new
								// ResultCallback<SendMessageResult>() {
								// @Override
								// public void onResult(SendMessageResult
								// sendMessageResult) {
								// Log.e(TAG, "sendMessage after time = " +
								// sdf.format(new Date()));
								// Log.e(TAG,
								// "手机端：消息结果返回, sendMessageResult status: "
								// + sendMessageResult.getStatus().toString());
								// }
								// });
								SendMessageResult sendMessageResult = pendingResult.await(5000, TimeUnit.MILLISECONDS);
								if (!sendMessageResult.getStatus().isSuccess()) {
									Log.d(TAG, "发送失败");
								} else {
									Log.d(TAG, "发送成功");
								}
								Log.e(TAG, "sendMessage handle time = " + sdf.format(new Date()));
							}
						}
					}
				});
	}

	private void putDataItem() {
		PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(IMAGE_PATH);
		DataMap dataMap = putDataMapRequest.getDataMap();
		Drawable beauty = getResources().getDrawable(R.drawable.img_skin_2);
		BitmapDrawable bd = (BitmapDrawable) beauty;
		Bitmap bitmap = bd.getBitmap();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] beautyContent = baos.toByteArray();
		Log.d(TAG, "asset1 content length：" + beautyContent.length);

		// String path =
		// Environment.getExternalStorageDirectory().getAbsolutePath();
		// File rootFile = new File(path);
		// if (rootFile.isDirectory()) {
		// String[] array = rootFile.list();
		// for (int i = 0; i < array.length; i++) {
		// Log.d(TAG, "array[" + i + "]" + array[i]);
		// }
		// }
		// Log.d(TAG, "path->" + path);
		// File file = new File(path + "//" + "ara.zip");
		//
		// Log.d(TAG, "file length = " + file.length());
		//
		// FileInputStream fis;
		// byte[] ara = null;
		// try {
		// fis = new FileInputStream(file);
		// ara = new byte[(int) file.length()];
		// fis.read(ara);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		Asset asset1 = Asset.createFromBytes(beautyContent);
		dataMap.putAsset("key1", asset1);

		// Asset asset2 = Asset.createFromBytes(ara);
		// dataMap.putAsset("key2", asset2);

		// int n = 10000, d = 10;
		// byte[] array = new byte[n];
		// for (int i = 0; i < n; i++) {
		// array[i] = (byte) (i % d);
		// }
		// Log.d(TAG, "array size = " + array);
		// Asset asset2 = Asset.createFromBytes(array);
		// dataMap.putAsset("key2", asset2);
		//
		// Asset asset3 = Asset.createFromBytes(new byte[] { 10, 8, 8, 6, 5, 4,
		// 3, 2, 1, 7, 7 });
		// dataMap.putAsset("key3", asset3);

		PutDataRequest request = putDataMapRequest.asPutDataRequest();

		Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(
				new ResultCallback<DataApi.DataItemResult>() {

					@Override
					public void onResult(DataItemResult result) {
						Log.e(TAG, "putDataItem onResult->" + result.getStatus().isSuccess());
					}
				});
	}

	@Override
	public void onPeerConnected(Node node) {
		Log.d(TAG, "onPeerConnected : " + node.getId());
	}

	@Override
	public void onPeerDisconnected(Node node) {
		Log.d(TAG, "onPeerDisconnected : " + node.getId());
	}
}
