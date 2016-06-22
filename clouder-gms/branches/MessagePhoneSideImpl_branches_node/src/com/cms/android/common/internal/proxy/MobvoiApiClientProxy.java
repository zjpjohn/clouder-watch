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
package com.cms.android.common.internal.proxy;

import java.util.Set;
import android.content.Context;
import android.util.Log;
import com.cms.android.common.MobvoiApiManager;
import com.cms.android.common.api.Api;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.Result;
import com.cms.android.common.internal.MobvoiApi;
import com.cms.android.common.internal.MobvoiApiClientImpl;

/**
 * ClassName: MobvoiApiClientProxy
 * 
 * @description MobvoiApiClientProxy
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MobvoiApiClientProxy implements MobvoiApiClient {
	private MobvoiApiClient instance;

	public MobvoiApiClientProxy(Context context, Set<Api> apiSet,
			Set<MobvoiApiClient.ConnectionCallbacks> connectionCallbackSet,
			Set<MobvoiApiClient.OnConnectionFailedListener> listenerSet) {
		if (MobvoiApiManager.getInstance().getGroup() == MobvoiApiManager.ApiGroup.MMS) {
			this.instance = new MobvoiApiClientImpl(context, apiSet, connectionCallbackSet, listenerSet);
		} else {
			Log.w("MobvoiApiManager", "create MobvoiApiClientProxy failed, invalid ApiGroup : "
					+ MobvoiApiManager.getInstance().getGroup());
		}
		// if (MobvoiApiManager.getInstance().getGroup() ==
		// MobvoiApiManager.ApiGroup.GMS) {
		// this.instance = new ApiClientGoogleImpl(context, apiSet,
		// connectionCallbackSet, listenerSet);
		// }
	}

	public void connect() {
		Log.d("MobvoiApiManager", "MobvoiApiClientProxy#connect()");
		this.instance.connect();
	}

	public void disconnect() {
		Log.d("MobvoiApiManager", "MobvoiApiClientProxy#disconnect()");
		this.instance.disconnect();
	}

	public MobvoiApiClient getInstance() {
		return this.instance;
	}

	public boolean isConnected() {
		Log.d("MobvoiApiManager", "MobvoiApiClientProxy#isConnected()");
		return this.instance.isConnected();
	}

	public <A extends Api.Connection, T extends MobvoiApi.ApiResult<? extends Result, A>> T setResult(T t) { 
		Log.d("MobvoiApiManager", "MobvoiApiClientProxy#setResult()");
		return this.instance.setResult(t);
	}
}