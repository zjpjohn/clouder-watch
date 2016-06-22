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
package com.cms.android.common.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;

/**
 * ClassName: MmsClientEvents
 * 
 * @description MmsClientEvents
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MmsClientEvents {

	private static final String TAG = "MmsClientEvents";

	private Set<MobvoiApiClient.ConnectionCallbacks> mCallbacks = new HashSet<MobvoiApiClient.ConnectionCallbacks>();

	private final Callbacks mClient;

	private boolean mFlag = false;

	private final Handler mHandler;

	private Set<MobvoiApiClient.OnConnectionFailedListener> mListeners = new HashSet<MobvoiApiClient.OnConnectionFailedListener>();

	public MmsClientEvents(Context context, Looper looper, Callbacks callbacks) {
		this.mHandler = new EventHandler(looper);
		this.mClient = callbacks;
	}

	public void connectFailed(ConnectionResult connectionResult) {
		this.mHandler.removeMessages(1);
		synchronized (this.mListeners) {
			Log.i(TAG, "connect failed.");
			Iterator<OnConnectionFailedListener> iterator = this.mListeners.iterator();
			while (iterator.hasNext()) {
				MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener = (MobvoiApiClient.OnConnectionFailedListener) iterator
						.next();
				onConnectionFailedListener.onConnectionFailed(connectionResult);
			}
		}
	}

	protected void connected() {
		synchronized (this.mCallbacks) {
			onConnected(this.mClient.getBundle());
			return;
		}
	}

	public void onConnected(Bundle bundle) {
		synchronized (this.mCallbacks) {
			Log.i("MmsClientEvents", "on connected.");
			this.mHandler.removeMessages(1);
			Iterator<ConnectionCallbacks> localIterator = this.mCallbacks.iterator();
			while (localIterator.hasNext()) {
				MobvoiApiClient.ConnectionCallbacks connectionCallbacks = (MobvoiApiClient.ConnectionCallbacks) localIterator
						.next();
				Log.d("MmsClientEvents", "call connection callback, inConnect = " + this.mClient.inConnect()
						+ ", isConnected = " + this.mClient.isConnected());
				connectionCallbacks.onConnected(bundle);
			}
		}
	}

	public void registerConnectionCallbacks(MobvoiApiClient.ConnectionCallbacks connectionCallbacks) {
		Assert.neNull(connectionCallbacks);
		Log.i(TAG, "register connection callbacks");
		synchronized (this.mCallbacks) {
			if (this.mCallbacks.contains(connectionCallbacks)) {
				Log.w("MmsClientEvents", "duplicated registered listener : " + connectionCallbacks + ".");
				if (this.mClient.isConnected())
					this.mHandler.obtainMessage(1, connectionCallbacks).sendToTarget();
				return;
			}
			this.mCallbacks.add(connectionCallbacks);
		}
	}

	public void registerConnectionFailedListener(MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener) {
		Assert.neNull(onConnectionFailedListener);
		Log.i(TAG, "register connection failed listener");
		synchronized (this.mListeners) {
			if (this.mListeners.contains(onConnectionFailedListener)) {
				Log.w("MmsClientEvents", "duplicated register connection failed listener : "
						+ onConnectionFailedListener + ".");
				return;
			}
			this.mListeners.add(onConnectionFailedListener);
		}
	}

	public void suspend(int cause) {
		synchronized (this.mCallbacks) {
			Log.i("MmsClientEvents", "on suspend.");
			this.mHandler.removeMessages(1);
			Iterator<ConnectionCallbacks> localIterator = this.mCallbacks.iterator();
			while (localIterator.hasNext()) {
				MobvoiApiClient.ConnectionCallbacks localConnectionCallbacks = (MobvoiApiClient.ConnectionCallbacks) localIterator
						.next();
				Log.d("MmsClientEvents", "call suspend callback, inConnect = " + this.mClient.inConnect()
						+ ", isConnected = " + this.mClient.isConnected());
				localConnectionCallbacks.onConnectionSuspended(cause);
			}
		}
	}

	public static abstract interface Callbacks {
		public abstract Bundle getBundle();

		public abstract boolean inConnect();

		public abstract boolean isConnected();
	}

	final class EventHandler extends Handler {
		public EventHandler(Looper looper) {
			super();
		}

		public void handleMessage(Message message) {
			Log.i(TAG, "handle message: what = " + message.what);
			if (message.what == 1)
				synchronized (MmsClientEvents.this.mCallbacks) {
					if ((MmsClientEvents.this.mClient.inConnect()) && (MmsClientEvents.this.mClient.isConnected())
							&& (MmsClientEvents.this.mCallbacks.contains(message.obj)))
						((MobvoiApiClient.ConnectionCallbacks) message.obj).onConnected(MmsClientEvents.this.mClient
								.getBundle());
				}
			Log.w(TAG, "discard an unkonwn message.");
		}
	}
}
