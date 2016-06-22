/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.hoperun.watch.service;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.NodeApi.GetConnectedNodesResult;
import com.cms.android.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * ClassName: FirmwareReceiveService
 *
 * @description
 * @author xing_peng
 * @Date 2015-9-8
 * 
 */
public class FirmwareReceiveService extends Service implements OnConnectionFailedListener, ConnectionCallbacks,
		MessageApi.MessageListener, DataApi.DataListener, NodeApi.NodeListener {

	private static final String TAG = "FirmwareReceveService";
	
	private MobvoiApiClient mobvoiApiClient;

	private static final String ClOUD_WATCH_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ File.separator + "CloudWatch" + File.separator;;

	public static final String FIRMWARE_SEND = "/firmware/send";
	public static final String FIRMWARE_SEND_PREPARE = "/firmware/send/prepare";
	public static final String FIRMWARE_SEND_READY = "/firmware/send/ready";
	public static final String FIRMWARE_SEND_VERSION = "/firmware/send/version";

	@Override
	public void onCreate() {
		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		mobvoiApiClient.connect();
		Log.d(TAG, "FirmwareReceveService start");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.d(TAG, "mobile：CMS onConnected");
		Wearable.MessageApi.addListener(mobvoiApiClient, this);
		Wearable.NodeApi.addListener(mobvoiApiClient, this);
	}

	@Override
	public void onPeerConnected(Node node) {
		Log.d(TAG, "onPeerConnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
		
		sendMessage(Build.VERSION.RELEASE.getBytes(), FIRMWARE_SEND_VERSION);
	}

	@Override
	public void onPeerDisconnected(Node node) {
		Log.e(TAG, "onPeerDisconnected: nodeId = " + node.getId() + " , displayName = " + node.getDisplayName());
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.e(TAG, "mobile：CMS onConnectionSuspended cause:" + arg0);
		mobvoiApiClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.e(TAG, "mobile：CMS onConnectionFailed" + arg0.toString());
		mobvoiApiClient.connect();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		if (messageEvent.getPath().equals(FIRMWARE_SEND_PREPARE)) {
			Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
			try {
				String fileName = new String(messageEvent.getData());
				File firmwareFile = checkFile(fileName);
				Long length = 0L;
				if (firmwareFile.exists()) {
					length = firmwareFile.length();
				}
				Log.d(TAG, "firmwareFile length : " + length);
				sendMessageAwait(String.valueOf(length).getBytes(), FIRMWARE_SEND_READY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage(final byte[] content, final String path) {

		Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).setResultCallback(
				new ResultCallback<GetConnectedNodesResult>() {
					@Override
					public void onResult(GetConnectedNodesResult result) {

						List<Node> nodes = result.getNodes();
						if (null != nodes && !nodes.isEmpty()) {
							for (Node node : nodes) {
								Log.d(TAG, "nodeId : " + node.getId() + " | displayName : " + node.getDisplayName());

								Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), path, content)
										.setResultCallback(new ResultCallback<SendMessageResult>() {
											@Override
											public void onResult(SendMessageResult sendMessageResult) {
												Log.e(TAG, "手机端：消息结果返回, sendMessageResult status: "
														+ sendMessageResult.getStatus().toString());
											}
										});
							}
						}
					}
				});
	}
	
	private void sendMessageAwait(final byte[] content, final String path) {

		GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
		if (result != null) {
			List<Node> nodes = result.getNodes();
			if (null != nodes && !nodes.isEmpty()) {
				for (Node node : nodes) {
					Log.d(TAG, "nodeId : " + node.getId() + " | displayName : " + node.getDisplayName());

					Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), path, content).await();
				}
			}
		}
	}

	private File checkFile(String fileName) throws Exception {
		File saveFile = new File(ClOUD_WATCH_DIRECTORY + fileName);
		Log.d(TAG,
				"cloud watch directory is " + ClOUD_WATCH_DIRECTORY + " save file path is "
						+ saveFile.getAbsolutePath());
		File parent = saveFile.getParentFile();
		if (!parent.exists()) {
			Log.d(TAG, "create directory " + parent.getAbsolutePath());
			boolean isParent = parent.mkdir();
			Log.d(TAG, "isParent:" + isParent);
		}
		if (!parent.canWrite()) {
			throw new Exception("can't write into the directory " + parent.getAbsolutePath());
		}
		if (saveFile.exists()) {
			if (!saveFile.canWrite()) {
				throw new Exception("can't write into the file " + saveFile.getAbsolutePath());
			}
		}

		return saveFile;
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEventBuffer) {
		for (DataEvent event : dataEventBuffer) {
			if (event.getType() == DataEvent.TYPE_CHANGED
					&& event.getDataItem().getUri().getPath().equals(FIRMWARE_SEND)) {
				FileOutputStream fos = null;
				try {
					Log.d(TAG, "onDataChanged " + FIRMWARE_SEND);
					DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
					Asset asset = dataMapItem.getDataMap().getAsset("file");
					String fileName = dataMapItem.getDataMap().getString("fileName");
					byte[] data = getDataFromAsset(asset);
					Log.d(TAG, "length -> " + (data == null ? 0 : data.length));
					Log.d(TAG, "执行回调方法 onDataChanged");

					fos = new FileOutputStream(new File(ClOUD_WATCH_DIRECTORY + fileName), true);
					fos.write(data);

					File firmwareFile = checkFile(fileName);
					Long length = 0L;
					if (firmwareFile.exists()) {
						length = firmwareFile.length();
					}
					sendMessage(String.valueOf(length).getBytes(), FIRMWARE_SEND_READY);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public byte[] getDataFromAsset(Asset asset) {
		if (asset == null) {
			throw new IllegalArgumentException("Asset must be non-null");
		}
		Thread thread = Thread.currentThread();
		Log.d(TAG, "thread id = " + thread.getId() + " name = " + thread.getName());
		InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mobvoiApiClient, asset).await().getInputStream();

		if (assetInputStream == null) {
			Log.w(TAG, "Requested an unknown Asset.");
			return null;
		}
		byte[] data = null;
		try {
			data = toByteArray(assetInputStream);
		} catch (IOException e) {
		}
		return data;
	}

	public static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}
}
