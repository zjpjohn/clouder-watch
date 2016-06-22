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

import android.content.Context;
import android.os.Looper;

/**
 * ClassName: Api
 * 
 * @description Api
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class Api {
	private final Builder<?> mBuilder;
	private final Key<?> mKey;

	public <C extends Connection> Api(Builder<C> builder, Key<C> key) {
		this.mBuilder = builder;
		this.mKey = key;
	}

	public Builder<?> getBuilder() {
		return this.mBuilder;
	}

	public Key<?> getKey() {
		return this.mKey;
	}

	public static abstract interface Builder<T extends Api.Connection> {
		public abstract T build(Context context, Looper looper,
				MobvoiApiClient.ConnectionCallbacks connectionCallbacks,
				MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener);
	}

	public static abstract interface Connection {
		public abstract void connect();

		public abstract void disconnect();

		public abstract Looper getLooper();

		public abstract boolean isConnected();
	}

	public static final class Key<C extends Api.Connection> {

	}
}