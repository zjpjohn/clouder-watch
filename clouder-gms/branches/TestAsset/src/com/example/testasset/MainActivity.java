package com.example.testasset;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

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

/**
 * ClassName: MainActivity
 * 
 * @description MainActivity
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private static final String COUNT_PATH = "/count";
	private static final String IMAGE_PATH = "/image";
	private static final String IMAGE_KEY = "photo";
	private static final String COUNT_KEY = "count";

	private EditText mMsgEdit;

	private Button mMsgBtn, mFdBtn;

	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();

	}

	private void initViews() {
		mMsgEdit = (EditText) findViewById(R.id.et_msg);
		mMsgBtn = (Button) findViewById(R.id.btn_msg);

		mFdBtn = (Button) findViewById(R.id.btn_getfdasset);
		mFdBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "onClick putDataItem");
				PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
				DataMap dataMap = putDataMapRequest.getDataMap();

				Asset asset1 = Asset.createFromBytes(new byte[] { 0, 9, 8 });
				dataMap.putAsset("key1", asset1);

				// Asset asset2 = Asset.createFromRef("tesrtsgdsj1");
				Asset asset2 = new Asset(1, new byte[] {}, "tesrtsgdsj1", null, null);
				dataMap.putAsset("key2", asset2);

				// Wearable.DataApi.putDataItem(mobvoiApiClient, request);
				//
				// Bundle bundle = new Bundle();
				// bundle.putInt("age", 1);
				// DataItemParcelable parcelable = new
				// DataItemParcelable(Uri.parse("/path"), bundle, new byte[] {
				// 1, 2 });
				// int versionCode, byte[] data, String digest,
				// ParcelFileDescriptor fd, Uri uri

				File file = Environment.getExternalStorageDirectory();
				Log.d(TAG, "file path  = " + file.getAbsolutePath());
				File txtFile = new File(file.getAbsolutePath() + "/test.txt");
				BufferedOutputStream bos = null;
				try {
					if (!txtFile.exists()) {
						txtFile.createNewFile();
					}
					bos = new BufferedOutputStream(new FileOutputStream(txtFile));
					bos.write("测试BOS".getBytes());
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
				Asset testAsset = new Asset(1, new byte[] { 1, 2, 3, 4, 5 }, "1252662", pfd, Uri.parse("/test"));
				dataMap.putAsset("key3", testAsset);
				Asset testAsset1 = new Asset(3, new byte[] { 1, 2, 3, 4, 5, 6 }, "3252662", pfd, Uri.parse("/eeee"));
				dataMap.putAsset("key4", testAsset1);
				Asset testAsset2 = new Asset(2, new byte[] {}, "asdsdsds", null, Uri.parse("/gjgklgjkkd"));
				dataMap.putAsset("key5", testAsset2);
				PutDataRequest request = putDataMapRequest.asPutDataRequest();
				Log.d("spencer", request.toString());

				DataInfo data = new DataInfo();
				data.setAssets(request.getBundle());
				data.setUri(request.getUri());
				data.setDeviceId("device12345678901234");
				data.setTimeStamp(new Date().getTime());
				Log.d("parsertest", "TimeStamp o " + new Date().getTime());
				data.setData(new byte[] { 5, 2, 0, 1, 3, 1, 4 });
				data.setProtocolType(1);
				data.setVersionCode(request.getVersionCode());

				byte[] dataPack = DataInfoParser.dataPack(data);

				DataInfo newData = DataInfoParser.dataUnpack(dataPack);
				Log.d("parsertest", "DeviceId " + newData.getDeviceId());
				Log.d("parsertest", "ProtocolType " + newData.getProtocolType());
				Log.d("parsertest", "TimeStamp " + newData.getTimeStamp());
				Log.d("parsertest", "Uri " + newData.getUri() == null ? "null" : newData.getUri() .getPath());
				Log.d("parsertest", "Data " + Arrays.toString(newData.getData()));
				Map<String, Asset> newAssetMap = newData.getAssetsMap();
				for (String key : newAssetMap.keySet()) {
					Asset asset = newAssetMap.get(key);
					if (asset != null) {
						Log.d("parsertest", key + " " + asset.getVersionCode() + " " + Arrays.toString(asset.getData()) 
								+ " " + asset.getDigest() + " " + (asset.getUri() == null ? "null" : asset.getUri().getPath()));
					}
				}
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// mobvoiApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Wearable.MessageApi.removeListener(mobvoiApiClient, this);
		// mobvoiApiClient.disconnect();
	}
}
