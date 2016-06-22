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

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.Api;
import com.cms.android.common.api.MobvoiApiClient;

/**
 * ClassName: MmsClient
 * 
 * @description MmsClient
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract class MmsClient<T extends IInterface> implements Api.Connection, MmsClientEvents.Callbacks {

	private static final String TAG = "MmsClient";

	private static final int STATE_DISCONNECT = 1;

	private static final int STATE_CONNECTING = 2;

	private static final int STATE_CONNECTED = 3;

	protected Context mContext;

	private MmsClientEvents mEvents;

	final Handler mHandler;

	private boolean mInConnect = false;

	// private final List<? extends MmsClient<T>.Proxy<?>> mListenerList = new
	// ArrayList<Proxy<?>>();

	private Looper mLooper;

	private T mService;

	private MmsServiceConnection mServiceConn = null;

	private int mStatus = STATE_DISCONNECT;

	protected MmsClient(Context context, Looper looper, MobvoiApiClient.ConnectionCallbacks connectionCallbacks,
			MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener, String[] array) {
		this.mContext = ((Context) Assert.neNull(context));
		Assert.notEmpty(onConnectionFailedListener, "Looper must not be null");
		this.mLooper = looper;
		this.mEvents = new MmsClientEvents(this.mContext, this.mLooper, this);
		this.mHandler = new EventHandler(this.mLooper);
		registerConnectionCallbacks((MobvoiApiClient.ConnectionCallbacks) Assert.neNull(connectionCallbacks));
		registerConnectionFailedListener((MobvoiApiClient.OnConnectionFailedListener) Assert
				.neNull(onConnectionFailedListener));
	}

	static void changeStatus(MmsClient<?> mmsClient, int status) {
		mmsClient.changeTo(status);
	}

	private void changeTo(int status) {
		Log.d("MmsClient", "status change, from status: " + this.mStatus + ", to status " + status);
		if (this.mStatus != status) {
			if ((this.mStatus == STATE_CONNECTED) && (status == STATE_DISCONNECT))
				onDisconnected();
			this.mStatus = status;
			if (status == STATE_CONNECTED)
				onConnected();
		}
	}

	static MmsServiceConnection getConnection(MmsClient<?> mmsClient) {
		return mmsClient.mServiceConn;
	}

	static Context getContext(MmsClient<?> mmsClient) {
		return mmsClient.mContext;
	}

	static MmsClientEvents getEvents(MmsClient<?> mmsClient) {
		return mmsClient.mEvents;
	}

	static void changeTo(MmsClient<?> mmsClient, int status) {
		mmsClient.changeTo(status);
	}

	// static List<?> getListenerList(MmsClient<?> mmsClient) {
	// return mmsClient.mListenerList;
	// }

	// static <I extends IInterface> I getService(MmsClient<I> mmsClient) {
	// return mmsClient.mService;
	// }

	static MmsServiceConnection setConnection(MmsClient<?> mmsClient, MmsServiceConnection mmsServiceConnection) {
		mmsClient.mServiceConn = mmsServiceConnection;
		return mmsServiceConnection;
	}

	protected static <I extends IInterface> I setService(MmsClient<I> mmsClient, I i) {
		mmsClient.mService = i;
		return i;
	}

	@Override
	public void connect() {
		Log.d(TAG, "MMSClient connect...");
		this.mInConnect = true;
		changeTo(STATE_CONNECTING);
		if (this.mServiceConn != null) {
			Log.e("MmsClient", "discard a connect request, another connect task is still running.");
			this.mService = null;
			MmsHandleCallback.getInstance(this.mContext).unbindService(getStartServiceAction(), this.mServiceConn);
		}
		this.mServiceConn = new MmsServiceConnection(this);
		if (!MmsHandleCallback.getInstance(this.mContext).bindService(getStartServiceAction(), this.mServiceConn)) {
			Log.e("MmsClient", "connect to service failed, action : " + getStartServiceAction());
			this.mHandler.obtainMessage(3, Integer.valueOf(9)).sendToTarget();
		}
	}

	@Override
	public void disconnect() {
		Log.d(TAG, "MMSClient disconnect...");
		this.mInConnect = false;
		changeTo(STATE_DISCONNECT);
		if (this.mServiceConn != null) {
			Log.e("MmsClient", "discard a connect request.");
			this.mService = null;
			MmsHandleCallback.getInstance(this.mContext).unbindService(getStartServiceAction(), this.mServiceConn);
		}
		this.mEvents.connectFailed(new ConnectionResult(13, null));
	}

	protected final void ensureConnected() {
		Log.d("MmsClient", "in ensure connected, state = " + isConnected());
		if (!isConnected())
			throw new IllegalStateException("not connected yet.");
	}

	public Bundle getBundle() {
		return null;
	}

	public Context getContext() {
		return this.mContext;
	}

	@Override
	public Looper getLooper() {
		return this.mLooper;
	}

	public T getService() {
		ensureConnected();
		Log.d("MmsClient", "get service, service: " + this.mService);
		return this.mService;
	}

	protected abstract T getService(IBinder iBinder);

	protected abstract String getServiceDescriptor();

	protected abstract String getStartServiceAction();

	public boolean inConnect() {
		return this.mInConnect;
	}

	private final void init(IBinder iBinder) {
		Log.d(TAG, "Service已经连接上，进行初始化");
		onInit(iBinder);
	}

	protected abstract void onInit(IBinder iBinder);

	@Override
	public boolean isConnected() {
		return this.mStatus == STATE_CONNECTED;
	}

	public boolean isConnecting() {
		return this.mStatus == STATE_CONNECTING;
	}

	protected void onConnected() {
	}

	protected void onDisconnected() {
	}

	// protected abstract void onInit(IMmsServiceBroker paramIMmsServiceBroker,
	// MmsServiceCallback paramMmsServiceCallback)
	// throws RemoteException;
	//
	// protected void onPostInitHandler(int statusCode, IBinder iBinder, Bundle
	// bundle) {
	// Log.d("MmsClient", "on post init handler, status = " + statusCode);
	// this.mHandler.obtainMessage(1, new MmsClientProxy(statusCode, iBinder,
	// bundle)).sendToTarget();
	// }

	public void registerConnectionCallbacks(MobvoiApiClient.ConnectionCallbacks connectionCallbacks) {
		Log.i("MmsClient", "register connection callbacks");
		this.mEvents.registerConnectionCallbacks(connectionCallbacks);
	}

	public void registerConnectionFailedListener(MobvoiApiClient.OnConnectionFailedListener onConnectionFailedListener) {
		Log.i("MmsClient", "register connection failed listener");
		this.mEvents.registerConnectionFailedListener(onConnectionFailedListener);
	}

	final class EventHandler extends Handler {
		private final MmsClient<T> mClient = MmsClient.this;

		public EventHandler(Looper looper) {
			super();
		}

		public void handleMessage(Message message) {
			Log.d("MmsClient", "msg content: what[" + message.what + "], isConnected[" + MmsClient.this.isConnected()
					+ "], isConnecting[" + MmsClient.this.isConnecting() + "].");
			if (((message.what == 1) && (!MmsClient.this.isConnecting()))
					|| ((message.what == 2) && (!MmsClient.this.isConnected()))) {
				// MmsClient.Proxy localProxy = (MmsClient.Proxy) message.obj;
				// localProxy.onFailure();
				// localProxy.unregister();
			}
			if (message.what == 3) {
				// 连接失败
				MmsClient.getEvents(this.mClient).connectFailed(
						new ConnectionResult(((Integer) message.obj).intValue(), null));
			}
			if (message.what == 4) {
				// 挂起
				MmsClient.changeStatus(this.mClient, STATE_DISCONNECT);
				MmsClient.setService(this.mClient, null);
				MmsClient.getEvents(this.mClient).suspend(((Integer) message.obj).intValue());
			}
			if ((message.what == 2) || (message.what == 1)) {
				// ((MmsClient.Proxy) message.obj).excute();
			}
			Log.w("MmsClient", "Discard a message, unknown message.");
		}
	}

	// protected final class MmsClientProxy extends MmsClient<T>.Proxy<Boolean>
	// {
	// public final IBinder mBinder;
	// public final Bundle mBundle;
	// final MmsClient<T> mThis = MmsClient.this;
	// public final int statusCode;
	//
	// public MmsClientProxy(int statusCode, IBinder ibinder, Bundle bundle) {
	// this.statusCode = statusCode;
	// this.mBinder = ibinder;
	// this.mBundle = bundle;
	// }
	//
	// protected void execute(Boolean paramBoolean) {
	// onExecute(paramBoolean);
	// }
	//
	// protected void onExecute(Boolean paramBoolean) {
	// if (paramBoolean == null)
	// MmsClient.changeStatus(this.mThis, 1);
	// while (true) {
	// return;
	// Log.d("MmsClient", "on excute, status code = " + this.statusCode);
	// switch (this.statusCode) {
	// default:
	// MmsClient.changeStatus(this.mThis, 1);
	// throw new IllegalStateException("occured unknown connect state");
	// case 10:
	// if (this.mBundle != null)
	// ;
	// for (Parcelable localParcelable =
	// this.mBundle.getParcelable("pendingIntent");; localParcelable = null) {
	// PendingIntent localPendingIntent = (PendingIntent) (PendingIntent)
	// localParcelable;
	// if (MmsClient.getConnection(this.mThis) != null) {
	// MmsHandleCallback.getInstance(MmsClient.getContext(this.mThis)).unbindService(
	// MmsClient.this.getStartServiceAction(),
	// MmsClient.getConnection(this.mThis));
	// MmsClient.setConnection(this.mThis, null);
	// }
	// MmsClient.changeStatus(this.mThis, 1);
	// MmsClient.setService(this.mThis, null);
	// MmsClient.getEvents(this.mThis).connectFailed(
	// new ConnectionResult(this.statusCode, localPendingIntent));
	// break;
	// }
	// case 0:
	// }
	// try {
	// String str = this.mBinder.getInterfaceDescriptor();
	// Log.d("MmsClient",
	// "interface descriptor: desc[ " + str + "], local desc[" +
	// this.mThis.getServiceDescriptor()
	// + "].");
	// if (this.mThis.getServiceDescriptor().equals(str)) {
	// MmsClient.setService(this.mThis,
	// MmsClient.this.getService(this.mBinder));
	// if (MmsClient.getService(this.mThis) != null) {
	// MmsClient.changeStatus(this.mThis, 3);
	// MmsClient.getEvents(this.mThis).connected();
	// }
	// }
	// } catch (RemoteException localRemoteException) {
	// localRemoteException.printStackTrace();
	// MmsHandleCallback.getInstance(MmsClient.getContext(this.mThis)).bindService(
	// MmsClient.this.getStartServiceAction(),
	// MmsClient.getConnection(this.mThis));
	// MmsClient.setConnection(this.mThis, null);
	// MmsClient.changeStatus(this.mThis, 1);
	// MmsClient.setService(this.mThis, null);
	// MmsClient.getEvents(this.mThis).connectFailed(new ConnectionResult(8,
	// null));
	// }
	// }
	// }
	//
	// protected void onFailure() {
	// }
	// }
	//
	// public static final class MmsServiceCallback extends IMmsCallback.Stub {
	// private final MmsClient<?> mClient;
	//
	// MmsServiceCallback(MmsClient<?> mmsClient) {
	// this.mClient = mmsClient;
	// }
	//
	// public void onPostInitComplete(int statusCode, IBinder iBinder, Bundle
	// bundle) throws RemoteException {
	// Assert.notEmpty("onPostInitComplete can be called only once per call to getServiceFromBroker",
	// this.mClient);
	// this.mClient.onPostInitHandler(statusCode, iBinder, bundle);
	// }
	// }

	static final class MmsServiceConnection implements ServiceConnection {
		private final MmsClient<?> mClient;

		MmsServiceConnection(MmsClient<?> mmsClient) {
			Assert.neNull(mmsClient);
			this.mClient = mmsClient;
		}

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			Log.e("MmsClient", "on service connected, component name = " + componentName + ", binder = " + iBinder
					+ ".");
			changeTo(this.mClient, STATE_CONNECTED);
			this.mClient.init(iBinder);
			MmsClient.getEvents(this.mClient).connected();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.e("MmsClient", "on service disconnected, component name = " + componentName + ".");
			this.mClient.mHandler.obtainMessage(4, Integer.valueOf(1)).sendToTarget();
		}
	}

	// protected abstract class Proxy<L> {
	// private boolean mIsUsed;
	// private L mListener;
	// final MmsClient<?> mThis = MmsClient.this;
	//
	// public Proxy() {
	// Object localObject;
	// this.mListener = localObject;
	// this.mIsUsed = false;
	// }
	//
	// public void clear() {
	// synchronized (this.mListener) {
	// this.mListener = null;
	// return;
	// }
	// }
	//
	// public void excute() {
	// if (this.mIsUsed)
	// Log.w("MmsClient", "It is not safe to reuse callback proxy for " + this +
	// ".");
	// if (this.mListener != null) {
	// try {
	// execute(this.mListener);
	// this.mIsUsed = true;
	// unregister();
	// return;
	// } catch (RuntimeException localRuntimeException) {
	// onFailure();
	// continue;
	// }
	// onFailure();
	// }
	// }
	//
	// protected abstract void execute(L l);
	//
	// protected abstract void onFailure();
	//
	// public void unregister() {
	// clear();
	// synchronized (MmsClient.getListenerList(this.mThis)) {
	// MmsClient.getListenerList(this.mThis).remove(this);
	// return;
	// }
	// }
	// }
}