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
package com.cms.android.common.internal.proxy;

import android.util.Log;

import com.cms.android.common.MobvoiApiManager;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.api.impl.NodeApiImpl;

/**
 * ClassName: NodeApiProxy
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-11
 * 
 */
public class NodeApiProxy implements Loadable, NodeApi {

	private static final String TAG = "NodeApiProxy";

	private NodeApi instance;

	public NodeApiProxy() {
		MobvoiApiManager.getInstance().registerProxy(this);
		load();
	}

	public void load() {
		if (MobvoiApiManager.getInstance().getGroup() == MobvoiApiManager.ApiGroup.CMS) {
			this.instance = new NodeApiImpl();
		}
		Log.d(TAG, "load message api success.");
	}
	
	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, NodeApi.NodeListener listener) {
		return this.instance.addListener(mobvoiApiClient, listener);
	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, NodeApi.NodeListener listener) {
		return this.instance.removeListener(mobvoiApiClient, listener);
	}
	
	@Override
	public PendingResult<NodeApi.GetConnectedNodesResult> getConnectedNodes(MobvoiApiClient mobvoiApiClient) {
		return this.instance.getConnectedNodes(mobvoiApiClient);
	}

	@Override
	public PendingResult<GetLocalNodeResult> getLocalNode(MobvoiApiClient mobvoiApiClient) {
		return this.instance.getLocalNode(mobvoiApiClient);
	}

}
