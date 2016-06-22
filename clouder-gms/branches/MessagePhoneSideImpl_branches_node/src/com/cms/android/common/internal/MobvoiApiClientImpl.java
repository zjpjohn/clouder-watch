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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.Api;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;

/**
 * ClassName: MobvoiApiClientImpl
 * 
 * @description MobvoiApiClientImpl
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MobvoiApiClientImpl implements MobvoiApiClient {

	private static final String TAG = "MobvoiApiClientImpl";

	private static final int STATE_CONNECTING = 1;

	private static final int STATE_CONNECTED = 2;

	private static final int STATE_DISCONNECTING = 3;

	private static final int STATE_DISCONNECTED = 4;

	private int mApiCount;

	private final Map<Api.Key<?>, Api.Connection> mApiMap = new HashMap<Api.Key<?>, Api.Connection>();

	private final Bundle mBundle = new Bundle();

	private final MobvoiApiClient.ConnectionCallbacks mCallback = new MobvoiApiClient.ConnectionCallbacks() {

		@Override
		public void onConnected(Bundle bundle) {
			MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).lock();
			try {
				Log.d(TAG, "on connected start, api count = " + MobvoiApiClientImpl.this.mApiCount);
				if (MobvoiApiClientImpl.getStatus(MobvoiApiClientImpl.this) == STATE_CONNECTING) {
					if (bundle != null)
						MobvoiApiClientImpl.getBundle(MobvoiApiClientImpl.this).putAll(bundle);
					MobvoiApiClientImpl.connectTo(MobvoiApiClientImpl.this);
				}
				return;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).unlock();
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).lock();
			try {
				Log.d(TAG, "on connection suspended start, api count = " + MobvoiApiClientImpl.this.mApiCount);
				MobvoiApiClientImpl.this.suspend(cause);
				return;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).unlock();
			}
		}
	};

	private MmsClientEvents.Callbacks mCallbacks = new MmsClientEvents.Callbacks() {
		public Bundle getBundle() {
			return null;
		}

		public boolean inConnect() {
			return MobvoiApiClientImpl.this.mInConnect;
		}

		public boolean isConnected() {
			return MobvoiApiClientImpl.this.isConnected();
		}
	};
	private final Lock mLocker = new ReentrantLock();

	private final Condition mCondition = this.mLocker.newCondition();

	private final MmsClientEvents mEvents;

	final Handler mHandler;

	private boolean mInConnect = false;

	private final MobvoiApiClient.OnConnectionFailedListener mListener = new MobvoiApiClient.OnConnectionFailedListener() {
		public void onConnectionFailed(ConnectionResult connectionResult) {
			Log.d(TAG, "on connection failed start, api count = " + MobvoiApiClientImpl.this.mApiCount);
			MobvoiApiClientImpl.this.mEvents.connectFailed(connectionResult);
		}
	};

	private final Looper mLooper;

	private ConnectionResult mResult;

	private volatile int mStatus = STATE_DISCONNECTED;

	private void changeTo(int status) {
		Log.d("MmsClient", "status change, from " + getStatusText(this.mStatus) + ", to " + getStatusText(status));
		this.mStatus = status;
	}

	private String getStatusText(int status) {
		String statusText = "";
		switch (status) {
		case STATE_CONNECTING:
			statusText = "CONNECTING";
			break;
		case STATE_CONNECTED:
			statusText = "CONNECTED";
			break;
		case STATE_DISCONNECTING:
			statusText = "DISCONNECTING";
			break;
		case STATE_DISCONNECTED:
			statusText = "DISCONNECTED";
			break;

		default:
			break;
		}
		return statusText;
	}

	public MobvoiApiClientImpl(Context context, Set<Api> apiSet, Set<MobvoiApiClient.ConnectionCallbacks> callbackSet,
			Set<MobvoiApiClient.OnConnectionFailedListener> listenerSet) {
		this.mLooper = context.getMainLooper();
		this.mHandler = new ConnectHandler(this.mLooper);
		this.mEvents = new MmsClientEvents(context, this.mLooper, this.mCallbacks);
		Iterator<ConnectionCallbacks> callbackIterator = callbackSet.iterator();
		while (callbackIterator.hasNext()) {
			MobvoiApiClient.ConnectionCallbacks localConnectionCallbacks = (MobvoiApiClient.ConnectionCallbacks) callbackIterator
					.next();
			this.mEvents.registerConnectionCallbacks(localConnectionCallbacks);
		}
		Iterator<OnConnectionFailedListener> listenerIterator = listenerSet.iterator();
		while (listenerIterator.hasNext()) {
			MobvoiApiClient.OnConnectionFailedListener localOnConnectionFailedListener = (MobvoiApiClient.OnConnectionFailedListener) listenerIterator
					.next();
			this.mEvents.registerConnectionFailedListener(localOnConnectionFailedListener);
		}
		Iterator<Api> apiIterator = apiSet.iterator();
		while (apiIterator.hasNext()) {
			Api localApi = (Api) apiIterator.next();
			Api.Builder<?> localBuilder = localApi.getBuilder();
			this.mApiMap.put(localApi.getKey(),
					create(localBuilder, context, this.mLooper, this.mCallback, this.mListener));
		}
	}

	private void cancel() {
		this.mLocker.lock();
		try {
			this.mHandler.removeMessages(1);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mLocker.unlock();
		}
	}

	private void connectClient() {
		this.mLocker.lock();
		try {
			this.mApiCount = (-1 + this.mApiCount);
			Log.d("MobvoiApiClientImpl", "connect client start, api count = " + this.mApiCount);
			if ((this.mApiCount == 0) && (this.mResult == null)) {
				changeTo(STATE_CONNECTED);
				this.mEvents.onConnected(this.mBundle);
			}
			this.mCondition.signalAll();
			return;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mLocker.unlock();
		}
	}

	public static void connectTo(MobvoiApiClientImpl mobvoiApiClientImpl) {
		mobvoiApiClientImpl.connectClient();
	}

	private static <C extends Api.Connection, O> C create(Api.Builder<C> builder, Context context, Looper looper,
			MobvoiApiClient.ConnectionCallbacks connectionCallbacks,
			MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener) {
		return builder.build(context, looper, connectionCallbacks, onConnectionFailedListener);
	}

	static Bundle getBundle(MobvoiApiClientImpl mobvoiApiClientImpl) {
		return mobvoiApiClientImpl.mBundle;
	}

	static Lock getLock(MobvoiApiClientImpl mobvoiApiClientImpl) {
		return mobvoiApiClientImpl.mLocker;
	}

	static int getStatus(MobvoiApiClientImpl mobvoiApiClientImpl) {
		return mobvoiApiClientImpl.mStatus;
	}

	private void suspend(int cause) {
		Log.e(TAG, "suspend...." + this.mApiCount + " " + this.mApiMap.size());
		this.mLocker.lock();
		try {
			// if ((this.mApiCount == this.mApiMap.size()) && (this.mResult ==
			// null)) {
			//
			// }
			changeTo(STATE_DISCONNECTED);
			this.mEvents.suspend(cause);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.mLocker.unlock();
		}
	}

	@Override
	public void connect() {
		// TODO 连接服务 在我们这边实现应该是连接BLE服务
		Log.d(TAG, "connect start.");
		this.mLocker.lock();
		this.mInConnect = true;
		try {
			if ((isConnected()) || (isConnecting()))
				return;
			this.mApiCount = this.mApiMap.size();
			changeTo(STATE_CONNECTING);
			this.mResult = null;
			Iterator<Api.Connection> apiConnectionIterator = this.mApiMap.values().iterator();
			this.mBundle.clear();
			while (apiConnectionIterator.hasNext()) {
				((Api.Connection) apiConnectionIterator.next()).connect();
			}
		} finally {
			this.mLocker.unlock();
		}
		this.mEvents.onConnected(this.mBundle);
	}

	@Override
	public void disconnect() {
		// TODO 断开连接服务
		Log.d(TAG, "disconnect start.");
		this.mInConnect = false;
		cancel();
		this.mLocker.lock();
		try {
			changeTo(STATE_DISCONNECTING);
			Iterator<Api.Connection> connectionIterator = this.mApiMap.values().iterator();
			while (connectionIterator.hasNext()) {
				Api.Connection connection = (Api.Connection) connectionIterator.next();
				if (!connection.isConnected())
					continue;
				connection.disconnect();
			}
		} finally {
			this.mLocker.unlock();
		}
		changeTo(STATE_DISCONNECTED);
	}

	@Override
	public boolean isConnected() {
		this.mLocker.lock();
		try {
			return this.mStatus == STATE_CONNECTED;
		} finally {
			this.mLocker.unlock();
		}
	}

	public boolean isConnecting() {
		this.mLocker.lock();
		try {
			return this.mStatus == STATE_CONNECTING;
		} finally {
			this.mLocker.unlock();
		}
	}

	public Looper getLooper() {
		return this.mLooper;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends Api.Connection, T extends MobvoiApi.ApiResult<? extends Result, A>> T setResult(T t) {
		this.mLocker.lock();
		try {
			Log.d(TAG, "in set result, result = " + t + ", isConnected = " + isConnected());
			if (isConnected()) {
				try {
					t.setConnection((A) this.mApiMap.get(t.getKey()));
					return t;
				} catch (Exception localException) {
					if (t.isReady())
						t.setStatus(new Status(9));
				}
			} else {
				Log.i(TAG, "当前状态未连接");
			}
		} finally {
			this.mLocker.unlock();
		}
//		Looper.prepare();
		t.setHandler(new MobvoiApi.ResultHandler(getLooper()));
		t.setStatus(new Status(9));
		return t;
	}

	class ConnectHandler extends Handler {
		public ConnectHandler(Looper looper) {
			super();
		}

		public void handleMessage(Message message) {
			if (message.what == 1)
				MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).lock();
			try {
				if ((!MobvoiApiClientImpl.this.isConnected()) || (!MobvoiApiClientImpl.this.isConnecting()))
					MobvoiApiClientImpl.this.connect();
				return;
			} catch (Exception localException) {
				Log.w(TAG, "handle a message failed.", localException);
				MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).unlock();
			} finally {
				MobvoiApiClientImpl.getLock(MobvoiApiClientImpl.this).unlock();
			}
		}
	}

	static abstract interface Connection<A extends Api.Connection> {
	}

	static abstract interface OnClearListener {
		public abstract void onClear(MobvoiApiClientImpl.Connection<?> connection);
	}
}