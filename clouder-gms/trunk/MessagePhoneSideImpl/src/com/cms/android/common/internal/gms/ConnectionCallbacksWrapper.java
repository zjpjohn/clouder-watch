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
package com.cms.android.common.internal.gms;

import android.os.Bundle;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient;

/**
 * ClassName: ConnectionCallbacksWrapper
 * 
 * @description ConnectionCallbacksWrapper
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class ConnectionCallbacksWrapper {
	
	private static final String TAG = "ConnectionCallbacksWrapper";

	private MobvoiApiClient.ConnectionCallbacks cb;

	public ConnectionCallbacksWrapper(MobvoiApiClient.ConnectionCallbacks connectionCallbacks) {
		this.cb = connectionCallbacks;
	}

	public boolean equals(Object obj) {
		return (obj instanceof ConnectionCallbacksWrapper) && this.cb.equals(((ConnectionCallbacksWrapper) obj).cb);
	}

	public int hashCode() {
		return this.cb.hashCode();
	}

	public void onConnected(Bundle bundle) {
		Log.d(TAG, "ConnectionCallbacksWrapper#onConnected()");
		this.cb.onConnected(bundle);
	}

	public void onConnectionSuspended(int paramInt) {
		Log.d(TAG, "ConnectionCallbacksWrapper#onConnectionSuspended()");
		this.cb.onConnectionSuspended(paramInt);
	}
}