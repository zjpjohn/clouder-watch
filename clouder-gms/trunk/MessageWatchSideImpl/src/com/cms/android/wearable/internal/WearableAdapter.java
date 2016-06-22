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

import java.util.List;

import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.common.api.Status;
import com.cms.android.common.internal.MmsClient;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.MessageApi.SendMessageResult;
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

			@Override
			public String getPackageName() throws RemoteException {
				return super.getPackageName();
			}
		}, node, path, data);
	}
	
	public void addListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().addListener(new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "addListener -> " + status.toString());
				wearableResult.setResult(status);
			}
			
			@Override
			public String getPackageName() throws RemoteException {
				return super.getPackageName();
			}

		}, listener);
	}
	
	public void removeListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {
		getService().removeListener(new WearableCallback(this.mContext) {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "removeListener -> " + status.toString());
				wearableResult.setResult(status);
			}

			@Override
			public String getPackageName() throws RemoteException {
				return super.getPackageName();
			}
			
		}, listener);
	}

	@Override
	protected String getServiceDescriptor() {
		return "com.cms.android.wearable.internal.IWearableService";
	}

	@Override
	protected String getStartServiceAction() {
		return "com.hoperun.ble.peripheral.service";
	}

	@Override
	protected void onInit(IBinder iBinder) {
		Log.d(TAG, "onInit...");
		setService(this, IBLEPeripheralService.Stub.asInterface(iBinder));
		// try {
		// Log.d(TAG, "register callback...");
		// getService().registerCallback(getContext().getPackageName(),
		// callback);
		// } catch (RemoteException e) {
		// e.printStackTrace();
		// }
	}

	// private IBLECentralCallback.Stub callback = new
	// IBLECentralCallback.Stub() {
	//
	// @Override
	// public void setStatusRsp(Status status) throws RemoteException {
	//
	// }
	//
	// @Override
	// public void setSendMessageRsp(SendMessageResponse sendMessageResponse)
	// throws RemoteException {
	//
	// }
	//
	// @Override
	// public void onCallback(int type, BluetoothDevice device) throws
	// RemoteException {
	//
	// }
	// };

	public void addNodeListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {

		getService().addNodeListener(new WearableCallback() {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "addNodeListener -> " + status.toString());
				wearableResult.setResult(status);
			}

		}, listener);
	}
	
	public void removeNodeListener(final WearableResult<Status> wearableResult, IWearableListener listener)
			throws RemoteException {

		getService().removeNodeListener(new WearableCallback() {

			@Override
			public void setStatusRsp(Status status) throws RemoteException {
				Log.i(TAG, "removeNodeListener -> " + status.toString());
				wearableResult.setResult(status);
			}

		}, listener);
	}
	
	public void getConnectedNodes(final WearableResult<NodeApi.GetConnectedNodesResult> wearableResult)
			throws RemoteException {
		Log.i(TAG, "getService getConnectedNodes");
		getService().getConnectedNodes(new WearableCallback(){
			
			@Override
			public void setGetConnectedNodesRsp(final GetConnectedNodesResponse getConnectedNodesResponse) throws RemoteException {
				wearableResult.setResult(new NodeApi.GetConnectedNodesResult() {
					
					@Override
					public Status getStatus() {
						return new Status(0);
					}
					
					@Override
					public List<Node> getNodes() {
						Log.i(TAG, "getNodes ->  size : " + getConnectedNodesResponse.getNodes().size());
						return getConnectedNodesResponse.getNodes();
					}
				});
			}

		});
	}
	
	public void getLocalNode(final WearableResult<NodeApi.GetLocalNodeResult> wearableResult)
			throws RemoteException {
		Log.i(TAG, "getService getConnectedNodes");
		getService().getLocalNode(new WearableCallback(){
			
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
}