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
package com.cms.android.common.api;

import java.util.HashSet;
import java.util.Set;
import android.content.Context;
import android.os.Bundle;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.internal.Assert;
import com.cms.android.common.internal.MobvoiApi;
import com.cms.android.common.internal.proxy.MobvoiApiClientProxy;

/**
 * ClassName: MobvoiApiClient
 * 
 * @description MobvoiApiClient
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface MobvoiApiClient {

	public abstract void connect();

	public abstract void disconnect();

	public abstract boolean isConnected();

	public abstract <A extends Api.Connection, T extends MobvoiApi.ApiResult<? extends Result, A>> T setResult(T t);

	public static final class Builder {
		private final Set<Api> mApis = new HashSet<Api>();
		private final Set<MobvoiApiClient.ConnectionCallbacks> mConnectionCallbacksSet = new HashSet<MobvoiApiClient.ConnectionCallbacks>();
		private Context mContext;
		private final Set<MobvoiApiClient.OnConnectionFailedListener> mOnConnectionFailedListenerSet = new HashSet<MobvoiApiClient.OnConnectionFailedListener>();

		public Builder(Context context) {
			this.mContext = context;
		}

		public Builder addApi(Api api) {
			this.mApis.add(api);
			return this;
		}

		public Builder addConnectionCallbacks(MobvoiApiClient.ConnectionCallbacks connectionCallbacks) {
			this.mConnectionCallbacksSet.add(connectionCallbacks);
			return this;
		}

		public Builder addOnConnectionFailedListener(
				MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener) {
			this.mOnConnectionFailedListenerSet.add(onConnectionFailedListener);
			return this;
		}

		public MobvoiApiClient build() {
			boolean isEmpty = this.mApis.isEmpty();
			Assert.notEmpty(!isEmpty, "must call addApi() to add at least one API");
			return new MobvoiApiClientProxy(this.mContext, this.mApis, this.mConnectionCallbacksSet,
					this.mOnConnectionFailedListenerSet);
		}
	}

	public static abstract interface ConnectionCallbacks {
		public abstract void onConnected(Bundle bundle);

		public abstract void onConnectionSuspended(int paramInt);
	}

	public static abstract interface OnConnectionFailedListener {
		public abstract void onConnectionFailed(ConnectionResult connectionResult);
	}
}