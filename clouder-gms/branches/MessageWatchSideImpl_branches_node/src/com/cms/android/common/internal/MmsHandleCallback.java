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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

/**
 * ClassName: MmsHandleCallback
 * 
 * @description MmsHandleCallback
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MmsHandleCallback implements Handler.Callback {

	private static final String TAG = "MmsHandleCallback";

	private static final Object mLocker = new Object();

	private static MmsHandleCallback sInstance;

	private final Map<String, Proxy> mConnMap = new HashMap<String, MmsHandleCallback.Proxy>();

	private final Context mContext;

	private final Handler mHandler;

	private MmsHandleCallback(Context context) {
		this.mContext = context.getApplicationContext();
		this.mHandler = new Handler(context.getMainLooper(), this);
	}

	static Map<String, Proxy> getConnMap(MmsHandleCallback mmsHandleCallback) {
		return mmsHandleCallback.mConnMap;
	}

	private Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
		// Retrieve all services that can match the given intent
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

		// Make sure only one match was found
		if (resolveInfo == null || resolveInfo.size() != 1) {
			return null;
		}

		// Get component info and create ComponentName
		ResolveInfo serviceInfo = resolveInfo.get(0);
		String packageName = serviceInfo.serviceInfo.packageName;
		String className = serviceInfo.serviceInfo.name;
		ComponentName component = new ComponentName(packageName, className);

		// Create a new intent. Use the old one for extras and such reuse
		Intent explicitIntent = new Intent(implicitIntent);

		// Set the component to be explicit
		explicitIntent.setComponent(component);

		return explicitIntent;
	}

	public static MmsHandleCallback getInstance(Context context) {
		synchronized (mLocker) {
			if (sInstance == null)
				sInstance = new MmsHandleCallback(context);
			MmsHandleCallback localMmsHandleCallback = sInstance;
			return localMmsHandleCallback;
		}
	}

	public boolean bindService(String action, MmsClient.MmsServiceConnection serviceConnection) {
		if (TextUtils.isEmpty(action) || serviceConnection == null) {
			return false;
		}
		try {
			Intent serviceIntent = createExplicitFromImplicitIntent(this.mContext, new Intent(action));
			this.mContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean handleMessage(Message message) {
		if (message.what == 0) {
			Proxy localProxy = (Proxy) message.obj;
			synchronized (this.mConnMap) {
				if (localProxy.isConnEmpty()) {
					this.mContext.unbindService(localProxy.getConn());
					this.mConnMap.remove(localProxy.getAction());
				}
			}
		}
		return true;
	}

	public void unbindService(String action, MmsClient.MmsServiceConnection serviceConnection) {
		if (TextUtils.isEmpty(action) || serviceConnection == null) {
			return;
		}
		try {
			this.mContext.unbindService(serviceConnection);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static final class Proxy {

		private final String mAction;

		private IBinder mBinder;

		private final MmsConn mConn = new MmsConn(this);

		private final Set<MmsClient.MmsServiceConnection> mConnSet = new HashSet<MmsClient.MmsServiceConnection>();

		private boolean mIsBound;

		private ComponentName mName;

		private int mState;

		public Proxy(String action) {
			this.mAction = action;
			this.mState = 0;
		}

		static Set<MmsClient.MmsServiceConnection> getConnSet(Proxy proxy) {
			return proxy.mConnSet;
		}

		static IBinder setBinder(Proxy proxy, IBinder iBinder) {
			proxy.mBinder = iBinder;
			return iBinder;
		}

		static ComponentName setComponentName(Proxy proxy, ComponentName componentName) {
			proxy.mName = componentName;
			return componentName;
		}

		static int setState(Proxy proxy, int state) {
			proxy.mState = state;
			return state;
		}

		public void addConn(MmsClient.MmsServiceConnection mmsServiceConnection) {
			this.mConnSet.add(mmsServiceConnection);
		}

		public String getAction() {
			return this.mAction;
		}

		public IBinder getBinder() {
			return this.mBinder;
		}

		public ComponentName getComponentName() {
			return this.mName;
		}

		public ServiceConnection getConn() {
			return this.mConn;
		}

		public int getState() {
			return this.mState;
		}

		public boolean hasConn(MmsClient.MmsServiceConnection mmsServiceConnection) {
			return this.mConnSet.contains(mmsServiceConnection);
		}

		public boolean isBound() {
			return this.mIsBound;
		}

		public boolean isConnEmpty() {
			return this.mConnSet.isEmpty();
		}

		public void removeConn(MmsClient.MmsServiceConnection mmsServiceConnection) {
			this.mConnSet.remove(mmsServiceConnection);
		}

		public void setIsBound(boolean isBound) {
			this.mIsBound = isBound;
		}

		public static class MmsConn implements ServiceConnection {
			private final MmsHandleCallback.Proxy mProxy;

			public MmsConn(MmsHandleCallback.Proxy proxy) {
				this.mProxy = proxy;
			}

			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

			}

			public void onServiceDisconnected(ComponentName componentName) {

			}
		}
	}
}