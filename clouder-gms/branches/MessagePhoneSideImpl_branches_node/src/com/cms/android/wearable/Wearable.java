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

import android.content.Context;
import android.os.Looper;

import com.cms.android.common.api.Api;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.internal.proxy.MessageApiProxy;
import com.cms.android.common.internal.proxy.NodeApiProxy;
import com.cms.android.wearable.internal.WearableAdapter;

/**
 * ClassName: Wearable
 * 
 * @description Wearable
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class Wearable {
	public static final Api API;
	private static final Api.Builder<WearableAdapter> CLIENT_BUILDER;
	public static final Api.Key<WearableAdapter> CLIENT_KEY = new Api.Key<WearableAdapter>();
	// public static final ConnectionApi ConnectionApi;
	// public static final DataApi DataApi;
	public static final MessageApi MessageApi;
	public static final NodeApi NodeApi;

	static {
		CLIENT_BUILDER = new Api.Builder<WearableAdapter>() {
			public WearableAdapter build(Context context, Looper looper,
					MobvoiApiClient.ConnectionCallbacks connectionCallbacks,
					MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener) {
				return new WearableAdapter(context, looper, connectionCallbacks, onConnectionFailedListener);
			}
		};
		API = new Api(CLIENT_BUILDER, CLIENT_KEY);
		// ConnectionApi = new ConnectionApiImpl();
		// DataApi = new DataApiProxy();
		MessageApi = new MessageApiProxy();
		NodeApi = new NodeApiProxy();
	}
}