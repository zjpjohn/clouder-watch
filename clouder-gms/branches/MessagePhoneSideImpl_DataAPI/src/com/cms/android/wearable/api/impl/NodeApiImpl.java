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
package com.cms.android.wearable.api.impl;

import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.internal.WearableAdapter;
import com.cms.android.wearable.internal.WearableListener;
import com.cms.android.wearable.internal.WearableResult;

/**
 * ClassName: NodeApiImpl
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-11
 * 
 */
public class NodeApiImpl implements NodeApi {

	private static final String TAG = "NodeApiImpl";

	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, NodeListener listener) {
		
		final WearableListener wearableListener = new WearableListener(listener);
		
		return mobvoiApiClient.setResult(new WearableResult<Status>() {		
			
			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter add NodeListener");
				adapter.addNodeListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "create NodeListener status: " + status);
				return status;
			}
		});
	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, NodeListener listener) {
		
		final WearableListener wearableListener = new WearableListener(listener);
		
		return mobvoiApiClient.setResult(new WearableResult<Status>() {		
			
			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter remove NodeListener");
				adapter.removeNodeListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "remove NodeListener status: " + status);
				return status;
			}
		});
	}
	
	@Override
	public PendingResult<GetConnectedNodesResult> getConnectedNodes(MobvoiApiClient mobvoiApiClient) {
		
		return mobvoiApiClient.setResult(new WearableResult<GetConnectedNodesResult>() {		
			
			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter get ConnectedNodesResult");
				adapter.getConnectedNodes(this);
			}

			@Override
			protected GetConnectedNodesResult create(Status status) {
				return this.await();
			}
		});
	}

	@Override
	public PendingResult<GetLocalNodeResult> getLocalNode(MobvoiApiClient mobvoiApiClient) {
		return mobvoiApiClient.setResult(new WearableResult<GetLocalNodeResult>() {		
			
			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter get LocalNodeResult");
				adapter.getLocalNode(this);
			}

			@Override
			protected GetLocalNodeResult create(Status status) {
				return this.await();
			}
		});
	}


}
