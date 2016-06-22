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
package com.cms.android.wearable;

import java.util.List;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;

/**
 * ClassName: NodeApi
 * 
 * @description NodeApi
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface NodeApi {
	public abstract PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, NodeListener nodeListener);
	
	public abstract PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, NodeListener nodeListener);

	public abstract PendingResult<GetConnectedNodesResult> getConnectedNodes(MobvoiApiClient mobvoiApiClient);
	
	public abstract PendingResult<GetLocalNodeResult> getLocalNode(MobvoiApiClient paramMobvoiApiClient);

	public static abstract interface GetConnectedNodesResult extends Result {
		public abstract List<Node> getNodes();
	}

	public static abstract interface GetLocalNodeResult extends Result {
		public abstract Node getNode();
	}

	public static abstract interface NodeListener {
		public abstract void onPeerConnected(Node node);

		public abstract void onPeerDisconnected(Node node);
	}
}