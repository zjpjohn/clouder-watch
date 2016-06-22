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
package com.cms.android.wearable.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.Status;
import com.cms.android.common.internal.MmsClient;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.api.impl.DataApiImpl;
import com.cms.android.wearable.api.impl.MessageApiImpl;
import com.cms.android.wearable.service.impl.IBLEPeripheralService;
import com.cms.android.wearable.service.impl.IWearableListener;

/**
 * ClassName: WearableAdapter
 * 
 * @description WearableAdapter
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class WearableAdapter extends MmsClient<IBLEPeripheralService> {

	private static final String TAG = "WearableAdapter";

	private static final int TYPE_MESSAGE_LISTENER = 1;

	private static final int TYPE_DATA_LISTENER = 2;

	private static final int TYPE_NODE_LISTENER = 3;

	private ExecutorService executorService = Executors.newCachedThreadPool();

	public WearableAdapter(Context context, Looper looper, ConnectionCallbacks connectionCallbacks,
			OnConnectionFailedListener onConnectionFailedListener) {
		super(context, looper, connectionCallbacks, onConnectionFailedListener, new String[0]);
	}

	@Override
	protected IBLEPeripheralService getService(IBinder iBinder) {
		return IBLEPeripheralService.Stub.asInterface(iBinder);
	}

	public void sendMessage(final WearableResult<SendMessageResult> wearableResult, String node, String path,
			byte[] data) throws RemoteException {
		Log.d(TAG, String.format("Send message -> node = %s ,path = %s , length = %d.", node, path, data == null ? 0
				: data.length));
		getService().sendMessage(new WearableCallback(this.mContext) {

			@Override
			public void setSendMessageRsp(SendMessageResponse response) throws RemoteException {
				Log.i(TAG, "setSendMessageRsp -> " + response);

				wearableResult.setResult(new MessageApiImpl.SendMessageResultImpl(new Status(response.getStatusCode()),
						response.getRequestId()));
			}

		}, node, path, data);
	}

	public void addMessageListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().addListener(TYPE_MESSAGE_LISTENER, new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "addMessageListener -> " + status);
				wearableResult.setResult(status);
			}

		}, listener);
	}

	public void addDataListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().addListener(TYPE_DATA_LISTENER, new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "addDataListener -> " + status);
				wearableResult.setResult(status);
			}

		}, listener);
	}

	public void addNodeListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		try {
			getService().addListener(TYPE_NODE_LISTENER, new WearableCallback(this.mContext) {

				@Override
				public void setStatusRsp(Status status) throws RemoteException {
					Log.i(TAG, "addNodeListener -> " + status);
					wearableResult.setResult(status);
				}

			}, listener);
		} catch (Exception e) {
			Log.e(TAG, "addNodeListener Exception", e);
		}

	}

	public void removeMessageListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().removeListener(TYPE_MESSAGE_LISTENER, new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "removeMessageListener -> " + status);
				wearableResult.setResult(status);
			}

		}, listener);
	}

	public void removeDataListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().removeListener(TYPE_DATA_LISTENER, new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "removeDataListener -> " + status);
				wearableResult.setResult(status);
			}

		}, listener);
	}

	public void removeNodeListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().removeListener(TYPE_NODE_LISTENER, new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "removeNodeListener -> " + status);
				wearableResult.setResult(status);
			}

		}, listener);
	}

	public void putDataItem(WearableResult<DataApi.DataItemResult> wearableResult, PutDataRequest putDataRequest)
			throws RemoteException {
		Log.d(TAG, "put data item, uri = " + putDataRequest.getUri());
		PutDataRequest request = PutDataRequest.createFromUri(putDataRequest.getUri());
		ArrayList<FutureTask<Boolean>> futureTaskList = new ArrayList<FutureTask<Boolean>>();
		List<String> fileList = new ArrayList<String>();
		request.setData(putDataRequest.getData());
		if (putDataRequest.getAssets() != null) {
			Iterator<Entry<String, Asset>> iterator = putDataRequest.getAssets().entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, Asset> entry = iterator.next();
				Asset asset = (Asset) entry.getValue();
				if (asset.getData() != null) {
					String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "//cloudwatchcache";
					File baseDir = new File(basePath);
					if (!baseDir.exists()) {
						baseDir.mkdir();
					}
					String filepath = basePath + "//" + UUID.randomUUID().toString();
					Log.d(TAG,
							" basePath = " + basePath + " filepath = " + filepath + " saved length = "
									+ asset.getData().length);
					File fdFile = new File(filepath);
					BufferedOutputStream bos = null;
					try {
						if (!fdFile.exists()) {
							fdFile.createNewFile();
						}
						bos = new BufferedOutputStream(new FileOutputStream(fdFile));
						bos.write(asset.getData());
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
					try {
						ParcelFileDescriptor pfd = ParcelFileDescriptor.open(fdFile,
								ParcelFileDescriptor.MODE_READ_WRITE);
						asset.setFd(pfd);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					fileList.add(filepath);
				}
				request.putAsset((String) entry.getKey(), Asset.createFromFd(asset.getFd()));
			}
		}
		Log.d(TAG, "call remote put data item: " + request);
		getService().putDataItem(new PutDataItemCallback(this.mContext, wearableResult, futureTaskList, fileList),
				request);
	}

	public void getFdForAsset(final WearableResult<DataApi.GetFdForAssetResult> wearableResult, Asset asset)
			throws RemoteException {
		getService().getFdForAsset(new WearableCallback(this.mContext) {

			@Override
			public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {
				Log.d(TAG, "[setGetFdForAssetRsp] status = " + getFdForAssetResponse.status + " fd = "
						+ getFdForAssetResponse.fd + " version = " + getFdForAssetResponse.version);
				wearableResult.setResult(new DataApiImpl.GetFdForAssetResultImpl(new Status(
						getFdForAssetResponse.status), getFdForAssetResponse.fd));
			}

		}, asset);
	}

	public void getConnectedNodes(final WearableResult<NodeApi.GetConnectedNodesResult> wearableResult)
			throws RemoteException {
		Log.i(TAG, "getService getConnectedNodes");
		getService().getConnectedNodes(new WearableCallback(this.mContext) {

			@Override
			public void setGetConnectedNodesRsp(final GetConnectedNodesResponse getConnectedNodesResponse)
					throws RemoteException {
				wearableResult.setResult(new NodeApi.GetConnectedNodesResult() {

					@Override
					public Status getStatus() {
						return new Status(0);
					}

					@Override
					public List<Node> getNodes() {
						List<Node> nodeList = new ArrayList<Node>();
						if (getConnectedNodesResponse.getNodes() != null) {
							nodeList = getConnectedNodesResponse.getNodes();
						}
						Log.i(TAG, "getNodes ->  size : " + nodeList.size());
						return nodeList;
					}
				});
			}

		});
	}

	public void getLocalNode(final WearableResult<NodeApi.GetLocalNodeResult> wearableResult) throws RemoteException {
		Log.i(TAG, "getService getConnectedNodes");
		getService().getLocalNode(new WearableCallback(this.mContext) {

			@Override
			public void setGetLocalNodeRsp(final GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
				wearableResult.setResult(new NodeApi.GetLocalNodeResult() {

					@Override
					public Status getStatus() {
						return new Status(0);
					}

					@Override
					public Node getNode() {
						return getLocalNodeResponse.getNode();
					}
				});
			}

		});
	}

	@Override
	protected String getServiceDescriptor() {
		return "com.cms.android.wearable.internal.IWearableService";
	}

	@Override
	protected String getStartServiceAction() {
		return "com.hoperun.ble.peripheral.service";
	}

	private FutureTask<Boolean> createFutureTask(ParcelFileDescriptor pfd, byte[] data) {
		return new FutureTask<Boolean>(new DataCallable(pfd, data));
	}

	@Override
	protected void onInit(IBinder iBinder) {
		Log.d(TAG, "onInit...");
		setService(this, IBLEPeripheralService.Stub.asInterface(iBinder));
	}

}